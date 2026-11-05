/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.sslcertificate;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.PathResolver;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SslCertificateMonitor extends ABaseMonitor {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(SslCertificateMonitor.class);

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
        return Lists.newArrayList();
    }

    static String[] buildCommand(String scriptPath, Map<String, ?> site) {
        String domain = (String) site.get("domain");
        String port = site.get("port").toString();
        Object targetHost = site.get("targetHost");
        String connectTo = (targetHost != null && !targetHost.toString().trim().isEmpty())
                ? targetHost.toString().trim() : domain;
        // OpenSSL's -connect host:port needs IPv6 literals in brackets, e.g. [2001:db8::5]:443.
        if (connectTo.indexOf(':') >= 0 && InetAddresses.isInetAddress(connectTo)) {
            connectTo = "[" + connectTo + "]";
        }
        // The third argument is the SNI hostname; IP literals are not valid SNI values, so it is omitted for them.
        if (InetAddresses.isInetAddress(domain)) {
            return new String[]{scriptPath, connectTo, port};
        }
        return new String[]{scriptPath, connectTo, port, domain};
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
                    String[] command = buildCommand(PathResolver.getFile(cmdFile, installDir).getPath(), site);
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
