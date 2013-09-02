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
package com.cloudbees.genapp.tomcat;

import com.cloudbees.genapp.CommandLineUtils;
import com.cloudbees.genapp.Files2;
import com.cloudbees.genapp.metadata.Metadata;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class Setup {
    static {
        // configure Logback SimpleLogger
        setSystemPropertyIfNotDefined(SimpleLogger.SHOW_DATE_TIME_KEY, "true");
        setSystemPropertyIfNotDefined(SimpleLogger.DATE_TIME_FORMAT_KEY, "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        setSystemPropertyIfNotDefined(SimpleLogger.LOG_FILE_KEY, "System.out");
        setSystemPropertyIfNotDefined(SimpleLogger.SHOW_LOG_NAME_KEY, "false");
        setSystemPropertyIfNotDefined(SimpleLogger.SHOW_THREAD_NAME_KEY, "false");
    }

    public static final String DEFAULT_JAVA_VERSION = "1.7";
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    final Path appDir;
    Path catalinaHome;
    final Path genappDir;
    final Path controlDir;
    final Path clickstackDir;
    final Path catalinaBase;
    final Path warFile;
    final Path logDir;
    final Path tmpDir;
    final Path agentLibDir;

    /**
     * @param appDir        parent folder of the instantiated app with {@code catalina.home}, {@code catalina.base}, ...
     * @param clickstackDir parent folder of the tomcat-clickstack
     * @param packageDir    parent folder of {@code app.war}
     */
    public Setup(@Nonnull Path appDir, @Nonnull Path clickstackDir, @Nonnull Path packageDir) throws IOException {
        logger.info("clickstackDir: {}", clickstackDir.toAbsolutePath());
        logger.info("appDir: {}", appDir.toAbsolutePath());
        logger.info("packageDir: {}", packageDir.toAbsolutePath());

        this.appDir = appDir;

        this.genappDir = Files.createDirectories(appDir.resolve(".genapp"));

        this.controlDir = Files.createDirectories(genappDir.resolve("control"));
        this.logDir = Files.createDirectories(genappDir.resolve("log"));
        Files2.chmodReadWrite(logDir);

        this.catalinaBase = Files.createDirectories(appDir.resolve("catalina-base"));

        this.agentLibDir = Files.createDirectories(appDir.resolve("javaagent-lib"));


        this.tmpDir = Files.createDirectories(appDir.resolve("tmp"));
        Files2.chmodReadWrite(tmpDir);

        this.clickstackDir = clickstackDir;
        Preconditions.checkState(Files.exists(clickstackDir) && Files.isDirectory(clickstackDir));

        this.warFile = packageDir.resolve("app.war");
        Preconditions.checkState(Files.exists(warFile) && !Files.isDirectory(warFile));

        logger.debug("warFile: {}", warFile.toAbsolutePath());
        logger.debug("tmpDir: {}", tmpDir.toAbsolutePath());
        logger.debug("genappDir: {}", genappDir.toAbsolutePath());
        logger.debug("catalinaBase: {}", catalinaBase.toAbsolutePath());
        logger.debug("agentLibDir: {}", agentLibDir.toAbsolutePath());
    }

    private static void setSystemPropertyIfNotDefined(String systemPropertyName, String value) {
        if (!System.getProperties().contains(systemPropertyName))
            System.setProperty(systemPropertyName, value);
    }

    public static void main(String[] args) throws Exception {
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
        Logger initialisationLogger = LoggerFactory.getLogger(Setup.class);

        FileSystem fs = FileSystems.getDefault();
        initialisationLogger.info("Start setup, current dir {}", fs.getPath(".").toAbsolutePath());

        Path appDir = fs.getPath(CommandLineUtils.getOption("app_dir", args));
        Path clickstackDir;
        try {
            clickstackDir = fs.getPath(CommandLineUtils.getOption("plugin_dir", args));
        } catch (NoSuchElementException e) {
            clickstackDir = fs.getPath(".");
            initialisationLogger.info("'plugin_dir' param not defined, default to '.': {}", clickstackDir.toAbsolutePath());
        }
        Path packageDir = fs.getPath(CommandLineUtils.getOption("pkg_dir", args));
        String appPort = CommandLineUtils.getOption("app_port", args);
        Path genappDir;
        try {
            genappDir = fs.getPath(CommandLineUtils.getOption("genapp_dir", args));
        } catch (NoSuchElementException e) {
            genappDir = packageDir.resolve(".genapp");
            initialisationLogger.info("'genapp_dir' param not defined, infer from 'pkg_dir': {}", genappDir.toAbsolutePath());
        }
        Path metadataPath = genappDir.resolve("metadata.json");

        Metadata metadata = Metadata.Builder.fromFile(metadataPath);


        Setup setup = new Setup(appDir, clickstackDir, packageDir);
        setup.installCatalinaHome();
        setup.installSkeleton();
        Path catalinaBase = setup.installCatalinaBase();
        setup.installCloudBeesJavaAgent();
        setup.installJmxTransAgent();
        setup.writeJavaOpts(metadata);
        setup.writeConfig(metadata, appPort);
        setup.installControlScripts();
        setup.installTomcatJavaOpts();

        ContextXmlBuilder contextXmlBuilder = new ContextXmlBuilder(metadata);
        contextXmlBuilder.buildTomcatConfigurationFiles(catalinaBase);
    }

    public void installSkeleton() throws IOException {
        logger.debug("installSkeleton() {}", appDir);

        Files2.copyDirectoryContent(clickstackDir.resolve("dist"), appDir);
    }

    public void installTomcatJavaOpts() throws IOException {
        Path optsFile = controlDir.resolve("java-opts-20-tomcat-opts");
        logger.debug("installTomcatJavaOpts() {}", optsFile);

        String opts = "" +
                "-Djava.io.tmpdir=\"" + tmpDir + "\" " +
                "-Dcatalina.home=\"" + catalinaHome + "\" " +
                "-Dcatalina.base=\"" + catalinaBase + "\" " +
                "-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager " +
                "-Djava.util.logging.config.file=\"" + catalinaBase + "/conf/logging.properties\"";

        Files.write(optsFile, Collections.singleton(opts), Charsets.UTF_8);
    }

    public void installCatalinaHome() throws Exception {

        Path tomcatPackagePath = Files2.findArtifact(clickstackDir, "tomcat", "zip");
        Files2.unzip(tomcatPackagePath, appDir);
        catalinaHome = Files2.findUniqueFolderBeginningWith(appDir, "apache-tomcat");
        logger.debug("installCatalinaHome() {}", catalinaHome);

        Path targetLibDir = Files.createDirectories(catalinaHome.resolve("lib"));
        Files2.copyArtifactToDirectory(clickstackDir.resolve("deps/tomcat-lib"), "cloudbees-web-container-extras", targetLibDir);

        // JDBC Drivers
        Files2.copyArtifactToDirectory(clickstackDir.resolve("deps/tomcat-lib-mysql"), "mysql-connector-java", targetLibDir);
        Files2.copyArtifactToDirectory(clickstackDir.resolve("deps/tomcat-lib-postgresql"), "postgresql", targetLibDir);

        // Mail
        Files2.copyArtifactToDirectory(clickstackDir.resolve("deps/tomcat-lib-mail"), "mail", targetLibDir);

        // Memcache
        // TODO once memcached-session-manager is available for tomcat

        Files2.chmodReadOnly(catalinaHome);
    }

    public Path installCatalinaBase() throws IOException {
        logger.debug("installCatalinaBase() {}", catalinaBase);

        Path workDir = Files.createDirectories(catalinaBase.resolve("work"));
        Files2.chmodReadWrite(workDir);

        Path logsDir = Files.createDirectories(catalinaBase.resolve("logs"));
        Files2.chmodReadWrite(logsDir);

        Path rootWebAppDir = Files.createDirectories(catalinaBase.resolve("webapps/ROOT"));
        Files2.unzip(warFile, rootWebAppDir);
        Files2.chmodReadWrite(rootWebAppDir);
        return catalinaBase;
    }

    public void installJmxTransAgent() throws IOException {
        logger.debug("installJmxTransAgent() {}", agentLibDir);

        Path jmxtransAgentJarFile = Files2.copyArtifactToDirectory(clickstackDir.resolve("deps/javaagent-lib"), "jmxtrans-agent", agentLibDir);
        Path jmxtransAgentConfigurationFile = catalinaBase.resolve("conf/tomcat-metrics.xml");
        Preconditions.checkState(Files.exists(jmxtransAgentConfigurationFile), "File %s does not exist", jmxtransAgentConfigurationFile);
        Path jmxtransAgentDataFile = logDir.resolve("tomcat-metrics.data");

        Path agentOptsFile = controlDir.resolve("java-opts-60-jmxtrans-agent");

        String agentOptsFileData =
                "-javaagent:" + jmxtransAgentJarFile.toString() + "=" + jmxtransAgentConfigurationFile.toString() +
                        " -Dtomcat_metrics_data_file=" + jmxtransAgentDataFile.toString();

        Files.write(agentOptsFile, Collections.singleton(agentOptsFileData), Charsets.UTF_8);
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

    public void writeJavaOpts(Metadata metadata) throws IOException {
        Path javaOptsFile = controlDir.resolve("java-opts-10-core");
        logger.debug("writeJavaOpts() {}", javaOptsFile);

        String javaOpts = metadata.getRuntimeParameter("java", "opts", "");
        Files.write(javaOptsFile, Collections.singleton(javaOpts), Charsets.UTF_8);
    }

    public void writeConfig(Metadata metadata, String appPort) throws IOException {

        Path configFile = controlDir.resolve("config");
        logger.debug("writeConfig() {}", configFile);

        PrintWriter writer = new PrintWriter(Files.newOutputStream(configFile));

        writer.println("app_dir=\"" + appDir + "\"");
        writer.println("app_tmp=\"" + appDir.resolve("tmp") + "\"");
        writer.println("log_dir=\"" + logDir + "\"");
        writer.println("catalina_home=\"" + catalinaHome + "\"");
        writer.println("catalina_base=\"" + catalinaBase + "\"");

        writer.println("port=" + appPort);

        Path javaPath = findJava(metadata);
        writer.println("java=\"" + javaPath + "\"");
        Path javaHome = findJavaHome(metadata);
        writer.println("JAVA_HOME=\"" + javaHome + "\"");
        writer.println("genapp_dir=\"" + genappDir + "\"");

        writer.println("catalina_opts=\"-Dport.http=" + appPort + "\"");

        String classpath = "" +
                catalinaHome.resolve("bin/bootstrap.jar") + ":" +
                catalinaHome.resolve("bin/tomcat-juli.jar") + ":" +
                catalinaHome.resolve("lib");
        writer.println("java_classpath=\"" + classpath + "\"");

        writer.close();
    }

    public void installControlScripts() throws IOException {
        logger.debug("installControlScripts() {}", controlDir);

        // Files2.copyDirectoryContent(clickstackDir.resolve("dist/scripts"), controlDir);
        Files2.chmodReadExecute(controlDir);

        Path genappLibDir = genappDir.resolve("lib");
        Files.createDirectories(genappLibDir);

        Path jmxInvokerPath = Files2.copyArtifactToDirectory(clickstackDir.resolve("deps/control-lib"), "cloudbees-jmx-invoker", genappLibDir);
        // create symlink without version to simplify jmx_invoker script
        Files.createSymbolicLink(genappLibDir.resolve("cloudbees-jmx-invoker-jar-with-dependencies.jar"), jmxInvokerPath);
    }

    public Path findJava(Metadata metadata) {
        Path javaPath = findJavaHome(metadata).resolve("bin/java");
        Preconditions.checkState(Files.exists(javaPath), "Java executable %s does not exist");
        Preconditions.checkState(!Files.isDirectory(javaPath), "Java executable %s is not a file");

        return javaPath;
    }

    public Path findJavaHome(Metadata metadata) {
        String javaVersion = metadata.getRuntimeParameter("java", "version", DEFAULT_JAVA_VERSION);
        Map<String, String> javaHomePerVersion = new HashMap<>();
        javaHomePerVersion.put("1.6", "/opt/java6");
        javaHomePerVersion.put("1.7", "/opt/java7");
        javaHomePerVersion.put("1.8", "/opt/java8");

        String javaHome = javaHomePerVersion.get(javaVersion);
        if (javaHome == null) {
            javaHome = javaHomePerVersion.get(DEFAULT_JAVA_VERSION);
        }
        Path javaHomePath = FileSystems.getDefault().getPath(javaHome);
        Preconditions.checkState(Files.exists(javaHomePath), "JavaHome %s does not exist");
        Preconditions.checkState(Files.isDirectory(javaHomePath), "JavaHome %s is not a directory");
        return javaHomePath;
    }
}
