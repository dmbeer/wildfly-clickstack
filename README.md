# Wildfly 8 ClickStack

To use: 

    bees app:deploy -t wildfly7 -RPLUGIN.SRC.wildfly7=https://community.ci.cloudbees.com/job/wildfly8-clickstack/lastSuccessfulBuild/artifact/build/distributions/wildfly8-clickstack-1.0.0-SNAPSHOT.zip -a APP_ID WAR_FILE

Please don't `-t wildfly7` as long as `-t wildfly8` has not been setup by CloudBees engineering team.

Wildfly 8 ClickStack for CloudBees PaaS.


# Build 

    $  gradlew clean installClickstack distClickstack

After successful build

* an expanded `wildfly8-clickstack`is created under `build/install` and can be used with your local [genapp](http://genapp-docs.cloudbees.com/).
* `wildfly8-clickstack-1.2.3.zip` is created under `build/distributions` and can be uploaded to the CloudBees platform location by the CloudBees team.

# Local development

Note: You should be familiar with developing ClickStacks using the genapp system first. \[[see docs](http://genapp-docs.cloudbees.com/quickstart.html)\]

* Build the plugin project using make to prepare for use in local app deploys
* In plugins\_home, add a symlink to the `wildfly8-clickstack/build/install/wildfly8-clickstack` dir named 'wildfly8'

   ```
   $ ln -s wildfly8-clickstack/build/install/wildfly8-clickstack PLUGINS\_HOME/wildfly8
   ```

* In your metadata.json, you can now reference the stack using the name 'wildfly8'

   ```
   { "app": {  "plugins": ["wildfly8"] } }
   ```

Once the plugin is published to a public URL, you can update an app to use it with the CloudBees SDK:

   ```
$ bees app:deploy -a APP_ID -t wildfly7 -RPLUGIN.SRC.wildfly7=URL_TO_YOUR_PLUGIN_ZIP PATH_TO_WARFILE
```

# Key concepts


* `build.gradle` : the gradle build to create the clickstack
* `com.cloudbees.clickstack.wildfly.Setup`: the setup code that instantiate the clickstack


## Clickstack layout

<code><pre>
└── wildfly8-clickstack
    ├── deps <== DEPS TO ADD TO THE DEPLOYED APP
    │   ├── control-lib <== DEPS FOR CONTROL SCRIPTS
    │   │   └── … .jar
    │   ├── javaagent-lib <== DEPS FOR JAVA AGENTS
    │   │   └── ... .jar
    │   ├── wildfly-lib <== DEPS TO UNCONDITIONALLY ADD TO TOMCAT LIB
    │   │   └── ... .jar
    │   ├── wildfly-lib-mail <== DEPS TO ADD IF A MAIL SESSION IS CONFIGURED (SENDGRID)
    │   │   └── ... .jar
    │   ├── wildfly-lib-memcache <== DEPS TO ADD IF MEMCACHE BASED SESSION REPLICATION IS CONFIGURED
    │   │   └── ... .jar
    │   ├── wildfly-lib-mysql <== DEPS TO ADD IF a MYSQL DATABASE IS BOUND TO THE APP
    │   │   └── ... .jar
    │   └── wildfly-lib-postgresql <== DEPS TO ADD IF a POSTGRESQL DATABASE IS BOUND TO THE APP
    │       └── ... .jar
    │
    ├── dist  <== FILES THAT WILL BE COPIED DIRECTLY UNDER APP_DIR
    │   ├── .genapp
    │   │   └── control
    │   │       ├── functions
    │   │       │   └── functions
    │   │       ├── jmx_invoker
    │   │       ├── print_environment
    │   │       ├── send_sigquit
    │   │       ├── start
    │   │       └── stats-appstat
    │   └── wildfly-base
    │       └── conf
    │           ├── context.xml
    │           ├── logging.properties
    │           ├── server.xml
    │           ├── wildfly-metrics.xml
    │           └── web.xml
    │ 
    ├── lib <== JARS USED BY THE SETUP SCRIPT
    │   ├── ...
    │   ├── wildfly8-clickstack-1.0.0-SNAPSHOT.jar
    │   └── ...
    │
    ├── setup
    ├── setup.bat
    └── wildfly-8.0.0-RC1.zip <== TOMCAT PACKAGE TO DEPLOY
</pre></code>

### ClickStack Detailed layout

<code><pre>
└── wildfly8-clickstack
    ├── deps
    │   ├── control-lib
    │   │   └── cloudbees-jmx-invoker-1.0.2-jar-with-dependencies.jar
    │   ├── javaagent-lib
    │   │   ├── cloudbees-clickstack-javaagent-1.2.0.jar
    │   │   ├── jmxtrans-agent-1.0.6.jar
    │   │   └── jsr305-2.0.1.jar
    │   ├── wildfly-lib
    │   │   └── cloudbees-web-container-extras-1.0.1.jar
    │   ├── wildfly-lib-mail
    │   │   ├── activation-1.1.jar
    │   │   └── mail-1.4.7.jar
    │   ├── wildfly-lib-memcache
    │   │   ├── annotations-1.3.9.jar
    │   │   ├── asm-3.2.jar
    │   │   ├── jsr305-1.3.9.jar
    │   │   ├── kryo-1.04.jar
    │   │   ├── kryo-serializers-0.10.jar
    │   │   ├── memcached-session-manager-1.6.4.jar
    │   │   ├── memcached-session-manager-tc7-1.6.4.jar
    │   │   ├── minlog-1.2.jar
    │   │   ├── msm-kryo-serializer-1.6.4.jar
    │   │   ├── reflectasm-1.01.jar
    │   │   └── spymemcached-2.8.12.jar
    │   ├── wildfly-lib-mysql
    │   │   └── mysql-connector-java-5.1.25.jar
    │   └── wildfly-lib-postgresql
    │       └── postgresql-9.1-901-1.jdbc4.jar
    ├── dist
    │   ├── .genapp
    │   │   └── control
    │   │       ├── functions
    │   │       │   └── functions
    │   │       ├── jmx_invoker
    │   │       ├── print_environment
    │   │       ├── send_sigquit
    │   │       ├── start
    │   │       └── stats-appstat
    │   └── wildfly-base
    │       └── conf
    │           ├── context.xml
    │           ├── logging.properties
    │           ├── server.xml
    │           ├── wildfly-metrics.xml
    │           └── web.xml
    ├── lib
    │   ├── Saxon-HE-9.4.jar
    │   ├── clickstack-framework-1.0.0.jar
    │   ├── dom4j-1.6.1.jar
    │   ├── guava-14.0.1.jar
    │   ├── hamcrest-core-1.3.jar
    │   ├── jackson-annotations-2.1.2.jar
    │   ├── jackson-core-2.1.3.jar
    │   ├── jackson-databind-2.1.3.jar
    │   ├── jdom-1.1.jar
    │   ├── jsr305-2.0.1.jar
    │   ├── slf4j-api-1.7.5.jar
    │   ├── slf4j-simple-1.7.5.jar
    │   ├── wildfly8-clickstack-1.0.0-SNAPSHOT.jar
    │   ├── xalan-2.7.0.jar
    │   ├── xercesImpl-2.8.0.jar
    │   ├── xml-matchers-1.0-RC1.jar
    │   ├── xml-resolver-1.2.jar
    │   ├── xmlunit-1.3.jar
    │   └── xom-1.2.5.jar
    ├── setup
    ├── setup.bat
    └── wildfly-8.0.0-RC1.zip    </pre></code>
    
# Deployed Application Layout

<code><pre>
├── .genapp
│   ├── control
│   │   ├── config
│   │   ├── env
│   │   ├── functions
│   │   │   └── functions
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
│   │   └── cloudbees-jmx-invoker-jar-with-dependencies.jar -> .../.genapp/lib/cloudbees-jmx-invoker-1.0.2-jar-with-dependencies.jar
│   ├── log
│   ├── metadata.json
│   ├── ports
│   │   └── 8604
│   └── setup_status
│       ├── ok
│       └── plugin_wildfly8_clickstack_0
├── apache-wildfly-8.0.0-RC1
│   ├── LICENSE
│   ├── NOTICE
│   ├── RELEASE-NOTES
│   ├── RUNNING.txt
│   ├── bin
│   │   ├── bootstrap.jar
│   │   ├── wildfly-tasks.xml
│   │   ├── wildfly.bat
│   │   ├── wildfly.sh
│   │   ├── commons-daemon-native.tar.gz
│   │   ├── commons-daemon.jar
│   │   ├── configtest.bat
│   │   ├── configtest.sh
│   │   ├── cpappend.bat
│   │   ├── daemon.sh
│   │   ├── digest.bat
│   │   ├── digest.sh
│   │   ├── setclasspath.bat
│   │   ├── setclasspath.sh
│   │   ├── shutdown.bat
│   │   ├── shutdown.sh
│   │   ├── startup.bat
│   │   ├── startup.sh
│   │   ├── wildfly-juli.jar
│   │   ├── wildfly-native.tar.gz
│   │   ├── tool-wrapper.bat
│   │   ├── tool-wrapper.sh
│   │   ├── version.bat
│   │   └── version.sh
│   ├── conf
│   │   ├── wildfly.policy
│   │   ├── wildfly.properties
│   │   ├── context.xml
│   │   ├── logging.properties
│   │   ├── server.xml
│   │   ├── wildfly-users.xml
│   │   └── web.xml
│   ├── lib
│   │   ├── annotations-api.jar
│   │   ├── wildfly-ant.jar
│   │   ├── wildfly-ha.jar
│   │   ├── wildfly-storeconfig.jar
│   │   ├── wildfly-tribes.jar
│   │   ├── wildfly.jar
│   │   ├── ecj-4.2.2.jar
│   │   ├── el-api.jar
│   │   ├── jasper-el.jar
│   │   ├── jasper.jar
│   │   ├── jsp-api.jar
│   │   ├── servlet-api.jar
│   │   ├── wildfly-api.jar
│   │   ├── wildfly-coyote.jar
│   │   ├── wildfly-dbcp.jar
│   │   ├── wildfly-i18n-es.jar
│   │   ├── wildfly-i18n-fr.jar
│   │   ├── wildfly-i18n-ja.jar
│   │   ├── wildfly-jdbc.jar
│   │   ├── wildfly-jni.jar
│   │   ├── wildfly-spdy.jar
│   │   ├── wildfly-util.jar
│   │   ├── wildfly-websocket.jar
│   │   └── websocket-api.jar
│   ├── logs
│   ├── temp
│   │   └── safeToDelete.tmp
│   ├── webapps
│   │   ├── ROOT
│   │   │   └── ...
│   │   ├── docs
│   │   │   └── ...
│   │   ├── examples
│   │   │   └── ...
│   │   ├── host-manager
│   │   │   └── ...
│   │   └── manager
│   │       └── ...
│   └── work
├── app-extra-files
├── wildfly-base
│   ├── conf
│   │   ├── context.xml
│   │   ├── logging.properties
│   │   ├── server.xml
│   │   ├── wildfly-metrics.xml
│   │   └── web.xml
│   ├── lib
│   │   ├── activation-1.1.jar
│   │   ├── cloudbees-web-container-extras-1.0.1.jar
│   │   ├── mail-1.4.7.jar
│   │   └── mysql-connector-java-5.1.25.jar
│   ├── logs
│   ├── webapps
│   │   └── ROOT
│   │       ├── WEB-INF
│   │       │   ├── classes
│   │       │   │   └── *.class
│   │       │   ├── lib
│   │       │   │   └── *.jar
│   │       │   └── web.xml
│   │       └── *.jsp ...
│   └── work
├── javaagent-lib
│   ├── cloudbees-clickstack-javaagent-1.2.0.jar
│   └── jmxtrans-agent-1.0.6.jar
└── tmp</pre></code>