package kr.hhplus.be.server.controller.account.request;

import lombok.*;

@Builder
public record AccountRequest(
    Long userId,
    Long amount
) {}


