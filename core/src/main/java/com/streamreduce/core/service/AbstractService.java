package com.streamreduce.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class all Nodeable services should extend.  Provides some plumbing common
 * amongst all services.
 */
public abstract class AbstractService {

    protected transient Logger logger = LoggerFactory.getLogger(getClass());

}
