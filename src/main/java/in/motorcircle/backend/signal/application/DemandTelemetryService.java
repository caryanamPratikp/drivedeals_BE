package in.motorcircle.backend.signal.application;

import in.motorcircle.backend.signal.api.TelemetrySummaryDto;
import in.motorcircle.backend.signal.domain.BuyerSignal;
import in.motorcircle.backend.signal.infrastructure.BuyerSignalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DemandTelemetryService {

    private final BuyerSignalRepository signalRepository;

    public DemandTelemetryService(BuyerSignalRepository signalRepository) {
        this.signalRepository = signalRepository;
    }

    @Transactional(readOnly = true)
    public TelemetrySummaryDto getAggregatedTelemetry(String groupId) {
        List<BuyerSignal> signals = signalRepository.findByGroupId(groupId);

        Map<String, Integer> timelineMap = new HashMap<>();
        Map<String, Integer> purchaseModeMap = new HashMap<>();
        int exchangeCount = 0;

        for (BuyerSignal s : signals) {
            timelineMap.merge(s.getTimelineMonths().name(), 1, Integer::sum);
            purchaseModeMap.merge(s.getPurchaseMode().name(), 1, Integer::sum);
            if (s.isExchangeCar()) {
                exchangeCount++;
            }
        }

        return new TelemetrySummaryDto(
                signals.size(),
                timelineMap,
                purchaseModeMap,
                exchangeCount
        );
    }
}
