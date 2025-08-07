package kr.hhplus.be.server.controller.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.controller.product.request.ProductRequest;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.service.product.ProductService;
import kr.hhplus.be.server.service.product.impl.ProductServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name="상품",description = "상품 관련 API")
@RestController
@Slf4j
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {


    private final ProductService productService;

    @GetMapping("/{id}")
    @Operation(summary = "상품 상세 조회",description = "상품별 상세 정보 조회하는 API")
    public ResponseEntity<ProductResponse> getProductDetail(@PathVariable("id") Long id) {
        ProductResponse result = productService.getProductById(id);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/top-selling")
    @Operation(summary = "3일간 판매량 순 상위 상품 조회", description = "최근 3일간 많이 팔린 순으로 상품 조회하는 API")
    public ResponseEntity<List<ProductResponse>> getTopSellingProducts() {
        List<ProductResponse> topSellingProducts = productService.getTopSellingProducts();
        return ResponseEntity.ok(topSellingProducts);
    }


}
