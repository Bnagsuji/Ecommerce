package kr.hhplus.be.server.controller.product.response;

import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        Long price,
        int stock,
        LocalDateTime regDate
){}