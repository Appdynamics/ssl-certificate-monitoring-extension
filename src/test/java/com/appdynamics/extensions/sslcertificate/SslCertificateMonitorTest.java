package com.appdynamics.extensions.sslcertificate;


import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;

public class SslCertificateMonitorTest {

    SslCertificateMonitor monitor = new SslCertificateMonitor(5);

    @Test
    public void testSslCertificateMonitor() throws TaskExecutionException {
        Map<String,String> taskArgs = Maps.newHashMap();
        taskArgs.put("config-file","src/test/resources/conf/config.yml");
        TaskOutput output = monitor.execute(taskArgs, null);
        assertTrue(output.getStatusMessage().contains("successfully"));
    }

}
