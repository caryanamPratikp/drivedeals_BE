package in.motorcircle.backend.quotation.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public record QuotationSubmissionRequest(
    @NotNull(message = "Ex-showroom price is required")
    @DecimalMin(value = "100000.00", message = "Ex-showroom price must be at least ₹1,00,000")
    BigDecimal exShowroomPrice,

    @NotNull(message = "Registration charges are required")
    @DecimalMin(value = "0.00", message = "Registration charges cannot be negative")
    BigDecimal registrationCharges,

    @NotNull(message = "Insurance charges are required")
    @DecimalMin(value = "0.00", message = "Insurance charges cannot be negative")
    BigDecimal insuranceCharges,

    @DecimalMin(value = "0.00", message = "Accessories charges cannot be negative")
    BigDecimal accessoriesCharges,

    @DecimalMin(value = "0.00", message = "Other charges cannot be negative")
    BigDecimal otherCharges,

    @NotNull(message = "Group discount is required")
    @DecimalMin(value = "1.00", message = "Group discount must be at least ₹1")
    BigDecimal groupDiscount,

    @DecimalMin(value = "0.00", message = "Corporate discount cannot be negative")
    BigDecimal corporateDiscount,

    @DecimalMin(value = "0.00", message = "Exchange bonus cannot be negative")
    BigDecimal exchangeBonus,

    String accessoryPackage,

    @Min(value = 1, message = "Warranty must be at least 1 year")
    @Max(value = 10, message = "Warranty cannot exceed 10 years")
    int warrantyYears,

    @NotNull(message = "Quotation validity timestamp is required")
    @Future(message = "Quotation validity must be a future date")
    Instant validUntil,

    @Min(value = 1, message = "Delivery timeline must be at least 1 week")
    @Max(value = 52, message = "Delivery timeline cannot exceed 52 weeks")
    int deliveryTimelineWeeks
) {}
