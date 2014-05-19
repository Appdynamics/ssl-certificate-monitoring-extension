package com.appdynamics.extensions.sslcertificate.common;


import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class SystemUtilTest {



    @Test
    public void canGetSSLExpiryDate() throws IOException, InterruptedException {
        String expiryDate = SystemUtil.getSSLCertificateExpirationDate("www.target.com",443,"./commands/openssl.sh");
        Assert.assertNotNull(expiryDate);
        DateTime dt = SystemUtil.parseDate(expiryDate);
        Assert.assertNotNull(dt);
    }

}
