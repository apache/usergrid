package com.usergrid.count.common;

/**
 * @author zznate
 */
public class CountTransportSerDeException extends RuntimeException {
    private static final String DEF_MSG = "There was a serialization/deserialization problem in Count transport. Reason: ";

    public CountTransportSerDeException() {
        super(DEF_MSG);
    }

    public CountTransportSerDeException(String msg) {
        super(DEF_MSG + msg);
    }

    public CountTransportSerDeException(String msg, Throwable t) {
        super(DEF_MSG + msg, t);
    }
}
