package org.apache.usergrid.android.client.callbacks;

import org.usergrid.java.client.response.ApiResponse;


public interface ApiResponseCallback extends ClientCallback<ApiResponse> {

	public void onResponse(ApiResponse response);

}
