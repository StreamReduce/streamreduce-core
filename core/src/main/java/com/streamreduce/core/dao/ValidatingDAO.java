package com.streamreduce.core.dao;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Key;
import com.google.code.morphia.dao.BasicDAO;
import com.mongodb.WriteConcern;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolationException;

public abstract class ValidatingDAO<T> extends BasicDAO<T, ObjectId> {

    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    protected ValidatingDAO(Datastore ds) {
        super(ds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Key<T> save(T entity) {
        try {
            return super.save(entity);
        } catch (ConstraintViolationException e) {
            logError(entity, e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Key<T> save(T entity, WriteConcern wc) {
        try {
            return super.save(entity, wc);
        } catch (ConstraintViolationException e) {
            logError(entity, e);
            throw e;
        }
    }

    private void logError(T entity, ConstraintViolationException e) {
        // Not logging this as an error because it's a user-created error.  Logging occurs in
        // debug mode in case we have validation errors during development that we don't expect.
        logger.debug("Unable to persist entity (" + entity.getClass() + "): " + e.getMessage());
    }

}
