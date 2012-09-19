package org.usergrid.security.tokens;

import java.util.Map;
import java.util.UUID;

import org.usergrid.security.AuthPrincipalInfo;

public class TokenInfo {

	UUID uuid;
	String type;
	long created;
	long accessed;
	long inactive;
	//total duration, in milliseconds
	long duration;
	AuthPrincipalInfo principal;
	Map<String, Object> state;

	public TokenInfo(UUID uuid, String type, long created, long accessed,
			long inactive, long duration, AuthPrincipalInfo principal,
			Map<String, Object> state) {
		this.uuid = uuid;
		this.type = type;
		this.created = created;
		this.accessed = accessed;
		this.inactive = inactive;
		this.principal = principal;
		this.duration = duration;
		this.state = state;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
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

	/**
     * @return the expiration
     */
    public long getDuration() {
        return duration;
    }
    
    /**
     * If the expiration is undefined, return the default that's been passed in
     * @param defaultExpiration
     * @return
     */
    public long getExpiration(long defaultExpiration){
        if(duration == 0){
            return defaultExpiration;
        }
        return duration;
    }

    /**
     * @param expiration the expiration to set
     */
    public void setDuration(long expiration) {
        this.duration = expiration;
    }

    public long getAccessed() {
		return accessed;
	}

	public void setAccessed(long accessed) {
		this.accessed = accessed;
	}

	public long getInactive() {
		return inactive;
	}

	public void setInactive(long inactive) {
		this.inactive = inactive;
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
