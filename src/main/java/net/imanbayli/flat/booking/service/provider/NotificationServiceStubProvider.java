package net.imanbayli.flat.booking.service.provider;

import net.imanbayli.flat.booking.service.NotificationService;

public class NotificationServiceStubProvider implements NotificationService {

    @Override
    public void send(String userId, String message) {
        System.out.println(message + " sent to userId: "+userId);
    }
}
