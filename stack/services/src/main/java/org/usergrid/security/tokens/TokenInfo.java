package org.usergrid.security.tokens;

import java.util.Map;
import java.util.UUID;

import org.usergrid.security.AuthPrincipalInfo;

public class TokenInfo {

	UUID uuid;
	String type;
	long created;
	long accessed;
	AuthPrincipalInfo principal;
	Map<String, Object> state;

	public TokenInfo(UUID uuid, String type, long created, long accessed,
			AuthPrincipalInfo principal, Map<String, Object> state) {
		this.uuid = uuid;
		this.type = type;
		this.created = created;
		this.accessed = accessed;
		this.principal = principal;
		this.state = state;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setId(UUID uuid) {
		this.uuid = uuid;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public long getCreated() {
		return created;
	}

	public void setCreated(long created) {
		this.created = created;
	}

	public long getAccessed() {
		return accessed;
	}

	public void setAccessed(long accessed) {
		this.accessed = accessed;
	}

	public AuthPrincipalInfo getPrincipal() {
		return principal;
	}

	public void setPrincipal(AuthPrincipalInfo principal) {
		this.principal = principal;
	}

	public Map<String, Object> getState() {
		return state;
	}

	public void setState(Map<String, Object> state) {
		this.state = state;
	}

}
