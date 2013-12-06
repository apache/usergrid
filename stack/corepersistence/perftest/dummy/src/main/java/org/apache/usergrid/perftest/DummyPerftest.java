package org.apache.usergrid.perftest;


/**
 * Created with IntelliJ IDEA. User: akarasulu Date: 12/6/13 Time: 1:07 AM To change this template use File | Settings |
 * File Templates.
 */
@SuppressWarnings( "UnusedDeclaration" )
public class DummyPerftest implements Perftest {
    private Dummy dummy = new Dummy();

    @Override
    public long getCallCount() {
        return 1000;
    }


    @Override
    public int getThreadCount() {
        return 10;
    }


    @Override
    public long getDelayBetweenCalls() {
        return 0;
    }


    @Override
    public void call() {
        dummy.foobar();
    }
}
