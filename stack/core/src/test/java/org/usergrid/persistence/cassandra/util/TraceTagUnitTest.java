package org.usergrid.persistence.cassandra.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author zznate
 */
public class TraceTagUnitTest {

    private TraceTagManager traceTagManager;
    private Slf4jTraceTagReporter traceTagReporter;
    private TaggedOpTimer taggedOpTimer;

    @Before
    public void setup() {
        traceTagManager = new TraceTagManager();
        traceTagReporter = new Slf4jTraceTagReporter();
        taggedOpTimer = new TaggedOpTimer(traceTagManager,traceTagReporter);
    }

    @Test
    public void createAttachDetach() throws Exception {
        TraceTag traceTag = traceTagManager.create("testtag1");
        traceTagManager.attach(traceTag);
        TimedOpTag timedOpTag = (TimedOpTag)taggedOpTimer.start();
        Thread.currentThread().sleep(500);
        taggedOpTimer.stop(timedOpTag,"op-tag-name",true);
        assertTrue(timedOpTag.getElapsed() >= 500);
        assertEquals(traceTag, timedOpTag.getTraceTag());
    }
}
