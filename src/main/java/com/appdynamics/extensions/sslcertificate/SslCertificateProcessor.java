/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.sslcertificate;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslCertificateProcessor implements AMonitorTaskRunnable {

    private static final Logger logger = LoggerFactory.getLogger(SslCertificateProcessor.class);
    private static final String DAYS_TO_EXPIRY = "daysToExpiry";
    private final String metricPrefix;
    private final String displayName;
    private final String domain;
    private final Integer port;
    private final SSLSocketFactory sslSocketFactory;
    private final MetricWriteHelper metricWriteHelper;

    public SslCertificateProcessor(final String metricPrefix, final String displayName, final String domain, final Integer port,
            final SSLSocketFactory sslSocketFactory, final MetricWriteHelper metricWriteHelper) {
        this.metricPrefix = metricPrefix;
        this.displayName = displayName;
        this.domain = domain;
        this.port = port;
        this.sslSocketFactory = sslSocketFactory;
        this.metricWriteHelper = metricWriteHelper;
    }

    public void run() {
        final String metricPath = String.format("%s|%s|%s", metricPrefix, displayName, DAYS_TO_EXPIRY);
        final long start = System.currentTimeMillis();
        try {
            final SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(domain, port);

            final AtomicReference<X509Certificate> x509CertificateReference = new AtomicReference<>();
            final CountDownLatch cdl = new CountDownLatch(1);
            sslSocket.addHandshakeCompletedListener(new HandshakeCompletedListener() {
                @Override
                public void handshakeCompleted(HandshakeCompletedEvent event) {
                    System.out.println(event);
                    try {
                        final Certificate[] peerCertificates = event.getPeerCertificates();
                        final X509Certificate x509Certificate = (X509Certificate) peerCertificates[0];
                        x509CertificateReference.set(x509Certificate);
                    } catch (final Exception e) {
                        logger.error("Unable to retrieve peer certificates", e);
                    }
                    cdl.countDown();
                }
            });

            sslSocket.startHandshake();
            cdl.await();
            sslSocket.close();

            if( x509CertificateReference.get() != null ) {
                final X509Certificate x509Certificate = x509CertificateReference.get();
                final Date notAfter = x509Certificate.getNotAfter();
                final ZonedDateTime notAfterZDT= ZonedDateTime.ofInstant(notAfter.toInstant(), ZoneOffset.UTC);
                final long daysLeftToExpiry =  ChronoUnit.DAYS.between(ZonedDateTime.now(ZoneOffset.UTC), notAfterZDT);
                logger.debug("Metric:{},Value:{}", metricPath, daysLeftToExpiry);
                metricWriteHelper.printMetric(metricPath,
                        Long.toString(daysLeftToExpiry),
                        MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
            } else {
                logger.error("Error fetching expiration date for {}",displayName);
            }
        } catch (final UnknownHostException e) {
            logger.error("Unknown hostname [{}] provided for {}", domain, displayName, e);
        } catch (final IOException e) {
            logger.error("IOException checking certificate for {}", displayName, e);
        } catch (final InterruptedException e) {
            logger.error("Interrupted while checking certificate for {}", displayName, e);
        }
        
        logger.debug("Time taken for ssl certificate processor for {} is {}",
                displayName,
                System.currentTimeMillis() - start);
    }

    @Override
    public void onTaskComplete() {
    }
}
