package com.streamreduce.core;

import com.streamreduce.NotFoundException;
import com.streamreduce.core.dao.DAODatasourceType;

import org.bson.types.ObjectId;

public class CollectionObjectNotFoundException extends NotFoundException {

    private static final long serialVersionUID = -126370387997570427L;

    public CollectionObjectNotFoundException(DAODatasourceType datasourceType, String collectionName, ObjectId id) {
        super("[" + datasourceType + "]: Unable to find a document with the id of " + id.toString() + " in the " +
                collectionName + " collection.");
    }

}
