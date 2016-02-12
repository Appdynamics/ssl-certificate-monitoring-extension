package com.appdynamics.extensions.sslcertificate.config;


import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;

public class ConfigUtilTest {

    ConfigUtil<Configuration> configUtil = new ConfigUtil<Configuration>();

    @Test
    public void loadConfigSuccessfully() throws FileNotFoundException {
        Configuration configuration = configUtil.readConfig(this.getClass().getResource("/conf/config.yml").getFile(),Configuration.class);
        Assert.assertTrue(configuration != null);
        for (Domain d : configuration.getDomains()) {
        	Assert.assertNotNull(d.getDomain());
        	Assert.assertNotEquals(d.getPort(), 0);
        	Assert.assertNotNull(d.getDisplayName());
        }
        
    }
}
