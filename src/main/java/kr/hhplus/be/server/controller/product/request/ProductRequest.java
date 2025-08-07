package kr.hhplus.be.server.controller.product.request;

public record ProductRequest(
    Long id,
    String name,
    Integer stock,
    Long price
) {}

