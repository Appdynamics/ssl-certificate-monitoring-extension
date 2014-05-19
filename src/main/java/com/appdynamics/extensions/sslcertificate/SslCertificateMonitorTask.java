package com.appdynamics.extensions.sslcertificate;

import com.appdynamics.extensions.sslcertificate.common.SystemUtil;
import com.appdynamics.extensions.sslcertificate.config.Domain;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.concurrent.Callable;

/**
 * To run openssl on a domain and return its expiry time.
 */
public class SslCertificateMonitorTask implements Callable<SslCertificateMetrics>{

    public static final Logger logger = Logger.getLogger(SslCertificateMonitorTask.class);

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
            String expiryDateString = SystemUtil.getSSLCertificateExpirationDate(domain.getDomain(), domain.getPort(), commandFile);
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
}
