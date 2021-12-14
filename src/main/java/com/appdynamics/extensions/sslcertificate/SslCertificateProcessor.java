/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.sslcertificate;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.sslcertificate.utils.SystemUtil;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.slf4j.Logger;

public class SslCertificateProcessor implements AMonitorTaskRunnable {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(SslCertificateProcessor.class);
    private static final String DAYS_TO_EXPIRY = "daysToExpiry";
    private static final String PREFIX = "notAfter=";
    private String metricPrefix;
    private String displayName;
    private String[] command;
    private MetricWriteHelper metricWriteHelper;
    private ITaskExecutor executor;



    public SslCertificateProcessor(String metricPrefix,
                                   String displayName,
                                   String[] command,
                                   ITaskExecutor executor,
                                   MetricWriteHelper metricWriteHelper) {
        this.metricPrefix = metricPrefix;
        this.displayName = displayName;
        this.command = command;
        this.metricWriteHelper = metricWriteHelper;
        this.executor = executor;
    }

    public void run() {
        long start = System.currentTimeMillis();
        logger.debug("Starting the ssl certificate processor for {}",displayName);
        String output = executor.execute(command);
        if(!Strings.isNullOrEmpty(output)){
            String expiryDate=output.substring(output.indexOf(PREFIX) + PREFIX.length());
            DateTime dt = SystemUtil.parseDate(expiryDate);
            if(dt != null) {
                int daysLeftToExpiry = Days.daysBetween(DateTime.now(), dt).getDays();
                String metricPath = metricPrefix + "|" + displayName + "|" + DAYS_TO_EXPIRY;
                logger.debug("Metric:{},Value:{}",metricPath,daysLeftToExpiry);
                metricWriteHelper.printMetric(metricPath,
                        Integer.toString(daysLeftToExpiry),
                        MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
            }
        }
        else{
            logger.error("Error fetching expiration date for {}",displayName);
        }

        logger.debug("Time taken for ssl certificate processor for {} is {}",
                displayName,
                System.currentTimeMillis() - start);
    }

    @Override
    public void onTaskComplete() {

    }
}
