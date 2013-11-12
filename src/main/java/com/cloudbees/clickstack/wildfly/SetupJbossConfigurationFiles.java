package com.cloudbees.clickstack.wildfly;

import com.cloudbees.clickstack.domain.metadata.*;
import com.cloudbees.clickstack.util.XmlUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

public class SetupJbossConfigurationFiles {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    @VisibleForTesting
    protected Map<String, String> jdbcDriverFileNameByDriverName;
    private Metadata metadata;

    /**
     * @param metadata
     * @param jdbcDriverFileNameByDriverName List of jdbc drivers to declare in {@code standalone.xml}.
     *                                       Key driver name (e.g. {@link Database#DRIVER_MYSQL}), value driver file name that will become the
     */
    public SetupJbossConfigurationFiles(Metadata metadata, Map<String, String> jdbcDriverFileNameByDriverName) {
        this.metadata = metadata;
        this.jdbcDriverFileNameByDriverName = jdbcDriverFileNameByDriverName;
    }

    protected SetupJbossConfigurationFiles addDatabase(Database database, Document standaloneXmlDocument) {
        if (!jdbcDriverFileNameByDriverName.containsKey(database.getDriver())) {
            logger.warn("Driver {} not loaded, skip datasource declaration for {}", database.getDriver(), database.getName());
            return this;
        }
        logger.info("Add DataSource name={}, url={}", database.getName(), database.getUrl());

        // see http://www.ironjacamar.org/doc/userguide/1.1/en-US/html/ch05.html

        Element e = standaloneXmlDocument.createElement("datasource");
        e.setAttribute("jndi-name", "java:jboss/datasources/" + database.getName());
        e.setAttribute("pool-name", database.getName());
        e.setAttribute("enabled", "true");
        e.setAttribute("use-java-context", "true");
        e.setAttribute("jta", "false");

        appendChildElement(e, "connection-url", "jdbc:" + database.getUrl());

        // don't declare 'datasource-class' configuration parameter, otherwise, <connection-url> is ignored

        String connectionProperty = database.getProperty("connection-property", "");
        if (!Strings.isNullOrEmpty(connectionProperty)) {
            appendChildElement(e, "connection-property", connectionProperty);
        }

        String jdbcDriverName = jdbcDriverFileNameByDriverName.get(database.getDriver());
        Preconditions.checkState(jdbcDriverName != null, "No JDBC driver declared for %s %s", database.getName(), database.getDriver());
        appendChildElement(e, "driver", jdbcDriverName);

        appendChildElement(e, "driver-class", database.getJavaDriver());


        Element security = standaloneXmlDocument.createElement("security");
        e.appendChild(security);
        appendChildElement(security, "user-name", database.getUsername());
        appendChildElement(security, "password", database.getPassword());


        Element pool = standaloneXmlDocument.createElement("pool");
        e.appendChild(pool);
        // by default max to 20 connections which is the limit of CloudBees MySQL databases
        appendChildElement(pool, "max-pool-size", database.getProperty("max-pool-size", "20"));
        appendChildElement(pool, "min-pool-size", database.getProperty("min-pool-size", "1"));

        Element validation = standaloneXmlDocument.createElement("validation");
        e.appendChild(validation);
        appendChildElement(validation, "check-valid-connection-sql", database.getProperty("check-valid-connection-sql", database.getValidationQuery()));
        appendChildElement(validation, "background-validation", database.getProperty("background-validation", "true"));
        appendChildElement(validation, "background-validation-millis", database.getProperty("background-validation-millis", String.valueOf(5 * 1000)));

        Element datasources = XmlUtils.getUniqueElement(standaloneXmlDocument, "//*[local-name() = 'datasources']");
        datasources.appendChild(e);
        return this;
    }

    private void appendChildElement(Element element, String name, String value) {
        Element userName = element.getOwnerDocument().createElement(name);
        element.appendChild(userName);
        userName.setTextContent(value);
    }

    protected SetupJbossConfigurationFiles addEmail(Email email, Document contextXmlDocument) {
        logger.warn("addEmail disabled");
        return this;
    }

    protected SetupJbossConfigurationFiles addSessionStore(SessionStore store, Document contextXmlDocument) {
        logger.warn("addSessionStore disabled");

        return this;
    }

