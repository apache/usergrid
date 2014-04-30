package org.apache.usergrid.android.client.callbacks;

public interface ClientCallback<T> {

	public void onResponse(T response);

	public void onException(Exception e);

}
