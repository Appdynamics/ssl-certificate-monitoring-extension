ssl-certificate-monitoring-extension
====================================

## Introduction ##

This extension monitors the SSL certificates for configurable domains. This extension should be used with standalone Java Machine Agents.

## Metrics Provided ##

daysToExpiry


## Prerequisites ##

Please make sure that the machine has OpenSSL installed. Windows users can download it from https://www.openssl.org/related/binaries.html
There is a bug in the windows openssl where in the command execution hangs. Please download Cygwin's openssl on Windows.

## Installation ##

1. To build from the source, run "mvn clean install" and find the SslCertificateMonitor.zip file in the "target" folder.
   You can also download the SslCertificateMonitor.zip from [AppDynamics Exchange][].
2. Unzip as "SslCertificateMonitor" and copy the "SslCertificateMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`.

## Configuration ##

###Note
Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a yaml validator http://yamllint.com/

1. Configure the domains by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/SslCertificateMonitor/`.

     ```

        #This will create this metric in all the tiers, under this path
        metricPrefix:  "Custom Metrics|SslCertificate"

        #This will create it in specific Tier. Replace <TIER_ID>
        #metricPrefix: Server|Component:<TIER_ID>|Custom Metrics|SslCertificate

        domains:
          - domain: "www.google.com"
            port: 443
            displayName: "Google"

          - domain: "www.ebay.com"
            port: 443
            displayName: "eBay"

          - domain: "www.amazon.com"
            port: 443
            displayName: "amazon"

        # Point to .sh for unix based and .bat for windows.
        # Incase if you are using Cygwin's openssl, please make sure to change the openssl.bat to point to Cygwin's openssl.
        # For eg. echo | C:\Cygwin64\bin\openssl s_client -connect %1:%2 2> null | C:\Cygwin64\bin\openssl x509 -noout -enddate
        cmdFile: "monitors/SslCertificateMonitor/cmd/openssl.sh"
        #cmdFile: "monitors\\SslCertificateMonitor\\cmd\\openssl.bat"


        #### Configurations below this need not be changed.###
        # number of concurrent tasks
        numberOfThreads: 10

        #timeout for the thread
        threadTimeout: 5
    ```


2. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/SslCertificateMonitor/` directory.
You can also change the frequency at which the MachineAgent calls the extension by changing the <execution-frequency-in-seconds> in monitor.xml. Below is the sample

    ```

         <task-arguments>
            <!-- config file-->
            <argument name="config-file" is-required="true" default-value="monitors/SslCertificateMonitor/config.yml" />
         </task-arguments>

    ```

    On Windows, please specify the absolute path to the config.yml.

3. If needed, configure the openssl command in the cmdFile pointed by config.yml.


## Custom Dashboard ##
![](https://raw.githubusercontent.com/Appdynamics/ssl-certificate-monitoring-extension/master/ssl-certificate.png)

## Troubleshooting ##

1. Verify Machine Agent Data: Please start the Machine Agent without the extension and make sure that it reports data.
   Verify that the machine agent status is UP and it is reporting Hardware Metrics.

2. config.yaml:Validate the file here. http://www.yamllint.com/

3. The config cannot be null :
   This usually happens when on a windows machine in monitor.xml you give config.yaml file path with linux file path separator `/`.
   Use Windows file path separator `\` e.g. `monitors\MQMonitor\config.yaml`. On Windows, please specify absolute file path.

4. Metric Limit: Please start the machine agent with the argument -Dappdynamics.agent.maxMetrics=5000 if there is a metric limit reached
   error in the logs. If you don't see the expected metrics, this could be the cause.

5. Debug Logs:Edit the file, /conf/logging/log4j.xml and update the level of the appender com.appdynamics to debug .
   Let it run for 5-10 minutes and attach the logs to a support ticket

## Contributing ##

Always feel free to fork and contribute any changes directly via [GitHub][].

## Community ##

Find out more in the [AppDynamics Exchange][].

## Support ##

For any questions or feature request, please contact [AppDynamics Center of Excellence][].

**Version:** 3.0.1
**Controller Compatibility:** 3.7+


[Github]: https://github.com/Appdynamics/ssl-certificate-monitoring-extension
[AppDynamics Exchange]: http://community.appdynamics.com/t5/AppDynamics-eXchange/idb-p/extensions
[AppDynamics Center of Excellence]: mailto:help@appdynamics.com
