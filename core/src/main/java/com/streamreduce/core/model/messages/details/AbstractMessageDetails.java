/*
 * Copyright 2012 Nodeable Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.streamreduce.core.model.messages.details;

import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.jackson.annotate.JsonIgnore;

public abstract class AbstractMessageDetails {

    protected String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String cleanHTMLString(String dirty) {
        if (dirty == null) {
            return null;
        }
        return StringEscapeUtils.unescapeHtml(dirty.replaceAll("\\<.*?>", "")
                .replaceAll("\\s", " ")
                .replaceAll(" +", " ")
                .trim());
    }

    @JsonIgnore
    private void setMessageDetailsType(MessageDetailsType messageDetailsType) {
    } //This exists to please the Jackson Gods.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractMessageDetails that = (AbstractMessageDetails) o;

        if (title != null ? !title.equals(that.title) : that.title != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return title != null ? title.hashCode() : 0;
    }
}
