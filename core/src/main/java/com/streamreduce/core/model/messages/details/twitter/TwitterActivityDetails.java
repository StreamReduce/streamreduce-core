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

package com.streamreduce.core.model.messages.details.twitter;

import com.streamreduce.core.model.messages.details.AbstractMessageDetails;
import com.streamreduce.core.model.messages.details.MessageDetailsType;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;
import net.sf.json.JSONObject;

public class TwitterActivityDetails extends AbstractMessageDetails implements SobaMessageDetails {

    private JSONObject profile;
    private int favoritesCount;
    private int followersCount;
    private int friendsCount;
    private int statusesCount;

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageDetailsType getMessageDetailsType() {
        return MessageDetailsType.TWITTER_ACTIVITY;
    }

    public JSONObject getProfile() {
        return profile;
    }

    public void setProfile(JSONObject profile) {
        this.profile = profile;
    }

    public int getFavoritesCount() {
        return favoritesCount;
    }

    public void setFavoritesCount(int favoritesCount) {
        this.favoritesCount = favoritesCount;
    }

    public int getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(int followersCount) {
        this.followersCount = followersCount;
    }

    public int getFriendsCount() {
        return friendsCount;
    }

    public void setFriendsCount(int friendsCount) {
        this.friendsCount = friendsCount;
    }

    public int getStatusesCount() {
        return statusesCount;
    }

    public void setStatusesCount(int statusesCount) {
        this.statusesCount = statusesCount;
    }

    public static class Builder {
        TwitterActivityDetails details;

        public Builder() {
            details = new TwitterActivityDetails();
        }

        public Builder profile(JSONObject profile) {
            details.setProfile(profile);
            return this;
        }

        public Builder favorites(int favoritesCount) {
            details.setFavoritesCount(favoritesCount);
            return this;
        }

        public Builder followers(int followersCount) {
            details.setFollowersCount(followersCount);
            return this;
        }

        public Builder friends(int friendsCount) {
            details.setFriendsCount(friendsCount);
            return this;
        }

        public Builder statuses(int statusesCount) {
            details.setStatusesCount(statusesCount);
            return this;
        }

        public TwitterActivityDetails build() {
            return details;
        }
    }

}
