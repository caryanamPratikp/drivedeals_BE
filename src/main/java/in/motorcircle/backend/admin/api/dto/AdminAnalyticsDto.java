package in.motorcircle.backend.admin.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminAnalyticsDto(
    long totalBuyers,
    long totalDealers,
    long verifiedDealers,
    long pendingDealers,
    long totalCars,
    long totalActiveGroups,
    long totalQuotations,
    long totalLeads,
    BigDecimal totalGMV,
    BigDecimal totalSavings,
    double avgDiscountPercentage,
    double conversionRate,
    List<HubMetric> hubDistribution,
    List<TimelineMetric> timeline
) {
    public record HubMetric(
        String hub,
        long buyers,
        long groups,
        long dealers,
        BigDecimal volume
    ) {}

    public record TimelineMetric(
        String label,
        BigDecimal dealVolume,
        BigDecimal savings,
        long activeDeals
    ) {}
}
