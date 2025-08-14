package kr.hhplus.be.server.service.product.impl;

import kr.hhplus.be.server.controller.product.request.ProductRequest;
import kr.hhplus.be.server.controller.product.request.ProductStockRequest;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.infrastructure.repository.product.ProductJpaRepository;
import kr.hhplus.be.server.lock.DistributedLock;
import kr.hhplus.be.server.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;


@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductJpaRepository productJpaRepository;
    //레디스
    private final RedissonClient redissonClient;


    /* 도메인 조회 */

    @Override
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다. id=" + id));
    }

    @Override
    public List<Product> findByIds(List<ProductRequest> reqs) {
        List<Long> ids = reqs.stream()
                .map(ProductRequest::id)
                .toList();

        return productRepository.findAllById(ids);
    }

    /*  상품 단일 조회  */
    @Override
    public ProductResponse getProductById(Long id) {
        Product product = findById(id);
        return ProductResponse.from(product);
    }

    /* 상품 복수 조회 (필요 시 확장)  */
    @Override
    public List<ProductResponse> getProductsById(List<Long> ids) {
        List<Product> products = productRepository.findAllById(ids);
        return products.stream()
                .map(ProductResponse::from)
                .toList();
    }

    /* 상위 판매 상품 조회 */

//    @Cacheable(
//            cacheNames = RedisCacheName.TOP_SELLING_V2,
//            key = "'d5:L3'",
//            sync = true // 스탬피드 방지. sync=true일 땐 unless 사용 금지!
//    )
    @Override
    public List<ProductResponse> getTopSellingProducts() {
        int days = 5;
        int limit = 3;

        List<Product> products = productRepository.findTopSellingList(days, limit);

        return products.stream()
                .map(ProductResponse::from)
                .toList();
    }

    /* 주문 상품 처리 (재고 차감 포함) */
    @Override
    @DistributedLock(
            keys = { "#reqs.![ 'product:' + id ]" },  // SpEL 배열 → 멀티락
            lease = 5,
            unit = ChronoUnit.SECONDS,
            waitFor = 2,
            waitUnit = ChronoUnit.SECONDS,
            pollMillis = 50,
            prefix = "lock:" // <- 통일을 위해 추가 (멀티락 시에도 붙는 prefix)
    )
    @Transactional
    public List<ProductResponse> useProduct(List<ProductStockRequest> reqs) {
        List<Product> products = getProductsByRequest(reqs);
        deductProductStocks(products, reqs);

        productJpaRepository.flush(); //추가 ; db즉시 반영
        return toResponse(products);
    }

//    public List<ProductResponse> useProduct(List<ProductStockRequest> reqs) {
//        // 1) 교착 방지: 잠글 대상 확정 (중복 제거 + 정렬)
//        List<Long> ids = reqs.stream().map(ProductStockRequest::id).distinct().sorted().toList();
//        if (ids.isEmpty()) return List.of();
//
//        // 2) 상품별 락 키 → 멀티락 구성
//        List<RLock> locks = ids.stream()
//                .map(id -> redissonClient.getLock("lock:product:" + id))
//                .toList();
//        RedissonMultiLock multiLock = new RedissonMultiLock(locks.toArray(new RLock[0]));
//
//        try {
//            // 3) 분산락 획득 (wait=2s, lease=0: 워치독 자동 연장)
//            boolean acquired = multiLock.tryLock(2, 0, TimeUnit.SECONDS);
//            if (!acquired) throw new IllegalStateException("LOCK_TIMEOUT: " + ids);
//
//            // 4) 커밋/롤백 '이후'에 락 해제 (finally에서 언락 금지)
//            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//                @Override public void afterCompletion(int status) {
//                    if (multiLock.isHeldByCurrentThread()) {
//                        try { multiLock.unlock(); } catch (Exception ignored) {}
//                    }
//                }
//            });
//
//            // 5) 기존 트랜잭션 로직 그대로 (안쪽은 JPA 비관적락으로 레코드 보호)
//            List<Product> products = getProductsByRequest(reqs); // findByIdForUpdate 사용
//            deductProductStocks(products, reqs);
//            productJpaRepository.flush();
//            return toResponse(products);
//
//        } catch (InterruptedException ie) {
//            Thread.currentThread().interrupt();
//            throw new IllegalStateException("LOCK_INTERRUPTED", ie);
//        }
//        // finally 블록 없음: 언락은 afterCompletion에서 처리
//    }

    /* 요청 정보 기준 도메인 조회 */
    private List<Product> getProductsByRequest(List<ProductStockRequest> reqs) {
        return reqs.stream()
                .map(req -> productRepository.findById(req.id())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품 ID: " + req.id())))
                .toList();
    }

    /* 재고 차감 */
    private void deductProductStocks(List<Product> products, List<ProductStockRequest> reqs) {
        Map<Long, Integer> reqMap = reqs.stream()
                .collect(toMap(ProductStockRequest::id, ProductStockRequest::quantity));

        products.forEach(product -> {
            int quantity = reqMap.get(product.getId());

            // 수량 초과하면 예외 던짐 (동시성 테스트 성공 조건에 맞게)
            if (product.getStock() < quantity) {
                throw new IllegalStateException("재고 부족: id=" + product.getId());
            }
            product.deductStock(quantity);
        });
    }

    /* 응답 변환 */
    private List<ProductResponse> toResponse(List<Product> products) {
        return products.stream()
                .map(ProductResponse::from)
                .toList();
    }



}
