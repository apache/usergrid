package org.apache.usergrid.apm.model;

import javax.persistence.*;

@Entity
@Table(name = "DEVICE_PLATFORM")
public class DevicePlatform {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String devicePlatform;

    /**
     * This will be null right now but later this could be set on per app basis
     */
    private Long appId;

    public DevicePlatform() {
        super();
    }

    public DevicePlatform(Long appId, String p) {
        this.appId = appId;
        this.devicePlatform = p;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDevicePlatform() {
        return devicePlatform;
    }

    public void setDevicePlatform(String devicePlatform) {
        this.devicePlatform = devicePlatform;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

}
