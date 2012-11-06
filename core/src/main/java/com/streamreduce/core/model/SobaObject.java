package com.streamreduce.core.model;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Transient;
import com.streamreduce.util.HashtagUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.ScriptAssert;

@ScriptAssert.List({
        @ScriptAssert(script = "_this instanceof com.streamreduce.core.model.User || _this.account != undefined",
                lang = "javascript",
                message = "account may not be null"), // Validate account for all non-User object since User validates itself
        @ScriptAssert(script = "_this instanceof com.streamreduce.core.model.User || (_this.alias != undefined && _this.alias.trim().length() > 0)",
                lang = "javascript",
                message = "alias may not be empty"), // Validate alias for all non-User object since User validates itself
        @ScriptAssert(script = "!(_this.updatedViaREST && _this.visibility.toString() == 'PUBLIC')",
                lang = "javascript",
                message = "visibility cannot be set to 'PUBLIC'") // Validate that visibility is not PUBLIC when updated via REST
})
public abstract class SobaObject extends ObjectWithId implements Taggable {

    private static final long serialVersionUID = 5391473553602970543L;
    @Indexed
    // @NotEmpty <<< Handled by ScriptAssert above
    @Size(max = 128, min = 1)
    protected String alias;
    @Transient
    protected int DESCRIPTION_MAX = 512; // enforced on setter until the service layer can deal
    protected String description;
    @Indexed
    @Reference
    // @NotNull <<< Handled by ScriptAssert above
    protected Account account;
    @Indexed
    @Reference
    protected User user;
    @NotNull
    protected Visibility visibility = Visibility.ACCOUNT;
    @Embedded
    @NotNull
    protected Set<String> hashtags = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    public enum Visibility {
        SELF,
        GROUP,
        ACCOUNT,
        PUBLIC;

        public int intValue() {
            switch (this) {
                case SELF:
                    return 1;
                case GROUP:
                    return 10;
                case ACCOUNT:
                    return 20;
                case PUBLIC:
                    return 30;
                default:
                    return 0;
            }
        }
    }

    @Override
    public void mergeWithJSON(JSONObject json) {
        super.mergeWithJSON(json);

        if (json != null) {
            if (json.containsKey("alias")) {
                setAlias(json.getString("alias"));
            }
            if (json.containsKey("description")) {
                setDescription(json.getString("description"));
            }
            if (json.containsKey("visibility")) {
                setVisibility(Visibility.valueOf(json.getString("visibility")));
            }
            if (json.containsKey("hashtags")) {
                setHashtags(new HashSet(json.getJSONArray("hashtags")));
            }
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description != null && description.length() > DESCRIPTION_MAX) {
            description = description.substring(0, DESCRIPTION_MAX - 1);
        }
        this.description = description;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        if (visibility == null) {
            throw new IllegalArgumentException("visibility can't be null");
        }
        this.visibility = visibility;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        if (StringUtils.isBlank(alias)) {
            throw new IllegalArgumentException("alias can't be blank");
        }
        this.alias = alias;
    }

    @Override
    public void addHashtags(Set<String> hashtags) {
        setHashtags(hashtags);
    }

    @Override
    public void addHashtag(String tag) {
        if (tag != null && !tag.isEmpty()) {
            tag = HashtagUtil.normalizeTag(tag);
            if (!hashtags.contains(tag)) {
                hashtags.add(tag);
            }
        }
    }

    @Override
    public void removeHashtag(String tag) {
        if (tag != null && !tag.isEmpty()) {
            tag = HashtagUtil.normalizeTag(tag);
            hashtags.remove(tag);
        }
    }

    public Set<String> getHashtags() {
        return hashtags;
    }

    public void setHashtags(Set<String> hashtags) {
        if (hashtags != null) {
            for (String tag : hashtags) {
                addHashtag(tag);
            }
        }
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @SuppressWarnings("rawtypes")
    public static abstract class Builder<T extends SobaObject, S extends Builder> {
        protected T theObject;

        /**
         * Constructor.
         *
         * @param theObject instance of the actual object to create.
         */
        public Builder(T theObject) {
            this.theObject = theObject;
        }

        protected boolean isBuilt = false;

        /**
         * This method will return the real builder object instead of one of its parents.
         *
         * @return the actual buidler being used by the caller
         */
        public abstract S getRealBuilder();

        /**
         * Builds the object and returns it. (Cannot be called more than once.)
         *
         * @return
         */
        public T build() {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built");
            }
            if (theObject.getAccount() == null) {
                throw new IllegalStateException("SobaObject must have an account set");
            }
            if (theObject.getUser() == null) {
                throw new IllegalStateException("SobaObject must have a user set");
            }
            return theObject;
        }

        public S account(Account account) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built.");
            }
            theObject.setAccount(account);
            return getRealBuilder();
        }

        /**
         * @param user Defines the {@link User} and the {@link Account} for this Object.
         * @return
         */
        public S user(User user) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built.");
            }
            theObject.setUser(user);
            theObject.setAccount(user.getAccount());
            return getRealBuilder();
        }

        public S alias(String alias) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built.");
            }
            theObject.setAlias(alias);
            return getRealBuilder();
        }

        public S description(String description) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built.");
            }
            theObject.setDescription(description);
            return getRealBuilder();
        }

        @SuppressWarnings("unchecked")
        public S hashtags(Set<String> hashtags) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built.");
            }
            theObject.setHashtags(hashtags);
            return getRealBuilder();
        }

        public S hashtag(String hashtag) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built.");
            }
            theObject.addHashtag(hashtag);
            return getRealBuilder();
        }

        public S visibility(Visibility visibility) {
            if (isBuilt) {
                throw new IllegalStateException("The object cannot be modified after built.");
            }
            // if null use object default
            if (visibility != null) {
                theObject.setVisibility(visibility);
            }
            return getRealBuilder();
        }
    }

}


