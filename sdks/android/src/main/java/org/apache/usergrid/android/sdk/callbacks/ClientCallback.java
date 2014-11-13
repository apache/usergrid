package org.apache.usergrid.android.sdk.callbacks;

/**
 * Interface for all callback methods
 */
public interface ClientCallback<T> {

	public void onResponse(T response);

	public void onException(Exception e);

}
