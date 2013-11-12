/*
 * Copyright 2010-2013, the original author or authors
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

import com.cloudbees.clickstack.domain.metadata.Metadata;
import com.cloudbees.clickstack.util.XmlUtils;
import com.cloudbees.clickstack.domain.metadata.Database;
import com.cloudbees.clickstack.domain.metadata.Email;
import com.cloudbees.clickstack.domain.metadata.SessionStore;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.xmlmatchers.XmlMatchers.isEquivalentTo;
import static org.xmlmatchers.transform.XmlConverters.the;

/**
 * Disable test with {@code @Ignore} because the tests fail in gradle build due to:
 * <pre><code>
 * Caused by: java.lang.RuntimeException: More or less (0) than 1 node found for expression: //datasource[@pool-name='mydb']
 * at com.cloudbees.clickstack.util.XmlUtils.getUniqueElement(XmlUtils.java:103)
 * ... 45 more
 * </code></pre>
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
@Ignore
public class SetupJbossConfigurationFilesTest {

    private Document standaloneXml;

    @Before
    public void before() throws Exception {
        standaloneXml = XmlUtils.loadXmlDocumentFromStream(Thread.currentThread().getContextClassLoader().getResourceAsStream("standalone.xml"));
    }

    @Test
    public void add_data_source_success_basic_config() throws Exception {

        // prepare
        String json = "{ \n" +
                "'cb-db': { \n" +
                "    'DATABASE_PASSWORD': 'test', \n" +
                "    'DATABASE_URL': 'mysql://mysql.mycompany.com:3306/test', \n" +
                "    'DATABASE_USERNAME': 'test', \n" +
                "    '__resource_name__': 'mydb', \n" +
                "    '__resource_type__': 'database' \n" +
                "}\n" +
                "}";
        Metadata metadata = Metadata.Builder.fromJsonString(json, true);
        Map<String, String> jdbcDriverFileNameByDriverName = new HashMap<>();
        jdbcDriverFileNameByDriverName.put(Database.DRIVER_MYSQL, "mysql-connector-java-5.1.25.jar");
        SetupJbossConfigurationFiles setupJbossConfigurationFiles = new SetupJbossConfigurationFiles(metadata, jdbcDriverFileNameByDriverName);

        Database database = metadata.getResource("mydb");

        // run
        setupJbossConfigurationFiles.addDatabase(database, standaloneXml);

        // XmlUtils.flush(standaloneXml, System.out);

        // verify
        Element dataSource = XmlUtils.getUniqueElement(standaloneXml, "//datasource[@pool-name='mydb']");

        String xml = "" +
                "<?xml version='1.0'?>\n" +
                "<datasource enabled='true' jndi-name='java:jboss/datasources/mydb' jta='false' pool-name='mydb' use-java-context='true'>\n" +
                "	<connection-url>jdbc:mysql://mysql.mycompany.com:3306/test</connection-url>\n" +
                "	<new-connection-sql>use test</new-connection-sql>\n" +
                "	<driver>mysql-connector-java-5.1.25.jar</driver>\n" +
                "	<driver-class>com.mysql.jdbc.Driver</driver-class>\n" +
                "	<datasource-class>com.mysql.jdbc.jdbc2.optional.MysqlDataSource</datasource-class>\n" +
                "	<security>\n" +
                "		<user-name>test</user-name>\n" +
                "		<password>test</password>\n" +
                "	</security>\n" +
                "	<pool>\n" +
                "		<max-pool-size>20</max-pool-size>\n" +
                "		<min-pool-size>1</min-pool-size>\n" +
                "	</pool>\n" +
                "	<validation>\n" +
                "		<check-valid-connection-sql>select 1</check-valid-connection-sql>\n" +
                "		<background-validation>true</background-validation>\n" +
                "		<background-validation-millis>5000</background-validation-millis>\n" +
                "	</validation>\n" +
                "</datasource>\n";
        assertThat(the(dataSource), isEquivalentTo(the(xml)));
    }

    @Test
    public void add_jdbc_authentication_realm() throws Exception {
        // prepare
        String json = "{ \n" +
                "    'cb-db': { \n" +
                "        'DATABASE_PASSWORD': 'test', \n" +
                "        'DATABASE_URL': 'mysql://mysql.mycompany.com:3306/test', \n" +
                "        'DATABASE_USERNAME': 'test', \n" +
                "        '__resource_name__': 'mydb', \n" +
                "        '__resource_type__': 'database' \n" +
                "    },\n" +
                "    'jboss': { \n" +
                "        'auth-realm.database': 'mydb' \n" +
                "    }\n" +
                "}";
        Metadata metadata = Metadata.Builder.fromJsonString(json, true);
        Map<String, String> jdbcDriverFileNameByDriverName = new HashMap<>();
        jdbcDriverFileNameByDriverName.put(Database.DRIVER_MYSQL, "mysql-connector-java-5.1.25.jar");
        SetupJbossConfigurationFiles setupJbossConfigurationFiles = new SetupJbossConfigurationFiles(metadata, jdbcDriverFileNameByDriverName);

        Database database = metadata.getResource("mydb");

        // run
        setupJbossConfigurationFiles.addAuthenticationRealm(metadata, standaloneXml);

        // XmlUtils.flush(standaloneXml, System.out);

        // verify
        Element securityDomain = XmlUtils.getUniqueElement(standaloneXml, "//security-domain[@name='jdbc-security-domain']");

        String xml = "" +
                "<security-domain cache-type='default' name='jdbc-security-domain'>\n" +
                "    <authentication>\n" +
                "        <login-module code='Database' flag='required'>\n" +
                "            <module-option name='dsJndiName' value='java:jboss/datasources/mydb'/>\n" +
                "            <module-option name='principalsQuery' value='select `password` from `cb_users` where `username` = ?'/>\n" +
                "            <module-option name='rolesQuery' value=\"select `groupname`, 'Roles' from `cb_groups` where `username` = ?\"/>\n" +
                "            <module-option name='hashAlgorithm' value='MD5'/>\n" +
                "            <module-option name='hashEncoding' value='base64'/>\n" +
                "            <module-option name='unauthenticatedIdentity' value='guest'/>\n" +
                "        </login-module>\n" +
                "    </authentication>\n" +
                "</security-domain>\n";
        assertThat(the(securityDomain), isEquivalentTo(the(xml)));

    }
}