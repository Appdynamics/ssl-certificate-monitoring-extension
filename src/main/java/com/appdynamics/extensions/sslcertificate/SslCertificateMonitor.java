/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.sslcertificate;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.util.PathResolver;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SslCertificateMonitor extends ABaseMonitor {

    private static final Logger logger = LoggerFactory.getLogger(SslCertificateMonitor.class);

    private volatile boolean initialized;

    @Override
    protected String getDefaultMetricPrefix() {
        return "Custom Metrics|SslCertificate";
    }

    @Override
    public String getMonitorName() {
        return "SSLCertificateMonitor";
    }

    @Override
    protected List<Map<String, ?>> getServers() {
        return null;
    }

    @Override
    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        Map<String, ?> config = getContextConfiguration().getConfigYml();
        String metricPrefix = getContextConfiguration().getMetricPrefix();
        if (config != null) {
            List<Map<String, ?>> sites = (List<Map<String, ?>>) config.get("domains");
            if (sites != null && !sites.isEmpty()) {
                for (Map<String, ?> site : sites) {
                    File installDir = PathResolver.resolveDirectory(AManagedMonitor.class);
                    String cmdFile = (String) config.get("cmdFile");
                    String[] command = {
                            PathResolver.getFile(cmdFile,installDir).getPath(),
                            (String)site.get("domain"),
                            ((Integer)site.get("port")).toString()
                    };
                    String displayName = (String) site.get("displayName");
                    //create a thread with a timeout. A timeout is needed because
                    //openssl command on windows may take forever to return. A workaround on windows is to use Cygwin's openssl.
                    SslCertificateProcessor task = new SslCertificateProcessor(
                            metricPrefix,
                            displayName,
                            command,
                            new ProcessExecutor(),
                            tasksExecutionServiceProvider.getMetricWriteHelper()
                            );
                    Future handle = getContextConfiguration().getContext().getExecutorService().submit(displayName, task);
                    try {
                        handle.get(((Integer)config.get("threadTimeout")).longValue(),TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        logger.error("Task interrupted for {}",displayName, e);
                    } catch (ExecutionException e) {
                        logger.error("Task execution failed for {}",displayName, e);
                    } catch (TimeoutException e) {
                        logger.error("Task timed out for {}",displayName,e);
                    }
                }
            }
        }
    }
}
