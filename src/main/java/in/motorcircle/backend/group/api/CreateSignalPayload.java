package in.motorcircle.backend.group.api;

import in.motorcircle.backend.signal.domain.PurchaseMode;
import in.motorcircle.backend.signal.domain.TimelineMonths;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSignalPayload(
    @NotNull(message = "Purchase timeline is required")
    TimelineMonths timelineMonths,

    @NotNull(message = "Purchase mode is required")
    PurchaseMode purchaseMode,

    boolean exchangeCar,

    @Size(max = 255, message = "Exchange car details cannot exceed 255 characters")
    String exchangeCarDetails,

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    String notes
) {}
