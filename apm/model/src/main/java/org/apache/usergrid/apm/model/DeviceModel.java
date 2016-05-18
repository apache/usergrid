package org.apache.usergrid.apm.model;

import javax.persistence.*;

@Entity
@Table(name = "DEVICE_MODEL")
public class DeviceModel {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceModel;

    /**
     * This will be null right now but later this could be set on per app basis
     */
    private Long appId;

    public DeviceModel() {
        super();
    }

    public DeviceModel(Long appId, String m) {
        this.appId = appId;
        this.deviceModel = m;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

}
