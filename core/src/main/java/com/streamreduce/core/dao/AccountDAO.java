package com.streamreduce.core.dao;

import com.google.code.morphia.Datastore;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.User;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

@Repository("accountDAO")
public class AccountDAO extends ValidatingDAO<Account> {

    @Autowired
    protected AccountDAO(@Qualifier(value="businessDBDatastore") Datastore datastore) {
        super(datastore);
    }

    public Account findByName(String name) {
        Assert.hasText(name);
        return ds.find(entityClazz, "name", name).get();
    }

    public List<User> getUsersForAccount(Account account) {
        Assert.notNull(account);
        return ds.find(User.class, "account", account).asList();
    }
}
