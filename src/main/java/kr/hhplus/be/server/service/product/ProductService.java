package kr.hhplus.be.server.service.product;

import kr.hhplus.be.server.controller.product.request.ProductRequest;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import java.util.List;



public interface ProductService {
    ProductResponse getProductById(Long id);
    List<ProductResponse> getProductByIdIn(List<Long> ids);
    List<ProductResponse> getTopSellingProducts();
    List<ProductResponse> useProduct(List<ProductRequest> reqs);
}
