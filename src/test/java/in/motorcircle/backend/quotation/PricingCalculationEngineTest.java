package in.motorcircle.backend.quotation;

import in.motorcircle.backend.catalog.domain.CarVariant;
import in.motorcircle.backend.quotation.application.PricingCalculationEngine;
import in.motorcircle.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingCalculationEngineTest {

    private PricingCalculationEngine pricingEngine;
    private CarVariant cretaVariant;

    @BeforeEach
    void setUp() {
        pricingEngine = new PricingCalculationEngine();
        cretaVariant = new CarVariant();
        cretaVariant.setExShowroomPrice(new BigDecimal("2015000.00"));
    }

    @Test
    @DisplayName("On-road price formula calculates exact rupee sum with TCS")
    void testAccurateOnRoadFormula() {
        BigDecimal exShowroom = new BigDecimal("2015000.00");
        BigDecimal registration = new BigDecimal("372775.00");
        BigDecimal insurance = new BigDecimal("65000.00");
        BigDecimal accessories = new BigDecimal("25000.00");
        BigDecimal other = new BigDecimal("3500.00");
        BigDecimal groupDiscount = new BigDecimal("45000.00");
        BigDecimal corpDiscount = new BigDecimal("10000.00");
        BigDecimal exchangeBonus = new BigDecimal("15000.00");

        var result = pricingEngine.computeOnRoadPrice(
                cretaVariant,
                exShowroom,
                registration,
                insurance,
                accessories,
                other,
                groupDiscount,
                corpDiscount,
                exchangeBonus
        );

        // Gross = 20,15,000 + 3,72,775 + 65,000 + 25,000 + 3,500 = 24,81,275
        // Deductions = 45,000 + 10,000 + 15,000 = 70,000
        // On-Road = 24,81,275 - 70,000 = 24,11,275.00
        assertThat(result.grossTotal()).isEqualByComparingTo(new BigDecimal("2481275.00"));
        assertThat(result.totalDeductions()).isEqualByComparingTo(new BigDecimal("70000.00"));
        assertThat(result.onRoadTotal()).isEqualByComparingTo(new BigDecimal("2411275.00"));
        assertThat(result.tcsApplicable()).isTrue();
        assertThat(result.calculatedTcs()).isEqualByComparingTo(new BigDecimal("20150.00")); // 1% of 20,15,000
    }

    @Test
    @DisplayName("Zero or negative group discount throws BusinessRuleException")
    void testZeroGroupDiscountRejected() {
        assertThatThrownBy(() -> pricingEngine.computeOnRoadPrice(
                cretaVariant,
                new BigDecimal("2015000.00"),
                new BigDecimal("300000.00"),
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO, // Zero discount
                BigDecimal.ZERO,
                BigDecimal.ZERO
        )).isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("Group discount must be greater than zero");
    }

    @Test
    @DisplayName("Quoted ex-showroom deviating > 2% from catalogue benchmark is rejected")
    void testExShowroomDeviationRejected() {
        // Benchmark is 20,15,000. Quoted is 22,00,000 (> 9% deviation)
        assertThatThrownBy(() -> pricingEngine.computeOnRoadPrice(
                cretaVariant,
                new BigDecimal("2200000.00"),
                new BigDecimal("300000.00"),
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("25000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        )).isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("deviates by more than 2%");
    }
}
