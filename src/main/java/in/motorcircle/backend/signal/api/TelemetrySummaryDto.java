package in.motorcircle.backend.signal.api;

import java.util.Map;

public record TelemetrySummaryDto(
    int totalSignals,
    Map<String, Integer> timelineBreakdown,
    Map<String, Integer> purchaseModeBreakdown,
    int exchangeCarCount
) {}
