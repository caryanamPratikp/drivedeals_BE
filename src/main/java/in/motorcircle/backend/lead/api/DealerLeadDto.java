package in.motorcircle.backend.lead.api;

import in.motorcircle.backend.lead.domain.Lead;
import in.motorcircle.backend.lead.domain.LeadStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DealerLeadDto(
        String id,
        String consentId,
        String dealerId,
        String buyerId,
        String buyerName,
        String buyerPhone,
        String buyerEmail,
        String buyerCity,
        String groupId,
        String carBrand,
        String carModel,
        String variantName,
        String quotationId,
        String quotationVersionId,
        BigDecimal agreedOnRoadPrice,
        BigDecimal totalDiscount,
        LeadStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<LeadEventDto> events
) {
    public static DealerLeadDto from(Lead lead, List<LeadEventDto> events) {
        var variant = lead.getQuotation().getGroup().getVariant();
        var quoteVer = lead.getQuotationVersion();
        BigDecimal totalDiscount = quoteVer.getGroupDiscount()
                .add(quoteVer.getCorporateDiscount())
                .add(quoteVer.getExchangeBonus());

        return new DealerLeadDto(
                lead.getId(),
                lead.getConsent().getId(),
                lead.getDealer().getId(),
                lead.getBuyer().getId(),
                lead.getBuyerName(),
                lead.getBuyerPhone(),
                lead.getBuyerEmail(),
                lead.getBuyerCity(),
                lead.getQuotation().getGroup().getId(),
                variant.getCar().getBrand(),
                variant.getCar().getModel(),
                variant.getName(),
                lead.getQuotation().getId(),
                quoteVer.getId(),
                quoteVer.getTotalOnRoadPrice(),
                totalDiscount,
                lead.getStatus(),
                lead.getCreatedAt(),
                lead.getUpdatedAt(),
                events != null ? events : List.of()
        );
    }
}
