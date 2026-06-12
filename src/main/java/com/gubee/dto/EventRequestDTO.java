package com.gubee.dto;

import com.gubee.model.enums.EventType;
import java.time.Instant;

public record EventRequestDTO(
        String eventId,
        EventType type,
        Instant occurredAt,
        String marketplace,
        String accountId,
        String externalOrderId,
        String sku,
        Integer quantity,
        Integer available,
        String reason,
        Integer quantitySent
) {}