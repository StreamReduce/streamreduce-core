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
