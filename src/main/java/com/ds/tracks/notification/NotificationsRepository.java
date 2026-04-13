package com.ds.tracks.notification;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationsRepository extends MongoRepository<NotificationData, String>, NotificationDao {
}
