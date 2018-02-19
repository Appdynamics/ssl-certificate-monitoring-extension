/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.sslcertificate;

import com.appdynamics.extensions.sslcertificate.common.SystemUtilTest;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        SystemUtilTest.class,
        ProcessExecutorTest.class,
        SslCertificateProcessorTest.class
})
public class SslCertificateMonitorTestSuite {

    @BeforeClass
    public static void setup() throws IOException {
        String path = SslCertificateMonitorTestSuite.class.getResource("/cmd").getPath();
        System.out.println("Changing the file permissions.");
        Runtime.getRuntime().exec(new String[]{"chmod", "-R","+x",path});
    }
}
