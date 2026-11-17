package in.motorcircle.backend.quotation.api;

import java.time.Instant;
import java.util.List;

public record QuotationDto(
    String id,
    String groupId,
    DealerSummaryDto dealer,
    String status,
    QuotationVersionDto currentVersion,
    List<QuotationVersionDto> versionHistory,
    Instant createdAt,
    Instant updatedAt
) {}
