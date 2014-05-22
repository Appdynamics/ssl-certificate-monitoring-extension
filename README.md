ssl-certificate-monitoring-extension
====================================

This extension monitors the SSL certificates for configurable domains.

## Metrics Provided ##

DaysToExpiry


## Prerequisites ##

Please make sure that the machine has OpenSSL installed. Windows users can download it from https://www.openssl.org/related/binaries.html


## Installation ##

1. Run "mvn clean install" and find the SslCertificateMonitor.zip file in the "target" folder. You can also download the SslCertificateMonitor.zip from [AppDynamics Exchange][].
2. Unzip as "SslCertificateMonitor" and copy the "SslCertificateMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`.

## Configuration ##

###Note
Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a yaml validator http://yamllint.com/

1. Configure the domains by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/SslCertificateMonitor/`.

    ```
    domains:
      - domain: "www.google.com"
        port: 443
        displayName: "Google"

      - domain: "www.ebay.com"
        port: 443
        displayName: "eBay"


    # Point to .sh for unix based and .bat for windows
    cmdFile: "monitors/SslCertificateMonitor/cmd/openssl.sh"
    metricPrefix:  "Custom Metrics|SslCertificate|"

    # number of concurrent tasks
    numberOfThreads: 10

    #timeout for the thread
    threadTimeout: 30
    ```


2. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/SslCertificateMonitor/` directory.
You can also change the frequency at which the MachineAgent calls the extension by changing the <execution-frequency-in-seconds> in monitor.xml. Below is the sample

    ```
         <task-arguments>
            <!-- config file-->
            <argument name="config-file" is-required="true" default-value="monitors/SslCertificateMonitor/config.yml" />
            <argument name="log-prefix" is-required="false" default-value="[SslCertificateAppDExt] " />
        </task-arguments>
    ```

3. If needed, configure the openssl command in the cmdFile pointed by config.yml.

## Custom Dashboard ##
![](https://raw.githubusercontent.com/Appdynamics/ssl-certificate-monitoring-extension/master/ssl-certificate.png)

## Contributing ##

Always feel free to fork and contribute any changes directly via [GitHub][].

## Community ##

Find out more in the [AppDynamics Exchange][].

## Support ##

For any questions or feature request, please contact [AppDynamics Center of Excellence][].

**Version:** 1.0.0
**Controller Compatibility:** 3.7+


[Github]: https://github.com/Appdynamics/ssl-certificate-monitoring-extension
[AppDynamics Exchange]: http://community.appdynamics.com/t5/AppDynamics-eXchange/idb-p/extensions
[AppDynamics Center of Excellence]: mailto:ace-request@appdynamics.com
