package kr.hhplus.be.server.controller.product.request;

public record ProductStockRequest(
        Long id,
        Integer quantity
) {
}
