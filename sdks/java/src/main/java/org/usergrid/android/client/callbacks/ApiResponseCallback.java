package org.usergrid.android.client.callbacks;

import org.usergrid.android.client.response.ApiResponse;

public interface ApiResponseCallback extends ClientCallback<ApiResponse> {

	public void onResponse(ApiResponse response);

	public void onException(Exception e);

}
