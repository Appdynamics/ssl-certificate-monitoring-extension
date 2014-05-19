package com.appdynamics.extensions.sslcertificate.common;


import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class SystemUtilTest {



    @Test
    public void canParseExpiryDate() throws IOException, InterruptedException {
        String expiryDate = "Aug  7 23:12:23 2014 GMT";
        Assert.assertNotNull(expiryDate);
        DateTime dt = SystemUtil.parseDate(expiryDate);
        Assert.assertNotNull(dt);
    }

}
