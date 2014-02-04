package org.usergrid.management.export;


import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.batch.JobExecution;
import org.usergrid.mq.Message;
import org.usergrid.persistence.PathQuery;
import org.usergrid.services.AbstractCollectionService;
import org.usergrid.services.ServiceContext;
import org.usergrid.services.ServiceParameter;
import org.usergrid.services.ServiceResults;


/**
 *
 *
 */
public class ExportServiceImpl extends AbstractCollectionService {

    private static final Logger LOG = LoggerFactory.getLogger(ExportServiceImpl.class);

    // ms to add when scheduling to ensure scheduler doesn't miss processing
    private static final long SCHEDULER_GRACE_PERIOD = 250;

    // number of jobs processed per batch (not private & final for testing
    // access)
    public static int BATCH_SIZE = 1000;

    // period to tell job scheduler to wait between heartbeats before timing out
    // this transaction
    private static final long SCHEDULER_HEARTBEAT_PERIOD = 5 * 60 * 1000;

    // period to poll for batch completion (keep well under Job scheduler
    // heartbeat value!)
    private static final long BATCH_POLL_PERIOD = 60 * 1000;

    // timeout for message queue transaction
    private static final long MESSAGE_TRANSACTION_TIMEOUT = SCHEDULER_HEARTBEAT_PERIOD;

    static final long BATCH_DEATH_PERIOD = 10 * 60 * 1000;

    public static final String NOTIFIER_ID_POSTFIX = ".export.id";

    static final String MESSAGE_PROPERTY_DEVICE_UUID = "jobUUID";

    static final String MESSAGE_PROPERTY_JOB_UUID = "jobUUID";

    private static final String NOTIFICATION_CONCURRENT_BATCHES = "export.concurrent.batches";

    private static final int QUEUING_HEARTBEAT_EVERY = 2000;

    private static final int QUEUE_UNDERFLOW_WAIT = 15000;

    // If this property is set Notifications are automatically expired in
    // the isOkToSent() method after the specified number of milliseconds
    //get explanation on this.
    public static final String PUSH_AUTO_EXPIRE_AFTER_PROPNAME = "usergrid.push-auto-expire-after";
    private Long pushAutoExpireAfter = null;

    static {
        Message.MESSAGE_PROPERTIES.put( MESSAGE_PROPERTY_JOB_UUID, UUID.class);
    }

    //create pool here to check for in active jobs.

    public ExportServiceImpl() {
        super();
        LOG.info("/export");
    }

    @Autowired
    public void setProperties( Properties usergridProperties) {

        String autoExpireAfterString = (String)
                usergridProperties.getProperty( PUSH_AUTO_EXPIRE_AFTER_PROPNAME );

        if (autoExpireAfterString != null) {
            pushAutoExpireAfter = Long.parseLong( autoExpireAfterString );
        }
    }

    private PathQuery<String> getPathQuery(List<ServiceParameter> parameters) {
        //TODO:Fill in export specific getPathQuery
        PathQuery pathQuery = null;
        return pathQuery;
    }

    @Override
    public ServiceResults postCollection(ServiceContext context) throws Exception {
        //difference between getOriginalParameters and getParameters.
        //PathQuery<String> pathQuery = getPathQuery(context.getRequest().getOriginalParameters());
        System.out.println("Magic");
        //ServiceResults results = super.postCollection( context );

        return super.postCollection( context );
    }

    private void safeHeartbeat(JobExecution jobExecution) {

        if (jobExecution != null) {
            try {
                jobExecution.heartbeat(SCHEDULER_HEARTBEAT_PERIOD);
            } catch (Exception e) {
                LOG.debug("heartbeat failure ignored");
            }
        }
    }
}
