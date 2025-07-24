package kr.hhplus.be.server.domain.product.response;

public record ProductResponse(
        Long id,
        String name,
        int stock,
        int price,
        String regDate
){}