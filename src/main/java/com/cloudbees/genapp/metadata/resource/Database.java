package com.cloudbees.genapp.metadata.resource;

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

import java.util.*;

/**
 * This class stores Resources that contain Database credentials.
 */

public class Database extends Resource {

    public static final String DRIVER_POSTGRES = "postgres";
    public static final String DRIVER_MYSQL = "mysql";
    public static final String DRIVER_MS_SQLSERVER = "sql";

    public static final String DRIVER_ORACLE = "oracle";
    public static final String URL_PROPERTY = "DATABASE_URL";
    public static final String USERNAME_PROPERTY = "DATABASE_USERNAME";
    public static final String PASSWORD_PROPERTY = "DATABASE_PASSWORD";
    public static final List<String> TYPES = Arrays.asList("datasource", "database");


    public static String getDriver(String url) {
        if (url.matches("^mysql://.*$"))
            return DRIVER_MYSQL;
        else if (url.matches("^sqlserver://.*$"))
            return DRIVER_MS_SQLSERVER;
        else if (url.matches("^postgresql://.*$"))
            return DRIVER_POSTGRES;
        else if (url.matches("^oracle:.*$"))
            return DRIVER_ORACLE;
        return null;
    }

    public static String getJavaDriver(String driver) {
        if (driver.equals(DRIVER_MYSQL))
            return "com.mysql.jdbc.Driver";
        else if (driver.equals(DRIVER_MS_SQLSERVER))
            return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        else if (driver.equals(DRIVER_POSTGRES))
            return "org.postgresql.Driver";
        else if (driver.equals(DRIVER_ORACLE))
            return "oracle.jdbc.OracleDriver";
        return null;
    }

    public static String getDataSourceClassName(String driver) {
        if (driver.equals(DRIVER_MYSQL))
            return "com.mysql.jdbc.jdbc2.optional.MysqlDataSource";
        else if (driver.equals(DRIVER_MS_SQLSERVER))
            return "com.microsoft.sqlserver.jdbc.SQLServerDataSource";
        else if (driver.equals(DRIVER_POSTGRES))
            return "org.postgresql.ds.PGSimpleDataSource";
        else if (driver.equals(DRIVER_ORACLE))
            return "oracle.jdbc.pool.OracleDataSource";
        return null;
    }

    public static String getValidationQuery(String driver) {
        if (driver.equals(DRIVER_MYSQL))
            return "select 1";
        else if (driver.equals(DRIVER_MS_SQLSERVER))
            return "select 1";
        else if (driver.equals(DRIVER_POSTGRES))
            return "select version();";
        else if (driver.equals(DRIVER_ORACLE))
            return "select 1 from dual";
        else
            return "select 1";
    }

    /**
     * Checks if a given Resource is a database definition.
     * @param resource The Resource to be tested.
     * @return A boolean, true if the Resource given is a database definition.
     */

    protected static boolean checkResource(Resource resource) {
        boolean isValid;
        if (isValid = resource != null) {
            isValid = TYPES.contains(resource.getType());
            isValid = isValid && resource.getProperty(URL_PROPERTY) != null;
            isValid = isValid && resource.getProperty(USERNAME_PROPERTY) != null;
            isValid = isValid && resource.getProperty(PASSWORD_PROPERTY) != null;
            isValid = isValid && getDriver(resource.getProperty(URL_PROPERTY)) != null;
        }
        return isValid;
    }

    public Database (Resource resource) {
        super(resource.getProperties(), resource.getDescriptors());
        if (!checkResource(resource))
            throw new IllegalArgumentException("Incorrect database resource definition.");
    }

    public String getUrl() {
        return getProperty(URL_PROPERTY);
    }

    public String getUsername() {
        return getProperty(USERNAME_PROPERTY);
    }

    public String getPassword() {
        return getProperty(PASSWORD_PROPERTY);
    }

    public String getDriver() {
        return getDriver(getProperty(URL_PROPERTY));
    }

    public String getJavaDriver() {
        return getJavaDriver(getDriver(getProperty(URL_PROPERTY)));
    }

    public String getDataSourceClassName() {
        return getDataSourceClassName(getDriver(getProperty(URL_PROPERTY)));
    }

    public String getValidationQuery() {
        return getValidationQuery(getDriver(getProperty(URL_PROPERTY)));
    }
}