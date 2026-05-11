package com.teapack.notification.repository;

import com.teapack.notification.entity.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface NotificationReadRepository
        extends JpaRepository<NotificationRead, NotificationRead.PK> {

    @Query("SELECT r.notificationId FROM NotificationRead r WHERE r.username = :username")
    Set<Long> findReadIdsByUsername(@Param("username") String username);

    boolean existsByNotificationIdAndUsername(Long notificationId, String username);

    long countByUsername(String username);

    @Modifying
    @Query(value = """
            INSERT INTO notification.notification_reads (notification_id, username, read_at)
            SELECT n.id, :username, NOW()
            FROM notification.notifications n
            WHERE NOT EXISTS (
                SELECT 1 FROM notification.notification_reads r
                WHERE r.notification_id = n.id AND r.username = :username
            )
            """, nativeQuery = true)
    int markAllReadForUser(@Param("username") String username);
}
