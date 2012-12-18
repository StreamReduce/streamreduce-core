package com.streamreduce.swagger;

import com.wordnik.swagger.jaxrs.JaxrsApiReader;

import javax.servlet.http.HttpServlet;

public class SwaggerBootstrap extends HttpServlet {
    static {
        JaxrsApiReader.setFormatString("");
    }
}
