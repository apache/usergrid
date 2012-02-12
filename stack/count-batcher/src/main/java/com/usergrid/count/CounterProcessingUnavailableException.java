package com.usergrid.count;

/**
 * @author zznate
 */
public class CounterProcessingUnavailableException extends RuntimeException {

    private static final String ERR_MSG = "Counter was not processed. Reason: ";

    public CounterProcessingUnavailableException() {
        super(ERR_MSG);
    }

    public CounterProcessingUnavailableException(String errMsg) {
        super(ERR_MSG + errMsg);
    }

    public CounterProcessingUnavailableException(String errMsg, Throwable t) {
        super(ERR_MSG + errMsg, t);
    }
}
