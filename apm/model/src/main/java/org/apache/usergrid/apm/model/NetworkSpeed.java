package org.apache.usergrid.apm.model;

import javax.persistence.*;

@Entity
@Table(name = "NETWORK_SPEED")
public class NetworkSpeed {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    String networkSpeed;

    /**
     * This will be null right now but later this could be set on per app basis
     */
    private Long appId;

    public NetworkSpeed() {
        super();
    }

    public NetworkSpeed(Long appId, String networkSpeed) {
        this.appId = null;
        this.networkSpeed = networkSpeed;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNetworkSpeed() {
        return networkSpeed;
    }

    public void setNetworkSpeed(String networkSpeed) {
        this.networkSpeed = networkSpeed;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

}
