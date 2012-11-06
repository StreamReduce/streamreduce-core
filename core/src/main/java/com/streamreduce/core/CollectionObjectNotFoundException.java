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
