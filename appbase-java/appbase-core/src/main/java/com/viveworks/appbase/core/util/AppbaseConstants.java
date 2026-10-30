/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://github.com/viveworks/Appbase).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.viveworks.appbase.core.util;

import java.io.File;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * This class contains important constants grouped in one place for quick reference.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class AppbaseConstants {
    public static final String CLASSPATH_FILE_PREFIX = "classpath://";

    public static final String APPBASE_ENVIRONMENT_VARIABLE = "APPBASE_ENVIRONMENT";
    public static final String APPBASE_SYSTEM_PROPERTY = "appbase.environment";
    public static final String DEFAULT_ENVIRONMENT = "";


    public static final String APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY = "appbase.configuration.file.path";
    public static final String APPBASE_CONFIGURATION_FILE_PATH_ENV_KEY = "APPBASE_CONFIGURATION_FILE_PATH";
    public static final String DEFAULT_CONFIGURATION_FILE = CLASSPATH_FILE_PREFIX + "/appbase-core.yaml";


    public static final String APPBASE_BASE_DIRECTORY_PROPERTY = "appbase.base.directory";
    public static final String DEFAULT_APPBASE_BASE_DIRECTORY = !AppbaseUtil.isWindows() ?
            File.separator + "opt" + File.separator + "appbase" :
            "C:\\Program Files\\Appbase";
    public static final String APPBASE_BASE_DIRECTORY =
            System.getProperty(
                    APPBASE_BASE_DIRECTORY_PROPERTY, DEFAULT_APPBASE_BASE_DIRECTORY);
    public static final String APPBASE_BOOTSTRAP_CONFIGURATION_NODE_NAME_PROPERTY_KEY =
            "appbase.bootstrap.configuration.node.name";
    public static final String APPBASE_BOOTSTRAP_CONFIGURATION_NODE_NAME_ENV_KEY =
            "APPBASE_BOOTSTRAP_CONFIGURATION_NODE_NAME";


    public static final String CONFIGURATION_SERVICE_CONFIGURATION_YAML_NODE_NAME_PROPERTY_NAME =
            "appbase.configurationServiceConfiguration.yamlNodeName";
    public static final String CONFIGURATION_SERVICE_CONFIGURATION_YAML_NODE_NAME_ENV_NAME =
            "APPBASE_CONFIGURATION_SERVICE_CONFIGURATION_YAML_NODE_NAME";
    public static final String DEFAULT_CONFIGURATION_SERVICE_CONFIGURATION_YAML_NODE_NAME =
            "defaultConfigurationServiceConfiguration";


    public static final String MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME_PROPERTY_NAME =
            "appbase.mediatorServiceConfiguration.yamlNodeName";
    public static final String MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME_ENV_NAME =
            "APPBASE_MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME";
    public static final String DEFAULT_MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME =
            "defaultMediatorServiceConfiguration";

    public static final String PERMISSION_PREFIX_ENV = "APPBASE_";
    public static final String PERMISSION_SUFFIX_ENV = "_PERMISSION_";
    public static final String ALL_OPERATIONS_ENV = "ALL_OPERATIONS";
    public static final String PERMISSION_PREFIX_PROPERTY = "appbase.";
    public static final String PERMISSION_SUFFIX_PROPERTY = ".permission.";
    public static final String ALL_OPERATIONS_PROPERTY = "allOperations";

    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd-HH-mm-ss";
    public static DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT);
    public static ZoneOffset DEFAULT_ZONE_OFFSET = ZoneOffset.UTC;

    public static final String TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX = "appbase.testHelper.configFile.";

    private AppbaseConstants() {
    }
}
