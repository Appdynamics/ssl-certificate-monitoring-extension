/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.sslcertificate;

import com.appdynamics.extensions.sslcertificate.common.SystemUtil;
import org.junit.Assert;
import org.junit.Test;


public class ProcessExecutorTest {

    ProcessExecutor executor = new ProcessExecutor();

    @Test
    public void runScriptWithNoErrors(){
        String scriptFile = this.getClass().getResource("/cmd/success_script.sh").getFile();
        if(SystemUtil.isWindows()){
            scriptFile = this.getClass().getResource("/cmd/success_script.bat").getFile();
        }
        String data = executor.execute(new String[]{scriptFile});
        Assert.assertTrue(!data.isEmpty());
    }


    @Test
    public void runScriptWithErrors(){
        String scriptFile = this.getClass().getResource("/cmd/error_script.sh").getFile();
        if(SystemUtil.isWindows()){
            scriptFile = this.getClass().getResource("/cmd/error_script.bat").getFile();
        }
        String data = executor.execute(new String[]{scriptFile});
        Assert.assertTrue(data.isEmpty());
    }
}
