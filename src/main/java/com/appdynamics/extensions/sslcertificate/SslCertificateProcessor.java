package com.appdynamics.extensions.sslcertificate;


import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.sslcertificate.common.SystemUtil;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslCertificateProcessor implements Runnable {

    public static final Logger logger = LoggerFactory.getLogger(SslCertificateProcessor.class);
    public static final String DAYS_TO_EXPIRY = "daysToExpiry";
    public static final String PREFIX = "notAfter=";

    protected MonitorConfiguration configuration;
    protected ITaskExecutor executor;
    protected String[] command;
    protected String displayName;

    public SslCertificateProcessor(String displayName,String[] command,MonitorConfiguration configuration,ITaskExecutor executor){
        this.displayName = displayName;
        this.configuration = configuration;
        this.executor = executor;
        this.command = command;
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
                String metricPath = configuration.getMetricPrefix() + "|" + displayName + "|" + DAYS_TO_EXPIRY;
                logger.debug("Metric:{},Value:{}",metricPath,daysLeftToExpiry);
                configuration.getMetricWriter().printMetric(metricPath,Integer.toString(daysLeftToExpiry), MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
            }
        }
        else{
            logger.error("Error fetching expiration date for {}",displayName);
        }

        logger.debug("Time taken for ssl certificate processor for {} is {}",displayName,System.currentTimeMillis() - start);
    }

}
