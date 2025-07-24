package kr.hhplus.be.server.service.product.impl;

import kr.hhplus.be.server.controller.product.request.ProductRequest;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.repository.product.ProductRepository;
import kr.hhplus.be.server.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    //상품 리스트
    private final List<ProductResponse> mockProducts = new ArrayList<>(
            List.of(
                    new ProductResponse(1L, "맥북 M3", 2000000L, 5, LocalDateTime.now()),
                    new ProductResponse(3L, "갤럭시 Z 플립", 130000L, 3, LocalDateTime.now()),
                    new ProductResponse(4L, "아이패드 프로", 0L, 1, LocalDateTime.now())
            )
    ) ;

    //상위 판매 상품 리스트 
    private final List<ProductResponse> top3selling = List.of(
            new ProductResponse(2L, "갤럭시 Z 플립", 2000000L, 5, LocalDateTime.now()),
            new ProductResponse(1L, "맥북 M3", 1300000L, 3, LocalDateTime.now()),
            new ProductResponse(3L, "아이패드 프로", 0L, 0, LocalDateTime.now())
    );


    /* 상품 상세 조회 */
    @Override
    public ProductResponse getProductById(Long id) {

        Optional<ProductResponse> product = mockProducts.stream()
                .filter(p->p.id().equals(id))
                .findFirst();

        if(product.isEmpty()) throw new IllegalArgumentException("해당 상품이 존재하지 않습니다.");

        return product.get();
    }

    @Override
    public List<ProductResponse> getProductByIdIn(List<Long> ids) {
        return mockProducts;
    }


    /* 상위 판매 상품 리스트 조회 */
    @Override
    public List<ProductResponse> getTopSellingProducts() {
        return top3selling;
    }

    @Override
    public List<ProductResponse> useProduct(List<ProductRequest> reqs) {

        // 요청된 productId 리스트 뽑기
        List<Long> ids = reqs.stream()
                .map(ProductRequest::getId)
                .toList();

        // mock 데이터에서 product 가져오기
        List<ProductResponse> products = getProductByIdIn(ids);

        // Map<productId, ProductResponse> 형태로 변환
        Map<Long, ProductResponse> productMap = products.stream()
                .collect(java.util.stream.Collectors.toMap(ProductResponse::id, java.util.function.Function.identity()));


        for (ProductRequest req : reqs) {
            ProductResponse product = productMap.get(req.getId());

            if (product.stock() < req.getStock()) {
                throw new IllegalArgumentException("재고가 부족합니다.");
            }
        }

        // 여기선 재고 차감 없이 그냥 리스트 반환
        return products;
    }




}
