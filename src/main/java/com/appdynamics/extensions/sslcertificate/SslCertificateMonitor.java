/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.sslcertificate;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslCertificateMonitor extends ABaseMonitor {

    private static final Logger logger = LoggerFactory.getLogger(SslCertificateMonitor.class);

    private final SSLSocketFactory sslSocketFactory;

    public SslCertificateMonitor() {
        try {
            final SSLContext permitAllSSLContext = SSLContext.getInstance("TLS");
            permitAllSSLContext.init(null, new TrustManager[] { PERMIT_ALL_TRUST_MANAGER }, null);

            sslSocketFactory = permitAllSSLContext.getSocketFactory();
        } catch (final NoSuchAlgorithmException e) {
            logger.error("Unable to initialize TLS context for certificate checking", e);
            throw new IllegalStateException("Unable to initialize TLS context for certificate checking", e);
        } catch (final KeyManagementException e) {
            logger.error("Key Management error while initializing TLS context for certificate checking", e);
            throw new IllegalStateException("Key Management error while initializing TLS context for certificate checking", e);
        }
    }

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

    @Override
    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        final Map<String, ?> config = getContextConfiguration().getConfigYml();
        final long threadTimeout = ((Integer)config.get("threadTimeout")).longValue();

        final String metricPrefix = getContextConfiguration().getMetricPrefix();
        
        if (null != config) {
            @SuppressWarnings("unchecked")
            final List<Map<String, ?>> sites = (List<Map<String, ?>>) config.get("domains");
            if (null != sites && !sites.isEmpty()) {
                for (final Map<String, ?> site : sites) {
                    final String displayName = (String) site.get("displayName");
                    final String domain = (String) site.get("domain");
                    final Integer port = (Integer) site.get("port");
                    //create a thread with a timeout. A timeout is needed because
                    //openssl command on windows may take forever to return. A workaround on windows is to use Cygwin's openssl.
                    final SslCertificateProcessor task = new SslCertificateProcessor(
                            metricPrefix,
                            displayName,
                            domain,
                            port,
                            sslSocketFactory,
                            tasksExecutionServiceProvider.getMetricWriteHelper());
                    final Future<?> handle = getContextConfiguration().getContext().getExecutorService().submit(displayName, task);
                    try {
                        handle.get(threadTimeout,TimeUnit.SECONDS);
                    } catch (final InterruptedException e) {
                        logger.error("Task interrupted for {}",displayName, e);
                    } catch (final ExecutionException e) {
                        logger.error("Task execution failed for {}",displayName, e);
                    } catch (final TimeoutException e) {
                        logger.error("Task timed out for {}",displayName,e);
                    }
                }
            }
        }
    }

    private static final X509TrustManager PERMIT_ALL_TRUST_MANAGER = new X509TrustManager() {

        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException { }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException { }

        @Override
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }

    };
}
