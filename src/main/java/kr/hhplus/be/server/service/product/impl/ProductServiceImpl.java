package kr.hhplus.be.server.service.product.impl;

import com.querydsl.core.Tuple;
import kr.hhplus.be.server.controller.product.request.ProductRequest;
import kr.hhplus.be.server.controller.product.request.ProductStockRequest;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.controller.product.response.TopSellingProductResponse;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.infrastructure.repository.product.ProductJpaRepository;
import kr.hhplus.be.server.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;


@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;


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
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = findById(id);
        return ProductResponse.from(product);
    }

    /* 상품 복수 조회 (필요 시 확장)  */
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsById(List<Long> ids) {
        List<Product> products = productRepository.findAllById(ids);
        return products.stream()
                .map(ProductResponse::from)
                .toList();
    }

    /* 상위 판매 상품 조회 */

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
    public List<ProductResponse> useProduct(List<ProductStockRequest> reqs) {
        List<Product> products = getProductsByRequest(reqs);
        deductProductStocks(products, reqs);
        return toResponse(products);
    }

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
