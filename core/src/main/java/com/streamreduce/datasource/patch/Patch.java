package com.streamreduce.datasource.patch;

import com.streamreduce.core.ApplicationManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public abstract class Patch {

    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    public abstract void applyPatch(ApplicationManager applicationManager, ApplicationContext applicationContext) throws PatchException;


}
