package com.streamreduce.rest.dto.response;

public class SystemInfoResponseDTO extends ObjectWithIdResponseDTO {

    private String appVersion;
    private String buildNumber;
    private String dbVersion;
    private Integer patchLevel;

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
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
