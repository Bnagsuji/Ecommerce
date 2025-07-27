package kr.hhplus.be.server.controller.order.response;

public record OrderHistoryResponse(
        Long id,
        String name,
        int quantity,
        String orderedDate
) {}