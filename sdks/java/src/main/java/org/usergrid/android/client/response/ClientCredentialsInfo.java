package org.usergrid.android.client.response;

import org.codehaus.jackson.annotate.JsonProperty;
import org.usergrid.android.client.Utils;

public class ClientCredentialsInfo {

	private String id;
	private String secret;

	public ClientCredentialsInfo(String id, String secret) {
		this.id = id;
		this.secret = secret;
	}

	@JsonProperty("client_id")
	public String getId() {
		return id;
	}

	@JsonProperty("client_id")
	public void setId(String id) {
		this.id = id;
	}

	@JsonProperty("client_secret")
	public String getSecret() {
		return secret;
	}

	@JsonProperty("client_secret")
	public void setSecret(String secret) {
		this.secret = secret;
	}

	@Override
	public String toString() {
		return Utils.toJsonString(this);
	}

}
