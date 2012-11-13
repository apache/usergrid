package org.usergrid.persistence.cassandra.util;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

/**
 * Use Aspects to apply a trace
 *
 * @author zznate
 */
public class TraceTagAspect {
    private static final Logger logger = LoggerFactory.getLogger(TraceTagAspect.class);

    @Resource
    private TraceTagManager traceTagManager;

    public Object applyTrace(ProceedingJoinPoint pjp) throws Throwable {
        String tagName = pjp.toLongString();
        logger.info("Applyng trace on {}", tagName);
        TimedOpTag timedOpTag = traceTagManager.timerInstance();
        boolean success = true;
        try {
            return pjp.proceed();
        } catch (Exception e) {
            success = false;
            throw e;
        } finally {
            timedOpTag.stopAndApply(tagName,success);
            traceTagManager.addTimer(timedOpTag);
            logger.debug("TimedOpTag added in Aspect on {}", tagName);
        }

    }
}
