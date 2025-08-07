package kr.hhplus.be.server.service.product;

import kr.hhplus.be.server.controller.product.request.ProductRequest;
import kr.hhplus.be.server.controller.product.request.ProductStockRequest;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.domain.product.Product;

import java.util.List;



public interface ProductService {

    Product findById(Long id);
    List<Product> findByIds(List<ProductRequest> req);
    ProductResponse getProductById(Long id);
    List<ProductResponse> getProductsById(List<Long> ids);
    List<ProductResponse> getTopSellingProducts();

    List<ProductResponse> useProduct(List<ProductStockRequest> reqs);
}
