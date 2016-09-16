package com.appdynamics.extensions.sslcertificate;


import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SslCertificateMonitor extends AManagedMonitor {

    public static final Logger logger = LoggerFactory.getLogger(SslCertificateMonitor.class);
    private volatile boolean initialized;
    private MonitorConfiguration configuration;


    public SslCertificateMonitor(){
        System.out.println(logVersion());
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        if (!initialized) {
            initialize(taskArgs);
        }
        logger.debug("The raw arguments are {}", taskArgs);
        configuration.executeTask();
        return new TaskOutput("SSL Certificate Monitor Completed");
    }

    protected void initialize(Map<String, String> argsMap) {
        if (configuration == null) {
            MonitorConfiguration conf = new MonitorConfiguration("Custom Metrics|SslCertificate",new TaskRunnable(),MetricWriteHelperFactory.create(this));
            final String configFilePath = argsMap.get("config-file");
            conf.setConfigYml(configFilePath);
            conf.checkIfInitialized(MonitorConfiguration.ConfItem.METRIC_PREFIX, MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE, MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER);
            this.configuration = conf;
            this.initialized=true;
        }
    }

    private class TaskRunnable implements Runnable {

        public void run() {
            Map<String, ?> config = configuration.getConfigYml();
            if (config != null) {
                List<Map> sites = (List) config.get("domains");
                if (sites != null && !sites.isEmpty()) {
                    for (Map site : sites) {
                        File installDir = PathResolver.resolveDirectory(AManagedMonitor.class);
                        String cmdFile = (String)config.get("cmdFile");
                        String[] command = {PathResolver.getFile(cmdFile,installDir).getPath(),(String)site.get("domain"),((Integer)site.get("port")).toString()};
                        String displayName = (String)site.get("displayName");
                        //create a thread with a timeout. A timeout is needed because
                        //openssl command on windows may take forever to return. A workaround on windows is to use Cygwin's openssl.
                        SslCertificateProcessor task = new SslCertificateProcessor(displayName,command,configuration,new ProcessExecutor());
                        Future handle = configuration.getExecutorService().submit(task);
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


    public static String getImplementationVersion() {
        return SslCertificateMonitor.class.getPackage().getImplementationTitle();
    }

    private String logVersion() {
        String msg = "Using SSLCertificate Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        return msg;
    }


}
