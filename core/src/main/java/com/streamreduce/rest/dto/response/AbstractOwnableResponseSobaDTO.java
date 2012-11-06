package com.streamreduce.rest.dto.response;

// if the object can be User "owned"
public abstract  class AbstractOwnableResponseSobaDTO extends SobaObjectResponseDTO {

    private boolean isOwner;

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean isOwner) {
        this.isOwner = isOwner;
    }

}
