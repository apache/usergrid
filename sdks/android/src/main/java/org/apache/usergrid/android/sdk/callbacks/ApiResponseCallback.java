package org.apache.usergrid.android.sdk.callbacks;

import org.apache.usergrid.android.sdk.response.ApiResponse;

/**
 * Default callback for async requests that return an ApiResponse object
 */
public interface ApiResponseCallback extends ClientCallback<ApiResponse> {

	public void onResponse(ApiResponse response);

}
