package com.usergrid.count;

import com.usergrid.count.common.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A BatchSubmitter that prints contents to the configured slf4j logger logger
 * @author zznate
 */
public class Slf4JBatchSubmitter implements BatchSubmitter {

    private Logger log = LoggerFactory.getLogger(Slf4JBatchSubmitter.class);

    @Override
    public void submit(SimpleBatcher.Batch batch) {
        for (Count c : batch.getCounts() ) {
            log.info("Found count {}",c);
        }
    }
}
