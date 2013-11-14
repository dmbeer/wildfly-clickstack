# Wildfly 8 ClickStack

To use: 

```
bees app:deploy -t wildfly8 -a APP_ID pat/to/my/app.war
```


# Build 

```
$  gradlew clean installClickstack distClickstack
```

After a successful build

* an expanded `wildfly8-clickstack`is created under `build/install` and can be used with your local [genapp](http://genapp-docs.cloudbees.com/).
  * **Tip**: Declare a variable `clickstackInstallDir` in `~/.gradle/gradle.properties` (e.g. `/Users/johndoe/genapp/plugins`) to install directly under your genapp plugin dir instead of the `build/install` dir.
* `wildfly8-clickstack-1.2.3.zip` is created under `build/distributions` and can be uploaded to the CloudBees platform location by the CloudBees team.

# Local development

Note: You should be familiar with developing ClickStacks using the genapp system first. [see docs](http://genapp-docs.cloudbees.com/quickstart.html).

* In your metadata.json, you can now reference the stack using the name 'wildfly8'

   ```
   { "app": {  "plugins": ["wildfly8"] } }
   ```
# Testing on RUN@cloud


Once the plugin is published to a public URL, you can update an app to use it with the CloudBees SDK:

   ```
$ bees app:deploy -a APP_ID -t wildfly8 -RPLUGIN.SRC.wildfly7=URL_TO_YOUR_PLUGIN_ZIP path/to/app.war
```

# Key concepts

* `build.gradle` : the gradle build to create the clickstack
* `com.cloudbees.clickstack.wildfly.Setup`: the setup code that instantiate the clickstack


## Clickstack layout

<code><pre>
├── deps
│   ├── control-lib <== USED FOR CONTROL SCRIPTS
│   │   └── cloudbees-jmx-invoker-1.0.2-jar-with-dependencies.jar
│   ├── javaagent-lib <== USED FOR JAVA AGENTS
│   │   ├── cloudbees-clickstack-javaagent-1.2.0.jar
│   │   └── jmxtrans-agent-1.0.6.jar
│   ├── wildfly-lib <== NOT USED FOR THE MOMENT
│   │   └── cloudbees-web-container-extras-1.0.1.jar
│   ├── wildfly-lib-memcache <== NOT USED FOR THE MOMENT
│   │   ├── annotations-1.3.9.jar
│   │   ├── asm-3.2.jar
│   │   ├── commons-codec-1.5.jar
│   │   ├── couchbase-client-1.1.4.jar
│   │   ├── httpcore-4.1.1.jar
│   │   ├── httpcore-nio-4.1.1.jar
│   │   ├── jettison-1.1.jar
│   │   ├── jsr305-1.3.9.jar
│   │   ├── kryo-1.04.jar
│   │   ├── kryo-serializers-0.10.jar
│   │   ├── memcached-session-manager-1.6.5.jar
│   │   ├── memcached-session-manager-tc7-1.6.5.jar
│   │   ├── minlog-1.2.jar
│   │   ├── msm-kryo-serializer-1.6.5.jar
│   │   ├── netty-3.5.5.Final.jar
│   │   ├── reflectasm-1.01.jar
│   │   ├── spymemcached-2.8.12.jar
│   │   └── stax-api-1.0.1.jar
│   ├── wildfly-lib-mysql <== MYSQL DRIVER JAR
│   │   └── mysql-connector-java-5.1.25.jar
│   └── wildfly-lib-postgresql <== POSTGRESQL DRIVER JAR
│       └── postgresql-9.1-901-1.jdbc4.jar
├── dist
├── lib <== JARS USED BY THE CLICKSTACK CODE TO INSTANTIATE THE WILDFLY
│   ├── wildfly-clickstack-8-1.0.2.jar
│   └── ***
├── resources
│   └── jboss-base-dir <== FILE TO COPY UNDER JBOSS BASE DIR
│       └── configuration
│           ├── logging.properties
│           ├── standalone-initial.xml
│           ├── standalone.xml
│           └── wildfly-metrics.xml
├── setup
├── setup.bat
└── wildfly-dist-8.0.0.Beta1.zip
</pre></code>

    
# Deployed Application Layout

<code><pre>
 $app_dir
 ├── .genapp
 │   ├── control
 │   │   ├── config
 │   │   ├── env
 │   │   ├── functions
 │   │   │   └── functions
 │   │   ├── java-opts-05-jboss-log-manager
 │   │   ├── java-opts-10-core
 │   │   ├── java-opts-20-javaagent
 │   │   ├── java-opts-20-wildfly-opts
 │   │   ├── java-opts-60-jmxtrans-agent
 │   │   ├── jmx_invoker
 │   │   ├── print_environment
 │   │   ├── send_sigquit
 │   │   ├── start
 │   │   └── stats-appstat
 │   ├── lib
 │   │   ├── cloudbees-jmx-invoker-1.0.2-jar-with-dependencies.jar
 │   │   └── cloudbees-jmx-invoker-jar-with-dependencies.jar -> .../cloudbees-jmx-invoker-1.0.2-jar-with-dependencies.jar
 │   ├── log
 │   ├── metadata.json
 │   ├── ports
 │   │   └── 8066
 │   ├── reserved
 │   └── setup_status
 │       ├── ok
 │       └── plugin_wildfly8_0
 ├── app-extra-files
 ├── javaagent-lib
 │   ├── cloudbees-clickstack-javaagent-1.2.0.jar
 │   └── jmxtrans-agent-1.0.6.jar
 ├── tmp
 └── wildfly-8.0.0.Beta1
     ├── LICENSE.txt
     ├── README.txt
     ├── appclient
     │   └── configuration
     │       ├── appclient.xml
     │       └── logging.properties
     ├── bin
     │   ├── standalone.sh
     │   └── ***
     ├── copyright.txt
     ├── docs
     │   └── ***
     ├── domain
     │   ├── configuration
     │   │   ├── application-roles.properties
     │   │   ├── application-users.properties
     │   │   ├── default-server-logging.properties
     │   │   ├── domain.xml
     │   │   ├── host-master.xml
     │   │   ├── host-slave.xml
     │   │   ├── host.xml
     │   │   ├── logging.properties
     │   │   ├── mgmt-groups.properties
     │   │   └── mgmt-users.properties
     │   ├── data
     │   │   └── content
     │   └── tmp
     │       └── auth
     ├── jboss-modules.jar
     ├── modules
     │   └── ***
     ├── standalone
     │   ├── configuration
     │   │   ├── application-roles.properties
     │   │   ├── application-users.properties
     │   │   ├── logging.properties
     │   │   ├── mgmt-groups.properties
     │   │   ├── mgmt-users.properties
     │   │   ├── standalone-full-ha.xml
     │   │   ├── standalone-full.xml
     │   │   ├── standalone-ha.xml
     │   │   ├── standalone-initial.xml
     │   │   ├── standalone.xml
     │   │   └── wildfly-metrics.xml
     │   ├── deployments
     │   │   ├── README.txt
     │   │   ├── ROOT.war <== WAR IS DEPLOYED HERE
     │   │   ├── cloudbees-web-container-extras-1.0.1.jar
     │   │   └── mysql-connector-java-5.1.25.jar <== MYSQL DRIVER
     │   ├── lib
     │   │   └── ext
     │   └── tmp
     │       └── auth
     └── welcome-content
         └── ***
</pre></code>