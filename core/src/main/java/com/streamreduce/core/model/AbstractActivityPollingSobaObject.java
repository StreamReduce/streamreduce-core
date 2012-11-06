package com.streamreduce.core.model;

import com.google.code.morphia.annotations.Embedded;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

public abstract class AbstractActivityPollingSobaObject extends SobaObject {

    private static final long serialVersionUID = -5301159528986967143L;
    @Embedded
    @NotNull
    private Map<String, String> metadata = new HashMap<String, String>();

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public void setLastActivityPollDate(Date lastActivityPollDate) {
        if (getMetadata() == null) {
            setMetadata(new HashMap<String, String>());
        }

        getMetadata().put("last_activity_poll", Long.toString(lastActivityPollDate.getTime()));
    }

    public Date getLastActivityPollDate() {
        Date defaultLastActivityPollDate = new Date(getCreated());

        if (getMetadata() == null) {
            return defaultLastActivityPollDate;
        } else {
            try {
                return new Date(Long.valueOf(getMetadata().get("last_activity_poll")));
            } catch (NumberFormatException e) {
                return defaultLastActivityPollDate;
            }
        }
    }

}
