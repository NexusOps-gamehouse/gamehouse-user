package gg.duo.user.dto;

import gg.duo.user.domain.notification.Notification;

import java.time.Instant;

public record NotificationDto(Long id, String message, String link, boolean read, Instant createdAt) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getMessage(), n.getLink(), n.isRead(), n.getCreatedAt());
    }
}
