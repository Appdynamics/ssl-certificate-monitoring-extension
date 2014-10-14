package com.appdynamics.extensions.sslcertificate;

import com.appdynamics.extensions.sslcertificate.common.SystemUtil;
import com.appdynamics.extensions.sslcertificate.config.Domain;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.*;

/**
 * To run openssl on a domain and return its expiry time.
 */
public class SslCertificateMonitorTask implements Callable<SslCertificateMetrics>{

    public static final Logger logger = Logger.getLogger(SslCertificateMonitorTask.class);
    public static final String EXPIRY_DATE = "notAfter=";
    private ExecutorService threadPool = Executors.newFixedThreadPool(1);
    private Domain domain;
    private String commandFile;

    public SslCertificateMonitorTask(Domain domain,String commandFile){
        this.domain = domain;
        this.commandFile = commandFile;
    }


    public SslCertificateMetrics call() throws Exception {
        SslCertificateMetrics certificateMetrics = new SslCertificateMetrics();
        certificateMetrics.setDisplayName(domain.getDisplayName());
        try {
            String expiryDateString = getSSLCertificateExpirationDate(domain.getDomain(), domain.getPort(), commandFile);
            if (expiryDateString != null) {
                DateTime dt = SystemUtil.parseDate(expiryDateString);
                if(dt != null) {
                    int daysLeftToExpiry = Days.daysBetween(DateTime.now(),dt).getDays();
                    certificateMetrics.setDaysLeftToExpiry(daysLeftToExpiry);
                }
                else{
                    logger.error("Error in parsing the date for ::" + certificateMetrics.getDisplayName());
                }
            }
        }catch(Exception e){
            logger.error("Error getting SSL certificate expiration for ::" + certificateMetrics.getDisplayName() + e);
            certificateMetrics.setDaysLeftToExpiry(SslCertificateMetrics.DEFAULT_VALUE);
        }
        return certificateMetrics;
    }


    public String getSSLCertificateExpirationDate(String domain, int port, String commandFile) throws IOException, InterruptedException {
        BufferedReader b = null;
        try {
            String expiryDate = null;
            Process p = Runtime.getRuntime().exec(commandFile + " " + domain + " " + port);
            // Executing it in a separate thread as for Unix (if echo not used in open ssl command) and Windows
            // the openssl s_client command doesn't return. By executing it in a separate thread we can put
            // a timeout on the thread itself and collect the data from the result.
            Future<Process> futureProcess = threadPool.submit(new CommandWaiter(p));
            try {
                p = futureProcess.get(10, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                logger.error("Error executing openssl command:" + e);
            } catch (TimeoutException e) {
                //logger.error("Timed out openssl command" + e);
            }
            if(p != null) {
                b = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = "";

                while ((line = b.readLine()) != null) {
                    int idx = line.indexOf(EXPIRY_DATE);
                    if (idx >= 0) {
                        expiryDate = line.substring(idx + EXPIRY_DATE.length());
                        break;
                    }
                }
            }
            return expiryDate;
        }
        finally{
            if(b != null){
                b.close();
            }
            if(threadPool.isShutdown()){
                threadPool.shutdown();
            }
        }
    }

    private class CommandWaiter implements Callable<Process> {
        private Process process;

        public CommandWaiter(Process p) {
            this.process = p;
        }

        public Process call() throws Exception {
            try {
                process.waitFor();
            }
            catch(Exception e){
                logger.error("Error while waiting for the process to end " + e);
            }
            return process;
        }
    }
}
