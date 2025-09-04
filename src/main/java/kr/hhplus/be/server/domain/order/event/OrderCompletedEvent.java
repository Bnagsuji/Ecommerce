package kr.hhplus.be.server.domain.order.event;

import kr.hhplus.be.server.domain.order.external.CompleteMsg;

public record OrderCompletedEvent(
        CompleteMsg payload,
        String key
) {}