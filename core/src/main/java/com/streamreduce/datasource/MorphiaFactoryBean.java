package com.streamreduce.datasource;

import com.google.code.morphia.Morphia;
import com.google.code.morphia.validation.MorphiaValidation;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Copyright (C) 2010 SarathOnline.com.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


public class MorphiaFactoryBean extends AbstractFactoryBean<Morphia> {

    private String[] mapPackages;
    private String[] mapClasses;
    private boolean ignoreInvalidClasses;

    @Override
    public Class<?> getObjectType() {
        return Morphia.class;
    }

    @Override
    protected Morphia createInstance() throws Exception {
        Morphia m = new Morphia();

        // lightweight wrapper around the JSR303 API Validation Extension
        new MorphiaValidation().applyTo(m);

        if (mapPackages != null) {
            for (String packageName : mapPackages) {
                m.mapPackage(packageName, ignoreInvalidClasses);
            }
        }
        if (mapClasses != null) {
            for (String entityClass : mapClasses) {
                m.map(Class.forName(entityClass));
            }
        }
        return m;
    }

    public void setMapPackages(String[] mapPackages) {
        this.mapPackages = mapPackages;
    }

    public void setMapClasses(String[] mapClasses) {
        this.mapClasses = mapClasses;
    }

    public void setIgnoreInvalidClasses(boolean ignoreInvalidClasses) {
        this.ignoreInvalidClasses = ignoreInvalidClasses;
    }


}