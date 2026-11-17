package in.motorcircle.backend.notification.application;

import in.motorcircle.backend.identity.domain.User;
import in.motorcircle.backend.notification.api.NotificationDto;
import in.motorcircle.backend.notification.domain.Notification;
import in.motorcircle.backend.notification.infrastructure.NotificationRepository;
import in.motorcircle.backend.shared.exception.ResourceNotFoundException;
import in.motorcircle.backend.shared.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification createNotification(User recipient, String title, String message, String type, String link) {
        Notification notification = new Notification(
                null,
                recipient,
                title,
                message,
                type,
                link,
                false
        );

        Notification saved = notificationRepository.save(notification);
        log.info("[NOTIFICATION_CREATED] Created notification [{}] for user [{}]", saved.getId(), recipient.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(User user, boolean unreadOnly) {
        List<Notification> list = unreadOnly
                ? notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId())
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        return list.stream().map(NotificationDto::from).toList();
    }

    @Transactional
    public void markAsRead(User user, String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to modify this notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }
}
