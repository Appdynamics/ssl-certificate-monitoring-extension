ssl-certificate-monitoring-extension
====================================

## Use Case ##

This extension monitors the SSL certificates for configurable domains. This extension should be used with standalone Java Machine Agents.

## Prerequisites ##

1. Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.
2. Please make sure that the machine has OpenSSL installed. Windows users can download it from https://www.openssl.org/related/binaries.html
There is a bug in the windows openssl where in the command execution hangs. Please download Cygwin's openssl on Windows.

## Installation ##

1. To build from the source, run "mvn clean install" and find the SslCertificateMonitor-VERSION.zip file in the "target" folder.
2. Unzip as "SslCertificateMonitor" and copy the "SslCertificateMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`.
3. Edit the file config.yml located at <MachineAgent_Dir>/monitors/SslCertificateMonitor
4. Restart the Machine Agent.

Please place the extension in the "monitors" directory of your Machine Agent installation directory. Do not place the extension in the "extensions" directory of your Machine Agent installation directory.

## Configuration ##

**Note**
Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](https://jsonformatter.org/yaml-validator)

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

## Metrics Provided ##

* daysToExpiry

## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-use-the-Extensions-WorkBench/ta-p/30130).

## Custom Dashboard ##
![](https://raw.githubusercontent.com/Appdynamics/ssl-certificate-monitoring-extension/master/ssl-certificate.png)

## Troubleshooting ##

1. Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension.

2. Verify Machine Agent Data: Please start the Machine Agent without the extension and make sure that it reports data.
   Verify that the machine agent status is UP and it is reporting Hardware Metrics.

3. config.yml: Validate the file [here](https://jsonformatter.org/yaml-validator).

4. The config cannot be null: This usually happens when on a windows machine in monitor.xml you give config.yml file path with linux file path separator `/`. Use Windows file path separator `\` e.g. `monitors\SSLMonitor\config.yml`. On Windows, please specify absolute file path.

5. Metric Limit: Please start the machine agent with the argument -Dappdynamics.agent.maxMetrics=5000 if there is a metric limit reached
   error in the logs. If you don't see the expected metrics, this could be the cause.

## Contributing ##

Always feel free to fork and contribute any changes directly here on [GitHub][].

## Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |3.0.2       |
|Last Update               |14/12/2021  |

**Note**: While extensions are maintained and supported by customers under the open-source licensing model, they interact with agents and Controllers that are subject to [AppDynamics’ maintenance and support policy](https://docs.appdynamics.com/latest/en/product-and-release-announcements/maintenance-support-for-software-versions). Some extensions have been tested with AppDynamics 4.5.13+ artifacts, but you are strongly recommended against using versions that are no longer supported.

[Github]: https://github.com/Appdynamics/ssl-certificate-monitoring-extension
[AppDynamics Exchange]: http://community.appdynamics.com/t5/AppDynamics-eXchange/idb-p/extensions
[AppDynamics Center of Excellence]: mailto:help@appdynamics.com
