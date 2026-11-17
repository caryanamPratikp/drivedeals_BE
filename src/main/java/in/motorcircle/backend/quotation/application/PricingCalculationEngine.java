package in.motorcircle.backend.quotation.application;

import in.motorcircle.backend.catalog.domain.CarVariant;
import in.motorcircle.backend.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PricingCalculationEngine {

    public static final BigDecimal TCS_THRESHOLD = new BigDecimal("1000000.00"); // 10 Lakhs INR
    public static final BigDecimal TCS_RATE = new BigDecimal("0.01"); // 1%

    public record PricingResult(
            BigDecimal grossTotal,
            BigDecimal totalDeductions,
            BigDecimal onRoadTotal,
            boolean tcsApplicable,
            BigDecimal calculatedTcs
    ) {}

    public PricingResult computeOnRoadPrice(
            CarVariant variant,
            BigDecimal exShowroom,
            BigDecimal registration,
            BigDecimal insurance,
            BigDecimal accessories,
            BigDecimal other,
            BigDecimal groupDiscount,
            BigDecimal corporateDiscount,
            BigDecimal exchangeBonus
    ) {
        // 1. Non-null and non-negative defaults
        BigDecimal ex = safeDecimal(exShowroom);
        BigDecimal reg = safeDecimal(registration);
        BigDecimal ins = safeDecimal(insurance);
        BigDecimal acc = safeDecimal(accessories);
        BigDecimal oth = safeDecimal(other);
        BigDecimal grpDisc = safeDecimal(groupDiscount);
        BigDecimal corpDisc = safeDecimal(corporateDiscount);
        BigDecimal exchBonus = safeDecimal(exchangeBonus);

        // 2. Validate mandatory group concession
        if (grpDisc.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("INVALID_DISCOUNT", "Group discount must be greater than zero.");
        }

        // 3. Ex-Showroom Benchmark Check: within 10% of variant OEM published price
        if (variant != null && variant.getExShowroomPrice() != null && variant.getExShowroomPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal benchmark = variant.getExShowroomPrice();
            BigDecimal diff = ex.subtract(benchmark).abs();
            BigDecimal maxAllowedDiff = benchmark.multiply(new BigDecimal("0.10"));
            if (diff.compareTo(maxAllowedDiff) > 0) {
                throw new BusinessRuleException("EX_SHOWROOM_MISMATCH",
                        String.format("Quoted ex-showroom ₹%s deviates by more than 10%% from catalogue benchmark ₹%s.", ex, benchmark));
            }
        }

        // 4. TCS Statutory Check: Section 206C(1F) of Indian Income Tax Act
        boolean tcsApplicable = ex.compareTo(TCS_THRESHOLD) > 0;
        BigDecimal calculatedTcs = tcsApplicable ? ex.multiply(TCS_RATE).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // 5. Line-item totals
        BigDecimal grossTotal = ex.add(reg).add(ins).add(acc).add(oth).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deductions = grpDisc.add(corpDisc).add(exchBonus).setScale(2, RoundingMode.HALF_UP);

        BigDecimal onRoadTotal = grossTotal.subtract(deductions).setScale(2, RoundingMode.HALF_UP);

        // 6. Floor check: prevent irrational negative or excessively sub-market quotes
        BigDecimal floorLimit = ex.multiply(new BigDecimal("0.70"));
        if (onRoadTotal.compareTo(floorLimit) < 0) {
            throw new BusinessRuleException("IRRATIONAL_TOTAL",
                    String.format("Calculated on-road price ₹%s is below realistic floor limit of 70%% of ex-showroom.", onRoadTotal));
        }

        return new PricingResult(grossTotal, deductions, onRoadTotal, tcsApplicable, calculatedTcs);
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("NEGATIVE_PRICE_COMPONENT", "Price line items cannot be negative.");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
