package kr.hhplus.be.server.controller.account.request;

import jakarta.validation.constraints.Min;
import lombok.*;

@Builder
public record AccountRequest(
    Long userId,
    @Min(1)
    Long amount
) {}