    public void addAuthenticationRealm(Metadata metadata, Document standaloneXml) throws XPathExpressionException {
        String jdbcRealmBinding = metadata.getRuntimeParameter("jboss", "auth-realm.database", null);
        if (jdbcRealmBinding == null) {
            return;
        }

        String hashAlgorithm = metadata.getRuntimeParameter("jboss", "auth-realm.hashAlgorithm", "MD5");
        String hashEncoding = metadata.getRuntimeParameter("jboss", "auth-realm.hashEncoding", "base64");
        String unauthenticatedIdentity = metadata.getRuntimeParameter("jboss", "auth-realm.unauthenticatedIdentity", "guest");
        String principalsQuery = metadata.getRuntimeParameter("jboss", "auth-realm.principalsQuery", "select `password` from `cb_users` where `username` = ?");
        String rolesQuery = metadata.getRuntimeParameter("jboss", "auth-realm.rolesQuery", "select `groupname`, 'Roles' from `cb_groups` where `username` = ?");
        String securityDomainName = metadata.getRuntimeParameter("jboss", "auth-realm.securityDomainName", "jdbc-security-domain");

        // Verify that the database binding exists
        Resource resource = metadata.getResource(jdbcRealmBinding);
        if (resource == null || !(resource instanceof Database)) {
            throw new RuntimeException("Database binding '" + jdbcRealmBinding + "' declared for RuntimeParameter " +
                    "jboss" + "#" + "auth-realm.database" + " does not exist! Existing Resources " + metadata.getResources().keySet());
        }

        logger.info("Insert security-domain '" + securityDomainName + "' associated to database '" + jdbcRealmBinding + "'");

        Element loginModule = standaloneXml.createElement("login-module");

        loginModule.setAttribute("code", "Database");
        loginModule.setAttribute("flag", "required");
        addModuleOption("dsJndiName", "java:jboss/datasources/" + jdbcRealmBinding, loginModule);
        addModuleOption("principalsQuery", principalsQuery, loginModule);
        addModuleOption("rolesQuery", rolesQuery, loginModule);
        addModuleOption("hashAlgorithm", hashAlgorithm, loginModule);
        addModuleOption("hashEncoding", hashEncoding, loginModule);
        addModuleOption("unauthenticatedIdentity", unauthenticatedIdentity, loginModule);

        Element authentication = standaloneXml.createElement("authentication");
        authentication.appendChild(loginModule);

        Element securityDomain = standaloneXml.createElement("security-domain");
        securityDomain.setAttribute("name", securityDomainName);
        securityDomain.setAttribute("cache-type", "default");
        securityDomain.appendChild(authentication);

        Element securityDomains = XmlUtils.getUniqueElement(standaloneXml, "//*[local-name() = 'security-domains']");
        securityDomains.appendChild(securityDomain);

    }

    protected void addModuleOption(String name, String value, Element parent) {
        Element property = parent.getOwnerDocument().createElement("module-option");
        property.setAttribute("name", name);
        property.setAttribute("value", value);
        parent.appendChild(property);
    }

    protected void buildJbossConfiguration(Metadata metadata, Document standaloneXml) throws Exception {

        String message = "File generated by wildfly-clickstack at " + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date());

        standaloneXml.appendChild(standaloneXml.createComment(message));

        for (Resource resource : metadata.getResources().values()) {
            if (resource instanceof Database) {
                addDatabase((Database) resource, standaloneXml);
            } else if (resource instanceof Email) {
                addEmail((Email) resource, standaloneXml);
            } else if (resource instanceof SessionStore) {
                addSessionStore((SessionStore) resource, standaloneXml);
            }
        }
        addAuthenticationRealm(metadata, standaloneXml);
    }

    public void buildWildflyConfigurationFiles(Path jbossBaseDir) throws Exception {

        Preconditions.checkArgument(Files.exists(jbossBaseDir), "Given jboss.base does not exist %s", jbossBaseDir);
        Preconditions.checkArgument(Files.isDirectory(jbossBaseDir), "Given jboss.base is not a directory %s", jbossBaseDir);


        Path contextXmlPath = jbossBaseDir.resolve("configuration/standalone.xml");
        Preconditions.checkArgument(Files.exists(contextXmlPath), "Given standalone.xml does not exist %s", contextXmlPath);

        Document standaloneXmlDocument = XmlUtils.loadXmlDocumentFromFile(contextXmlPath.toFile());

        this.buildJbossConfiguration(metadata, standaloneXmlDocument);

        StringWriter writer = new StringWriter();
        XmlUtils.flush(standaloneXmlDocument, writer);

        // tweak: remove 'xmlns=""'
        // see https://community.jboss.org/message/778488
        String out = writer.toString().replaceAll("xmlns=\"\"", "");


        FileWriter to = new FileWriter(contextXmlPath.toFile());
        CharStreams.copy(new StringReader(out.toString()), to);
        to.close();
    }
}
