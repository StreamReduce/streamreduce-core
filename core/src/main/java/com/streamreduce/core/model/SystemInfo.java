package com.streamreduce.core.model;

import com.google.code.morphia.annotations.Entity;

@Entity(value = "systemInfo", noClassnameStored = true)
public class SystemInfo extends ObjectWithId {

    private static final long serialVersionUID = 5114858763625888907L;
    private String appVersion;
    private String buildNumber;
    private String dbVersion;
    private Integer patchLevel;

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getDbVersion() {
        return dbVersion;
    }

    public void setDbVersion(String dbVersion) {
        this.dbVersion = dbVersion;
    }

    public Integer getPatchLevel() {
        return patchLevel;
    }

    public void setPatchLevel(Integer patchLevel) {
        this.patchLevel = patchLevel;
    }
}
