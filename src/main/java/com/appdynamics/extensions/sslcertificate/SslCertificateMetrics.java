package com.appdynamics.extensions.sslcertificate;


public class SslCertificateMetrics {
    public static final int DEFAULT_VALUE = -1;
    public static final String DAYS_TO_EXPIRY = "days_till_certificate_expiration";

    private String displayName;
    private int daysLeftToExpiry = DEFAULT_VALUE;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getDaysLeftToExpiry() {
        return daysLeftToExpiry;
    }

    public void setDaysLeftToExpiry(int daysLeftToExpiry) {
        this.daysLeftToExpiry = daysLeftToExpiry;
    }
}
