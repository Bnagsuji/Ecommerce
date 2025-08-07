package kr.hhplus.be.server.controller.account.response;

import kr.hhplus.be.server.controller.account.request.TransactionType;

import java.time.LocalDateTime;

public record AccountHistoryResponse(
        Long userId, // "CHARGE" or "USE"z
        Long amount,
        TransactionType type,
        LocalDateTime createdDate
) {}
