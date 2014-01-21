/*
 * Copyright 2010-2013, CloudBees Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudbees.clickstack.wildfly;

import com.cloudbees.clickstack.domain.environment.Environment;
import com.cloudbees.clickstack.domain.metadata.Database;
import com.cloudbees.clickstack.domain.metadata.Metadata;
import com.cloudbees.clickstack.domain.metadata.SessionStore;
import com.cloudbees.clickstack.plugin.java.JavaPlugin;
import com.cloudbees.clickstack.plugin.java.JavaPluginResult;
import com.cloudbees.clickstack.util.ApplicationUtils;
import com.cloudbees.clickstack.util.CommandLineUtils;
import com.cloudbees.clickstack.util.Files2;
import com.cloudbees.clickstack.util.Manifests;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Setup {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * Root dir of the genapp install
     */
    @Nonnull
    final Path appDir;
    @Nonnull
    final Path genappDir;
    @Nonnull
    final Path controlDir;
    @Nonnull
    final Path clickstackDir;
    @Nonnull
    final Path javaHome;
    @Nonnull
    final Path warOrEarFile;
    @Nonnull
    final Path logDir;
    @Nonnull
    final Path tmpDir;
    @Nonnull
    final Path agentLibDir;
    @Nonnull
    final Path appExtraFilesDir;
    @Nonnull
    final Metadata metadata;
    @Nonnull
    final Environment env;
    /**
     * List of jdbc drivers to declare in {@code standalone.xml}.
     * <p/>
     * Key driver name (e.g. {@link Database#DRIVER_MYSQL}), value driver file name that will become the
     */
    @Nonnull
    private final Map<String, String> jdbcDriverFileNameByDriverName = new HashMap<>();
    /**
     * initialised by {@link #installJBossHome()}
     */
    @Nullable
    Path jbossHome;
    /**
     * initialised by {@link #installJBossHome()}
     */
    @Nullable
    Path jbossBaseDir;


    public Setup(@Nonnull Environment env, @Nonnull Metadata metadata, @Nonnull Path javaHome) throws IOException {
        logger.info("Setup: {}, {}", env, metadata);

        this.env = env;
        this.appDir = env.appDir;

        this.genappDir = env.genappDir;

        this.controlDir = env.controlDir;
        this.logDir = Files.createDirectories(genappDir.resolve("log"));
        Files2.chmodAddReadWrite(logDir);

        this.agentLibDir = Files.createDirectories(appDir.resolve("javaagent-lib"));

        this.tmpDir = Files.createDirectories(appDir.resolve("tmp"));
        Files2.chmodAddReadWrite(tmpDir);

        this.clickstackDir = env.clickstackDir;
        Preconditions.checkState(Files.exists(clickstackDir) && Files.isDirectory(clickstackDir));

        if (Files.exists(env.packageDir.resolve("app.war"))) {
            this.warOrEarFile = env.packageDir.resolve("app.war");
        } else if (Files.exists(env.packageDir.resolve("app.ear"))) {
            this.warOrEarFile = env.packageDir.resolve("app.ear");
        } else {
            throw new IllegalArgumentException("No app.War or app.ear found in " + env.packageDir);
        }
        Preconditions.checkState(!Files.isDirectory(warOrEarFile), "Expected to be a file and not a directory %s", warOrEarFile);

        this.appExtraFilesDir = Files.createDirectories(appDir.resolve("app-extra-files"));
        Files2.chmodAddReadWrite(appExtraFilesDir);

        this.metadata = metadata;

        this.javaHome = Preconditions.checkNotNull(javaHome, "javaHome");
        Preconditions.checkArgument(Files.exists(javaHome), "JavaHome does not exist %s", javaHome);

        logger.debug("warOrEarFile: {}", warOrEarFile.toAbsolutePath());
        logger.debug("agentLibDir: {}", agentLibDir.toAbsolutePath());
        logger.debug("appExtraFilesDir: {}", appExtraFilesDir.toAbsolutePath());
    }

    public static void main(String[] args) throws Exception {
        try {
            Logger initialisationLogger = LoggerFactory.getLogger(Setup.class);

            initialisationLogger.info("Setup clickstack {} - {}, current dir {}",
                    Manifests.getAttribute(Setup.class, "Implementation-Artifact"),
                    Manifests.getAttribute(Setup.class, "Implementation-Date"),
                    FileSystems.getDefault().getPath(".").toAbsolutePath());
            Environment env = CommandLineUtils.argumentsToEnvironment(args);
            Path metadataPath = env.genappDir.resolve("metadata.json");
            Metadata metadata = Metadata.Builder.fromFile(metadataPath);

            JavaPlugin javaPlugin = new JavaPlugin();
            JavaPluginResult javaPluginResult = javaPlugin.setup(metadata, env);

            Setup setup = new Setup(env, metadata, javaPluginResult.getJavaHome());
            setup.setup();
        } catch (Exception e) {
            String hostname;
            try {
                hostname = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e2) {
                hostname = "#hostname#";
            }
            throw new Exception("Exception deploying on " + hostname, e);
        }
    }

    public void setup() throws Exception {
        installJBossHome();
        installSkeleton();
        installJbossBase();
        installApplication();

        installJbossLogManager();
        installCloudBeesJavaAgent();
        installJmxTransAgent();
        writeJavaOpts();
        writeConfig();
        installControlScripts();
        installWildflyJavaOpts();

        SetupJbossConfigurationFiles setupJbossConfigurationFiles = new SetupJbossConfigurationFiles(metadata, jdbcDriverFileNameByDriverName);
        setupJbossConfigurationFiles.buildWildflyConfigurationFiles(jbossBaseDir);
        logger.info("Clickstack successfully installed");
    }

    public void installSkeleton() throws IOException {
        logger.debug("installSkeleton() {}", appDir);

        Files2.copyDirectoryContent(clickstackDir.resolve("dist"), appDir);
    }

    public void installWildflyJavaOpts() throws IOException {
        Path optsFile = controlDir.resolve("java-opts-20-wildfly-opts");
        logger.debug("installWildflyJavaOpts() {}", optsFile);

        String opts = "" +
                "-D[Standalone] " +
                "-Djava.awt.headless=true " +
                "-Djava.io.tmpdir=\"" + tmpDir + "\" " +
                "-Djboss.home.dir=\"" + jbossHome + "\" " +
                "-Djboss.server.base.dir=\"" + jbossBaseDir + "\" " +
                "-Dapp_extra_files=\"" + appExtraFilesDir + "\" " +
                "-Dorg.jboss.boot.log.file=" + jbossBaseDir + "/log/server.log " +
                "-Dlogging.configuration=\"file:" + jbossBaseDir + "/configuration/logging.properties\" " +
                "-Djboss.http.port=" + env.appPort + " " +
                "-Djboss.bind.address=\"0.0.0.0\" ";

        Files.write(optsFile, Collections.singleton(opts), Charsets.UTF_8);
    }

    public void installJBossHome() throws Exception {

        Path wildflyPackagePath = Files2.findArtifact(clickstackDir, "wildfly-dist", "zip");
        Files2.unzip(wildflyPackagePath, appDir);
        jbossHome = Files2.findUniqueDirectoryBeginningWith(appDir, "wildfly");
        logger.debug("jbossHome: {}", jbossHome);

        this.jbossBaseDir = jbossHome.resolve("standalone");
        logger.debug("jbossBaseDir: {}", jbossBaseDir);
        Preconditions.checkArgument(Files.exists(jbossBaseDir), "jbossBaseDir not found %s", jbossBaseDir);

        Files2.chmodReadOnly(jbossHome);
    }

    public void installJbossBase() throws IOException {
        logger.debug("installJbossBase() {}", jbossBaseDir);

        Files2.copyDirectoryContent(clickstackDir.resolve("resources/jboss-base-dir"), jbossBaseDir);

        Path targetLibDir = jbossBaseDir.resolve("deployments");
        Preconditions.checkState(Files.exists(targetLibDir), "deployments dir not found %s", targetLibDir);

        Files2.copyDirectoryContent(clickstackDir.resolve("deps/wildfly-lib"), targetLibDir);

        // JDBC Drivers
        Collection<Database> mysqlDatabases = Collections2.filter(metadata.getResources(Database.class), new Predicate<Database>() {
            @Override
            public boolean apply(@Nullable Database database) {
                return Database.DRIVER_MYSQL.equals(database.getDriver());
            }
        });

        if (!mysqlDatabases.isEmpty()) {
            logger.debug("Add mysql jars");
            Files2.copyDirectoryContent(clickstackDir.resolve("deps/wildfly-lib-mysql"), targetLibDir);
            jdbcDriverFileNameByDriverName.put(
                    Database.DRIVER_MYSQL,
                    Files2.findArtifact(targetLibDir, "mysql-connector-java").toFile().getName());
        }

        Collection<Database> postgresqlDatabases = Collections2.filter(metadata.getResources(Database.class), new Predicate<Database>() {
            @Override
            public boolean apply(@Nullable Database database) {
                return Database.DRIVER_POSTGRES.equals(database.getDriver());
            }
        });
        if (!postgresqlDatabases.isEmpty()) {
            Files2.copyDirectoryContent(clickstackDir.resolve("deps/wildfly-lib-postgresql"), targetLibDir);
        }

        // Memcache
        if (!metadata.getResources(SessionStore.class).isEmpty()) {
            logger.debug("Add memcache jars");
            Files2.copyDirectoryContent(clickstackDir.resolve("deps/wildfly-lib-memcache"), targetLibDir);
        }

        Files2.chmodAddReadWrite(jbossBaseDir);
    }

    public void installApplication() throws IOException {

        Path deploymentsDir = jbossBaseDir.resolve("deployments");
        if (warOrEarFile.endsWith("app.war")) {
            Files.copy(warOrEarFile, deploymentsDir.resolve("ROOT.war"));
        } else {
            Files2.copyToDirectory(warOrEarFile, deploymentsDir);
        }

        Path overwrittenStandaloneXml = Files2.unzipSubFileIfExists(warOrEarFile, "META-INF/standalone.xml", jbossBaseDir.resolve("configuration"));
        if (overwrittenStandaloneXml != null) {
            logger.info("standalone.xml overwritten by application provided version");
        }

        ApplicationUtils.extractApplicationExtraFiles(warOrEarFile, appDir);
        ApplicationUtils.extractContainerExtraLibs(warOrEarFile, deploymentsDir);
    }

    public void installJmxTransAgent() throws IOException {
        logger.debug("installJmxTransAgent() {}", agentLibDir);

        Path jmxtransAgentJarFile = Files2.copyArtifactToDirectory(clickstackDir.resolve("deps/javaagent-lib"), "jmxtrans-agent", agentLibDir);
        Path jmxtransAgentConfigurationFile = jbossBaseDir.resolve("configuration/wildfly-metrics.xml");
        Preconditions.checkState(Files.exists(jmxtransAgentConfigurationFile), "File %s does not exist", jmxtransAgentConfigurationFile);
        Path jmxtransAgentDataFile = logDir.resolve("wildfly-metrics.data");

        Path agentOptsFile = controlDir.resolve("java-opts-60-jmxtrans-agent");

        String agentOptsFileData =
                "-javaagent:" + jmxtransAgentJarFile.toString() + "=" + jmxtransAgentConfigurationFile.toString() +
                        " -Dwildfly_metrics_data_file=" + jmxtransAgentDataFile.toString();

        Files.write(agentOptsFile, Collections.singleton(agentOptsFileData), Charsets.UTF_8);
    }

    /**
     * https://issues.jboss.org/browse/WFLY-895
     * See http://danielpocock.com/monitoring-jboss-tomcat-and-application-servers-with-jmxetric
     *
     * @throws IOException
     */
    public void installJbossLogManager() throws IOException {

        Path optsFile = controlDir.resolve("java-opts-05-jboss-log-manager");
        logger.debug("installJbossLogManager() {}", optsFile);

        Path jbossLogManagerFile = Files2.findArtifact(jbossHome.resolve("modules/system/layers/base/org/jboss/logmanager/main/"), "jboss-logmanager");

        String opts = "" +
                "-Xbootclasspath/p:" + jbossLogManagerFile + " " +
                "-Djava.util.logging.manager=org.jboss.logmanager.LogManager " +
                "-Djboss.modules.system.pkgs=\"org.jboss.logmanager\" ";
        Files.write(optsFile, Collections.singleton(opts), Charsets.UTF_8);
    }

    public void installCloudBeesJavaAgent() throws IOException {
        logger.debug("installCloudBeesJavaAgent() {}", agentLibDir);

        Path cloudbeesJavaAgentJarFile = Files2.copyArtifactToDirectory(clickstackDir.resolve("deps/javaagent-lib"), "cloudbees-clickstack-javaagent", this.agentLibDir);
        Path agentOptsFile = controlDir.resolve("java-opts-20-javaagent");

        Path envFile = controlDir.resolve("env");
        if (!Files.exists(envFile)) {
            logger.error("Env file not found at {}", envFile);
        }
        String agentOptsFileData = "-javaagent:" +
                cloudbeesJavaAgentJarFile +
                "=sys_prop:" + envFile;

        Files.write(agentOptsFile, Collections.singleton(agentOptsFileData), Charsets.UTF_8);
    }

    public void writeJavaOpts() throws IOException {
        Path javaOptsFile = controlDir.resolve("java-opts-10-core");
        logger.debug("writeJavaOpts() {}", javaOptsFile);

        String javaOpts = metadata.getRuntimeParameter("java", "opts", "");
        Files.write(javaOptsFile, Collections.singleton(javaOpts), Charsets.UTF_8);
    }

    public void writeConfig() throws IOException {

        Path configFile = controlDir.resolve("config");
        logger.debug("writeConfig() {}", configFile);

        PrintWriter writer = new PrintWriter(Files.newOutputStream(configFile));

        writer.println("app_dir=\"" + appDir + "\"");
        writer.println("app_tmp=\"" + appDir.resolve("tmp") + "\"");
        writer.println("log_dir=\"" + logDir + "\"");
        writer.println("JBOSS_HOME=\"" + jbossHome + "\"");
        writer.println("JBOSS_MODULEPATH=\"" + jbossHome.resolve("modules") + "\"");
        writer.println("JBOSS_BASE_DIR=\"" + jbossBaseDir + "\"");

        writer.println("port=" + env.appPort);

        writer.println("JAVA_HOME=\"" + javaHome + "\"");
        writer.println("java=\"" + javaHome.resolve("bin/java") + "\"");
        writer.println("genapp_dir=\"" + genappDir + "\"");

        writer.close();
    }

    public void installControlScripts() throws IOException {
        logger.debug("installControlScripts() {}", controlDir);

        Files2.chmodAddReadExecute(controlDir);

        Path genappLibDir = genappDir.resolve("lib");
        Files.createDirectories(genappLibDir);

        Path jmxInvokerPath = Files2.copyArtifactToDirectory(clickstackDir.resolve("deps/control-lib"), "cloudbees-jmx-invoker", genappLibDir);
        // create symlink without version to simplify jmx_invoker script
        Files.createSymbolicLink(genappLibDir.resolve("cloudbees-jmx-invoker-jar-with-dependencies.jar"), jmxInvokerPath);
    }
}
