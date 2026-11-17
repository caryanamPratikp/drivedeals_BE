package in.motorcircle.backend.notification.api;

import in.motorcircle.backend.notification.domain.Notification;

import java.time.Instant;

public record NotificationDto(
        String id,
        String title,
        String message,
        String type,
        String link,
        boolean read,
        Instant createdAt
) {
    public static NotificationDto from(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getLink(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
