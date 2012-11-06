package com.streamreduce.core.model.messages.details.jira;


import org.junit.Assert;
import org.junit.Test;

public class JiraActivityDetailsTest {

    @Test
    public void testJiraActivityDetailsTestFromBuilder() {
        JiraActivityDetails details = new JiraActivityDetails.Builder()
                .html("     <p>Hello World</p>       ")
                .build();

        Assert.assertEquals("<p>Hello World</p>", details.getHtml());
    }
}
