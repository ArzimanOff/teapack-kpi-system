package com.teapack.notification.repository;

import com.teapack.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByShiftIdOrderByCreatedAtDesc(Long shiftId);
    List<Notification> findByLineIdOrderByCreatedAtDesc(String lineId);

    /**
     * Уведомления, которых ещё не «прочёл» данный пользователь.
     * NOT EXISTS вместо NOT IN — корректно при пустой таблице reads.
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE NOT EXISTS (
                SELECT 1 FROM NotificationRead r
                WHERE r.notificationId = n.id AND r.username = :username
            )
            ORDER BY n.createdAt DESC
            """)
    List<Notification> findUnreadForUser(@Param("username") String username);

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE NOT EXISTS (
                SELECT 1 FROM NotificationRead r
                WHERE r.notificationId = n.id AND r.username = :username
            )
            """)
    long countUnreadForUser(@Param("username") String username);
}
