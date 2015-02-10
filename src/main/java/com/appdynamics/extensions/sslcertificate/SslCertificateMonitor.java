package com.appdynamics.extensions.sslcertificate;


import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.sslcertificate.common.SystemUtil;
import com.appdynamics.extensions.sslcertificate.config.ConfigUtil;
import com.appdynamics.extensions.sslcertificate.config.Configuration;
import com.appdynamics.extensions.sslcertificate.config.Domain;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * This extension
 */
public class SslCertificateMonitor extends AManagedMonitor {

    public static final Logger logger = Logger.getLogger(SslCertificateMonitor.class);
    public static final String CONFIG_ARG = "config-file";
    public static final String METRIC_SEPARATOR = "|";
    private static final int DEFAULT_NUMBER_OF_THREADS = 10;
    public static final int DEFAULT_THREAD_TIMEOUT = 30;

    private ExecutorService threadPool;
    //To load the config files
    private final static ConfigUtil<Configuration> configUtil = new ConfigUtil<Configuration>();



    public SslCertificateMonitor(){
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        if(taskArgs != null) {
            logger.info(" Starting the SSL Certificate Monitoring task.");
            if (logger.isDebugEnabled()) {
                logger.debug("Task Arguments Passed ::" + taskArgs);
            }
            String configFilename = getConfigFilename(taskArgs.get(CONFIG_ARG));
            try {
                //read the config.
                Configuration config = configUtil.readConfig(configFilename, Configuration.class);
                threadPool = Executors.newFixedThreadPool(config.getNumberOfThreads() == 0 ? DEFAULT_NUMBER_OF_THREADS : config.getNumberOfThreads());
                //create parallel tasks to get the ssl certificate for each domain
                List<Future<SslCertificateMetrics>> parallelTasks = createConcurrentTasks(config);
                //collect the metrics
                List<SslCertificateMetrics> sslCertMetrics = collectMetrics(parallelTasks,config.getThreadTimeout() == 0 ? DEFAULT_THREAD_TIMEOUT : config.getThreadTimeout());
                //print the metrics
                printStats(config, sslCertMetrics);
                return new TaskOutput("SSL Certificate monitoring task completed successfully.");
            } catch (FileNotFoundException e) {
                logger.error("Config file not found :: " + configFilename, e);
            } catch (Exception e) {
                logger.error("Metrics collection failed", e);
            } finally {
                if(!threadPool.isShutdown()){
                    threadPool.shutdown();
                }
            }
        }

        throw new TaskExecutionException("SSL Certificate monitoring task completed with failures.");
    }

    private List<SslCertificateMetrics> collectMetrics(List<Future<SslCertificateMetrics>> parallelTasks,int timeout) {
        List<SslCertificateMetrics> allMetrics = new ArrayList<SslCertificateMetrics>();
        for (Future<SslCertificateMetrics> aParallelTask : parallelTasks) {
            SslCertificateMetrics certMetricsForDomain = null;
            try {
                certMetricsForDomain = aParallelTask.get(timeout, TimeUnit.SECONDS);
                allMetrics.add(certMetricsForDomain);
            } catch (InterruptedException e) {
                logger.error("Task interrupted." + e);
            } catch (ExecutionException e) {
                logger.error("Task execution failed." + e);
            } catch (TimeoutException e) {
                logger.error("Task timed out." + e);
            }
        }
        return allMetrics;
    }

    private void printStats(Configuration config, List<SslCertificateMetrics> zMetrics) {
        for (SslCertificateMetrics certMetric : zMetrics) {
            StringBuffer metricPath = new StringBuffer();
            metricPath.append(config.getMetricPrefix()).append(certMetric.getDisplayName()).append(METRIC_SEPARATOR)
                    .append(SslCertificateMetrics.DAYS_TO_EXPIRY);
            printCollectiveObservedCurrent(metricPath.toString(),Integer.toString(certMetric.getDaysLeftToExpiry()));
        }
    }



    private void printCollectiveObservedCurrent(String metricPath, String metricValue) {
        printMetric(metricPath, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
    }

    /**
     * A helper method to report the metrics.
     * @param metricPath
     * @param metricValue
     * @param aggType
     * @param timeRollupType
     * @param clusterRollupType
     */
    private void printMetric(String metricPath,String metricValue,String aggType,String timeRollupType,String clusterRollupType){
        MetricWriter metricWriter = getMetricWriter(metricPath,
                aggType,
                timeRollupType,
                clusterRollupType
        );
           System.out.println("Sending [" + aggType + METRIC_SEPARATOR + timeRollupType + METRIC_SEPARATOR + clusterRollupType
                    + "] metric = " + metricPath + " = " + metricValue);
        if (logger.isDebugEnabled()) {
            logger.debug("Sending [" + aggType + METRIC_SEPARATOR + timeRollupType + METRIC_SEPARATOR + clusterRollupType
                    + "] metric = " + metricPath + " = " + metricValue);
        }
        metricWriter.printMetric(metricValue);
    }

    private List<Future<SslCertificateMetrics>> createConcurrentTasks(Configuration config) throws Exception{
        List<Future<SslCertificateMetrics>> parallelTasks = new ArrayList<Future<SslCertificateMetrics>>();
        String commandFile = getConfigFilename(config.getCmdFile());
        if(config != null && config.getDomains() != null){
            for(Domain domain : config.getDomains()){
                SslCertificateMonitorTask monitorTask = new SslCertificateMonitorTask(domain,commandFile);
                parallelTasks.add(getThreadPool().submit(monitorTask));
            }
        }
        return parallelTasks;
    }


    public static String getImplementationVersion() {
        return SslCertificateMonitor.class.getPackage().getImplementationTitle();
    }

    /**
     * Returns a config file name,
     * @param filename
     * @return String
     */
    private String getConfigFilename(String filename) {
        if(filename == null){
            return "";
        }
        //for absolute paths
        if(new File(filename).exists()){
            return filename;
        }
        //for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if(!Strings.isNullOrEmpty(filename)){
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }


    public ExecutorService getThreadPool() {
        return threadPool;
    }
}
