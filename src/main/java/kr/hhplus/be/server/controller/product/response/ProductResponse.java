package kr.hhplus.be.server.controller.product.response;

public record ProductResponse(
        Long id,
        String name,
        int stock,
        int price,
        String regDate
){}