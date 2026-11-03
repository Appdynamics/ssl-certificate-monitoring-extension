package com.appdynamics.extensions.sslcertificate;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SslCertificateMonitorTest {

    private Map<String, Object> site(String domain, Integer port, Object targetHost) {
        Map<String, Object> site = new HashMap<>();
        site.put("domain", domain);
        site.put("port", port);
        if (targetHost != null) {
            site.put("targetHost", targetHost);
        }
        return site;
    }

    @Test
    public void withoutTargetHostConnectsToDomain() {
        String[] cmd = SslCertificateMonitor.buildCommand("openssl.sh", site("example.com", 443, null));
        Assert.assertArrayEquals(new String[]{"openssl.sh", "example.com", "443", "example.com"}, cmd);
    }

    @Test
    public void withTargetHostConnectsToTargetAndKeepsDomainAsSni() {
        String[] cmd = SslCertificateMonitor.buildCommand("openssl.sh", site("example.com", 443, "10.0.0.5"));
        Assert.assertArrayEquals(new String[]{"openssl.sh", "10.0.0.5", "443", "example.com"}, cmd);
    }

    @Test
    public void blankTargetHostFallsBackToDomain() {
        String[] cmd = SslCertificateMonitor.buildCommand("openssl.sh", site("example.com", 443, "  "));
        Assert.assertArrayEquals(new String[]{"openssl.sh", "example.com", "443", "example.com"}, cmd);
    }

    @Test
    public void ipv4DomainDoesNotSendSni() {
        String[] cmd = SslCertificateMonitor.buildCommand("openssl.sh", site("10.1.2.3", 443, null));
        Assert.assertArrayEquals(new String[]{"openssl.sh", "10.1.2.3", "443"}, cmd);
    }

    @Test
    public void ipv6DomainDoesNotSendSni() {
        String[] cmd = SslCertificateMonitor.buildCommand("openssl.sh", site("2001:db8::1", 443, null));
        Assert.assertArrayEquals(new String[]{"openssl.sh", "2001:db8::1", "443"}, cmd);
    }

    @Test
    public void ipDomainWithTargetHostConnectsToTargetWithoutSni() {
        String[] cmd = SslCertificateMonitor.buildCommand("openssl.sh", site("10.1.2.3", 443, "10.0.0.5"));
        Assert.assertArrayEquals(new String[]{"openssl.sh", "10.0.0.5", "443"}, cmd);
    }
}
