package com.teapack.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "notification_reads", schema = "notification")
@IdClass(NotificationRead.PK.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRead {

    @Id
    @Column(name = "notification_id")
    private Long notificationId;

    @Id
    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @PrePersist
    public void prePersist() {
        if (readAt == null) readAt = LocalDateTime.now();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PK implements Serializable {
        private Long notificationId;
        private String username;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(notificationId, pk.notificationId)
                    && Objects.equals(username, pk.username);
        }

        @Override
        public int hashCode() {
            return Objects.hash(notificationId, username);
        }
    }
}
