package com.streamreduce.servlet;


import com.streamreduce.storm.RunStorm;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;


@WebListener
public class StandaloneServletContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        RunStorm.main(new String[]{"local"});
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
