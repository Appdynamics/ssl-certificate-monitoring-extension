package com.appdynamics.extensions.sslcertificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Executes the Command, reads the data from the OutputStream and returns it.
 * <p/>
 * Created by abey.tom on 3/31/15.
 */
public class ProcessExecutor implements ITaskExecutor{
    public static final Logger logger = LoggerFactory.getLogger(ProcessExecutor.class);

    public String execute(String[] commands) {
        try {
            logger.info("Executing the command {}", Arrays.toString(commands));
            Process process = Runtime.getRuntime().exec(commands);
            new ErrorReader(process.getErrorStream()).start();
            OutputReader outputReader = new OutputReader(process.getInputStream());
            outputReader.start();
            process.waitFor();
            outputReader.join();
            return outputReader.getData();
        } catch (Exception e) {
            logger.error("Error while executing the process " + Arrays.toString(commands), e);
            return "";
        }
    }


    /**
     * Listens to the Output Stream and gets the data.
     */
    public static class OutputReader extends Thread {
        public static final Logger logger = LoggerFactory.getLogger(ErrorReader.class);

        private final InputStream in;
        private StringBuffer data = new StringBuffer("");

        public OutputReader(InputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String temp;
            try {
                while ((temp = reader.readLine()) != null) {
                    data.append(temp);
                }
            } catch (IOException e) {
                logger.debug("Error while reading the contents of the output stream", e);
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        public String getData() {
            return data.toString();
        }
    }


    /**
     * Listens to the Error Stream and logs the response.
     */
    public static class ErrorReader extends Thread {
        public static final Logger logger = LoggerFactory.getLogger(ErrorReader.class);


        private final InputStream in;

        public ErrorReader(InputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String temp;
            try {
                while ((temp = reader.readLine()) != null) {
                    logger.error("Process Error - {}", temp);
                }
            } catch (IOException e) {
                logger.debug("Error while reading the contents of the the error stream", e);
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
            logger.trace("Closing the Error Reader {}", Thread.currentThread().getName());
        }
    }

}



