/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.sslcertificate;


import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.sslcertificate.utils.SystemUtil;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.junit.Test;

import java.util.concurrent.*;

import static org.mockito.Mockito.*;

public class SslCertificateProcessorTest {

    private ExecutorService threadPool = Executors.newFixedThreadPool(2);

    @Test(expected = TimeoutException.class)
    public void throwExceptionIfCommandExceedsTimeout() throws Exception {
        MonitorConfiguration configuration = new MonitorConfiguration("Custom Metrics|SSLCertificateMonitor", new Runnable() {
            public void run() {

            }
        }, mock(MetricWriteHelper.class));
        String[] cmd = {this.getClass().getResource("/cmd/infinite_script.sh").getFile(),"www.google.com","443"};
        if(SystemUtil.isWindows()){
            cmd = new String[]{this.getClass().getResource("/cmd/infinite_script.bat").getFile(),"www.google.com","443"};
        }
        SslCertificateProcessor testProcessor = new SslCertificateProcessor("test",cmd,configuration,new ProcessExecutor());
        Future handle = threadPool.submit(testProcessor);
        handle.get(3, TimeUnit.SECONDS);
    }

    @Test
    public void runSuccessFullyForValidCommand() throws Exception {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        MonitorConfiguration configuration = new MonitorConfiguration("Custom Metrics|SSLCertificateMonitor", new Runnable() {
            public void run() {

            }
        }, writer);
        String[] cmd = {this.getClass().getResource("/cmd/success_script.sh").getFile()};
        if(SystemUtil.isWindows()){
            cmd = new String[]{this.getClass().getResource("/cmd/success_script.bat").getFile()};
        }
        SslCertificateProcessor testProcessor = new SslCertificateProcessor("Google",cmd,configuration,new ProcessExecutor());
        Future handle = threadPool.submit(testProcessor);
        handle.get(30, TimeUnit.SECONDS);
        verify(writer,times(1)).printMetric(eq("Custom Metrics|SSLCertificateMonitor|Google|"+SslCertificateProcessor.DAYS_TO_EXPIRY),anyString(),
                eq(MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE),eq(MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE),eq(MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL));
    }


}
