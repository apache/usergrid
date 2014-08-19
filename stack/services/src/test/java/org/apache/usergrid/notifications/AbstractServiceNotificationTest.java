package org.apache.usergrid.notifications;

import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Receipt;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.services.notifications.NotificationsService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.usergrid.services.AbstractServiceIT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AbstractServiceNotificationTest extends AbstractServiceIT {
    private NotificationsService ns;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void beforeClass() {
    }

    @Before
    public void before() throws Exception {

    }

    protected NotificationsService getNotificationService(){
        ns = (NotificationsService) app.getSm().getService("notifications");
        return ns;
    }

    protected Notification scheduleNotificationAndWait(Notification notification)
            throws Exception {
        getNotificationService().getQueueManager().processBatchAndReschedule(notification,null);
        long timeout = System.currentTimeMillis() + 60000;
        while (System.currentTimeMillis() < timeout) {
            Thread.sleep(200);
            notification = app.getEm().get(notification.getUuid(),
                    Notification.class);
            if (notification.getFinished() != null) {
                return notification;
            }
        }
        fail("Notification failed to complete");
        return null;
    }

    protected List<EntityRef> getNotificationReceipts(EntityRef notification)
            throws Exception {
        Results r = app.getEm().getCollection(notification,
                Notification.RECEIPTS_COLLECTION, null, 1000000,
                Query.Level.REFS, false);
        List<EntityRef> list =new ArrayList<EntityRef>();//get all
        PagingResultsIterator it = new PagingResultsIterator(r);
        while(it.hasNext()){
           list.add((EntityRef)it.next());
        }
        return list;
    }

    protected void checkReceipts(Notification notification, int expected)
            throws Exception {
        List<EntityRef> receipts = getNotificationReceipts(notification);
        long timeout = System.currentTimeMillis() + 60000;
        while (System.currentTimeMillis() < timeout) {
            Thread.sleep(200);
            receipts =getNotificationReceipts(notification);
            if (receipts.size()==expected) {
                 break;
            }
        }
        assertEquals(expected, receipts.size());
        for (EntityRef receipt : receipts) {
            Receipt r = app.getEm().get(receipt, Receipt.class);
            assertNotNull(r.getSent());
            assertNotNull(r.getPayload());
            assertNotNull(r.getNotifierId());
            EntityRef source = getNotificationService().getSourceNotification(r);
            assertEquals(source.getUuid(), notification.getUuid());
        }
    }

    protected void checkStatistics(Notification notification, long sent,  long errors) throws Exception{
        Map<String, Long> statistics = null;
        long timeout = System.currentTimeMillis() + 60000;
        while (System.currentTimeMillis() < timeout) {
            Thread.sleep(200);
            statistics = app.getEm().get(notification.getUuid(), Notification.class).getStatistics();
            if (statistics.get("sent")==sent && statistics.get("errors")==errors) {
                break;
            }
        }
        assertEquals(sent, statistics.get("sent").longValue());
        assertEquals(errors, statistics.get("errors").longValue());
    }

}
