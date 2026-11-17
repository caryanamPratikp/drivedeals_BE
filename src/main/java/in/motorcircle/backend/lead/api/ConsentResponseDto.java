package in.motorcircle.backend.lead.api;

import in.motorcircle.backend.lead.domain.Consent;
import in.motorcircle.backend.lead.domain.ConsentStatus;

import java.time.Instant;

public record ConsentResponseDto(
        String id,
        String buyerId,
        String dealerId,
        String dealerBusinessName,
        String groupId,
        String quotationId,
        String quotationVersionId,
        ConsentStatus status,
        Instant grantedAt,
        Instant revokedAt,
        String revocationReason,
        String leadId,
        String leadStatus
) {
    public static ConsentResponseDto from(Consent consent) {
        return from(consent, null, null);
    }

    public static ConsentResponseDto from(Consent consent, String leadId, String leadStatus) {
        return new ConsentResponseDto(
                consent.getId(),
                consent.getUser().getId(),
                consent.getDealer().getId(),
                consent.getDealer().getDealershipName(),
                consent.getQuotation().getGroup().getId(),
                consent.getQuotation().getId(),
                consent.getQuotationVersion().getId(),
                consent.getStatus(),
                consent.getGrantedAt(),
                consent.getRevokedAt(),
                consent.getRevocationReason(),
                leadId,
                leadStatus
        );
    }
}
