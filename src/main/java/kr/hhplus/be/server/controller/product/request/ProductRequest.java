package kr.hhplus.be.server.controller.product.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductRequest {
    Long id;
    int stock;
}
