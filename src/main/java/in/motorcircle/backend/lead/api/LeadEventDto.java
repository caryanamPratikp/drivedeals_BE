package in.motorcircle.backend.lead.api;

import in.motorcircle.backend.lead.domain.LeadEvent;

import java.time.Instant;

public record LeadEventDto(
        String id,
        String leadId,
        String actorId,
        String actorName,
        String eventType,
        String description,
        Instant createdAt
) {
    public static LeadEventDto from(LeadEvent event) {
        return new LeadEventDto(
                event.getId(),
                event.getLead().getId(),
                event.getActor().getId(),
                event.getActor().getName(),
                event.getEventType(),
                event.getDescription(),
                event.getCreatedAt()
        );
    }
}
