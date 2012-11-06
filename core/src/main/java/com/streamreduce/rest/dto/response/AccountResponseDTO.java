package com.streamreduce.rest.dto.response;


public class AccountResponseDTO extends ObjectWithIdResponseDTO {

    private String name;
    private String url;
    private String description;
    private String fuid;

    public AccountResponseDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFuid() {
        return fuid;
    }

    public void setFuid(String fuid) {
        this.fuid = fuid;
    }

}
