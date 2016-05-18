package org.apache.usergrid.apm.model;

import javax.persistence.*;

@Entity
@Table(name = "NETWORK_CARRIERS")
public class NetworkCarrier {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String networkCarrier;

    /**
     * This will be null right now but later this could be set on per app basis
     */
    private Long appId;

    public NetworkCarrier() {
        super();
    }

    public NetworkCarrier(Long appId, String networkCarrier) {
        this.appId = null;
        this.networkCarrier = networkCarrier;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNetworkCarrier() {
        return networkCarrier;
    }

    public void setNetworkCarrier(String networkCarrier) {
        this.networkCarrier = networkCarrier;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

}
