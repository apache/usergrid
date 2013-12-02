package org.apache.usergrid.persistence.collection.mvcc.stage;


import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.connectionpool.OperationResult;


/** @author tnine */
public interface WriteListener extends ListenableFuture<OperationResult<Void>> {}
