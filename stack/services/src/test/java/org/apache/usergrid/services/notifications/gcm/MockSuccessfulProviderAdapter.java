package org.apache.usergrid.services.notifications.gcm;

import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.services.notifications.ConnectionException;
import org.apache.usergrid.services.notifications.NotificationsService;
import org.apache.usergrid.services.notifications.ProviderAdapter;
import org.apache.usergrid.services.notifications.TaskTracker;

import java.util.Date;
import java.util.Map;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.services.ServicePayload;

public class MockSuccessfulProviderAdapter implements ProviderAdapter {

    private static ProviderAdapter realProviderAdapter;

    public static void install(NotificationsService ns) {
        if (realProviderAdapter != null)
            realProviderAdapter = ns.providerAdapters.get("google");
        ns.providerAdapters.put("google", new MockSuccessfulProviderAdapter());
    }

    public static void uninstall(NotificationsService ns) {
        if (realProviderAdapter != null) {
            ns.providerAdapters.put("google", realProviderAdapter);
        }
    }

    public MockSuccessfulProviderAdapter() {
    }

    @Override
    public void testConnection(Notifier notifier) throws ConnectionException {
    }

    @Override
    public String translatePayload(Object payload) throws Exception {
        return payload.toString();
    }

    @Override
    public Map<String, Date> getInactiveDevices(Notifier notifier,
            EntityManager em) throws Exception {
        return null;
    }

    @Override
    public void validateCreateNotifier(ServicePayload payload) throws Exception {
    }

    @Override
    public void doneSendingNotifications() throws Exception {
    }

    @Override
    public void sendNotification(String providerId, Notifier notifier,
            Object payload, Notification notification, final TaskTracker tracker)
            throws Exception {
        new Thread() {
            @Override
            public void run() {
                try {
                    tracker.completed();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
