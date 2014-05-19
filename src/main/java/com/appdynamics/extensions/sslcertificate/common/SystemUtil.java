package com.appdynamics.extensions.sslcertificate.common;


import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.*;

public class SystemUtil {

    public static final Logger logger = Logger.getLogger(SystemUtil.class);
    public static final String WINDOWS = "windows";
    public static final DateTimeFormatter dtf = DateTimeFormat.forPattern("MMM  d HH:mm:ss yyyy ZZZ");
    public static final DateTimeFormatter dtf1 = DateTimeFormat.forPattern("MMM d HH:mm:ss yyyy ZZZ");

    public static boolean isWindows(){
        String os = System.getProperty("os.name");
        if(os != null && os.toLowerCase().indexOf(WINDOWS) >= 0){
            return true;
        }
        return false;
    }


    /**
     * Have to use weird two patterns as openssl returns a space when the date is single digit.
     * @param expiryDate
     * @return
     */
    public static DateTime parseDate(String expiryDate){
        DateTime dateTime = null;
        try {
            dateTime = DateTime.parse(expiryDate, dtf);
        }
        catch(IllegalArgumentException iae){
            try {
                dateTime = DateTime.parse(expiryDate, dtf1);
            }
            catch(IllegalArgumentException ie){
                logger.error("Cannot parse the date " + expiryDate);
            }
        }
        return dateTime;
    }



}
