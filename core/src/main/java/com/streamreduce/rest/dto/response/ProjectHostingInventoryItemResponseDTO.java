package com.streamreduce.rest.dto.response;


public class ProjectHostingInventoryItemResponseDTO extends AbstractConnectionInventoryItemResponseDTO {

    private String key;
    private String url; 
    private boolean isPublic;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

}
