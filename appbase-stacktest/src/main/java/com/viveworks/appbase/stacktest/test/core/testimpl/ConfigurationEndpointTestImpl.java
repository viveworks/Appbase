/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://example.invalid/scratch-rehearsal).
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
package com.viveworks.appbase.stacktest.test.core.testimpl;

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListener;
import com.viveworks.appbase.core.service.configuration.property.deserializer.PropertyDeserializer;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationsJson;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.SetOfStringsValue;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.dummytypes.DummyAppbaseServiceConfiguration1;
import com.viveworks.appbase.stacktest.dummytypes.DummyAppbaseServiceConfiguration2;
import com.viveworks.appbase.stacktest.dummytypes.DummyConfigurationListener;
import com.viveworks.appbase.stacktest.dummytypes.DummyPropertyDeserializer;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.TestUtil;
import com.viveworks.appbase.stacktest.test.core.jaxrs.ConfigurationJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.core.servlet.ConfigurationServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ConfigurationEndpointTestImpl extends AbstractTest {
    private final Class<?> testType;

    private final String resourceUrl;

    private final String servletUrl;

    public ConfigurationEndpointTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        checkTestType(testType, ConfigurationJaxrsResourceTest.class, ConfigurationServletTest.class);

        this.testType = testType;
        this.resourceUrl = baseUrl + AppbaseServiceConstants.DEFAULT_CONFIGURATION_RESTAPI_PATH;
        this.servletUrl = baseUrl + AppbaseServiceConstants.DEFAULT_CONFIGURATION_SERVLET_PATH;
    }


    // ..............................
    // GET tests .....
    // ..............................

    @SuppressWarnings("unchecked")
    public void getPropertyTestImpl() throws Exception {
        final String testName = "getPropertyTest";
        startTest(testType, testName);

        final String randomKey = TestUtil.randomKey();
        final String randomValue = TestUtil.randomValue();
        configurationService().putString(randomKey, randomValue);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/properties/" + randomKey;
        } else {
            url = servletUrl + "?operation=property&propertyKey=" + randomKey;
        }

        try {
            final ConfigurationValue<String> configurationValue = httpGetJson(url, ConfigurationValue.class);
            checkNotNull(configurationValue, testName, "configurationValue");

            final String value = configurationValue.getValue();
            checkNotBlank(value, testName, "value");

            checkEquals(randomValue, value, testName, "value");
        } finally {
            configurationService().removeString(randomKey);
        }

        endTest(testType, testName);
    }

    public void getTypedConfigurationYamlTestImpl() throws Exception {
        final String testName = "getTypedConfigurationYamlTest";
        startTest(testType, testName);

        final DummyAppbaseServiceConfiguration2 dummyAppbaseServiceConfiguration2 =
                new DummyAppbaseServiceConfiguration2("dc2", "host2", 924);
        configurationService().putTypedConfiguration(
                DummyAppbaseServiceConfiguration2.class, dummyAppbaseServiceConfiguration2);
        checkTrue(configurationService().containsTypedConfiguration(
                DummyAppbaseServiceConfiguration2.class), testName, "dummyAppbaseServiceConfiguration2");

        final String url;
        if (isRestApiTest()) {
            url = servletUrl + "?operation=typedConfiguration&typeName=" + DummyAppbaseServiceConfiguration2.class.getName();
        } else {
            url = resourceUrl + "/typedConfigurations/" + DummyAppbaseServiceConfiguration2.class.getName();
        }

        try {
            final String yaml = httpGetText(url, String.class);
            final DummyAppbaseServiceConfiguration2 actual = YamlUtil
                    .getYamlMapper()
                    .readerFor(DummyAppbaseServiceConfiguration2.class)
                    .readValue(yaml);
            checkEquals(dummyAppbaseServiceConfiguration2, actual, testName, "dummyAppbaseServiceConfiguration2");
        } finally {
            configurationService().removeTypedConfiguration(DummyAppbaseServiceConfiguration2.class);
        }

        endTest(testType, testName);
    }

    public void getNonValidatingTypesTestImpl() throws Exception {
        final String testName = "getNonValidatingTypesTest";
        startTest(testType, testName);

        configurationService().registerNonValidatingType(DummyAppbaseServiceConfiguration1.class);
        checkEquals(1, configurationService().getNonValidatingTypeNames().size(), testName, "nonValidatingTypeNames");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/nonValidatingTypes";
        } else {
            url = servletUrl + "?operation=nonValidatingTypes";
        }

        try {
            final SetOfStringsValue setOfStringsValue = httpGetJson(url, SetOfStringsValue.class);
            checkNotNull(setOfStringsValue, testName, "setOfStringsValue");

            final Set<String> stringSet = setOfStringsValue.getValue();
            checkNotNull(stringSet, testName, "stringSet");

            checkTrue(
                    stringSet.contains(DummyAppbaseServiceConfiguration1.class.getName()),
                    testName,
                    "containsSampleAppbaseServiceConfiguration1");
        } finally {
            configurationService().unregisterNonValidatingType(DummyAppbaseServiceConfiguration1.class);
        }

        endTest(testType, testName);
    }

    public void getSecurePropertyKeysTestImpl() throws Exception {
        final String testName = "getSecurePropertyKeysTest";
        startTest(testType, testName);

        final String property = "property1";
        configurationService().registerSecurePropertyKey(property);
        checkTrue(
                configurationService().getSecurePropertyKeys().contains(property),
                testName,
                "containsSecurePropertyKeys");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/securePropertyKeys";
        } else {
            url = servletUrl + "?operation=securePropertyKeys";
        }

        try {
            final SetOfStringsValue setOfStringsValue = httpGetJson(url, SetOfStringsValue.class);
            checkNotNull(setOfStringsValue, testName, "setOfStringsValue");

            final Set<String> stringSet = setOfStringsValue.getValue();
            checkNotNull(stringSet, testName, "stringSet");

            checkTrue(stringSet.contains(property), testName, "containsProperty1");
        } finally {
            configurationService().unregisterSecurePropertyKey(property);
        }

        endTest(testType, testName);
    }

    public void getSecureTypesTestImpl() throws Exception {
        final String testName = "getSecureTypesTest";
        startTest(testType, testName);

        configurationService().registerSecureType(DummyAppbaseServiceConfiguration1.class);
        checkTrue(
                configurationService().getSecureTypeNames().contains(DummyAppbaseServiceConfiguration1.class.getName()),
                testName,
                "containsSecureTypeNames");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/secureTypes";
        } else {
            url = servletUrl + "?operation=secureTypes";
        }

        try {
            final SetOfStringsValue setOfStringsValue = httpGetJson(url, SetOfStringsValue.class);
            checkNotNull(setOfStringsValue, testName, "setOfStringsValue");

            final Set<String> stringSet = setOfStringsValue.getValue();
            checkNotNull(stringSet, testName, "stringSet");

            checkTrue(
                    stringSet.contains(DummyAppbaseServiceConfiguration1.class.getName()),
                    testName,
                    "containsProperty1");
        } finally {
            configurationService().unregisterSecureType(DummyAppbaseServiceConfiguration1.class);
        }

        endTest(testType, testName);
    }

    public void getConfigurationsAsJsonTestImpl() throws Exception {
        final String testName = "getConfigurationsAsJsonTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/configurations";
        } else {
            url = servletUrl + "?operation=configurationsAsJson";
        }

        final ConfigurationsJson configurationsJson = configurationService().getConfigurationsJson();
        final ConfigurationsJson configurationsJson1 = httpGetJson(url, ConfigurationsJson.class);
        checkNotNull(configurationsJson1, testName, "configurationsJson1");
        checkEquals(configurationsJson, configurationsJson1, testName, "configurationsJson1");

        endTest(testType, testName);
    }


    // ..............................
    // POST tests .....
    // ..............................

    public void setPropertyTestImpl() throws Exception {
        final String testName = "setPropertyTest";
        startTest(testType, testName);

        final String randomKey = TestUtil.randomKey();
        final String randomValue = TestUtil.randomValue();
        final boolean property = configurationService().containsProperty(randomKey);
        checkFalse(property, testName, "randomKey");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/properties/" + randomKey + "/" + randomValue;
        } else {
            url = servletUrl + "?operation=property&propertyKey=" + randomKey + "&propertyValue=" + randomValue;
        }

        try {
            httpPostEmpty(url);

            final String actualRandomValue = configurationService().getString(randomKey);
            checkNotBlank(actualRandomValue, testName, "actualRandomValue");
            checkEquals(randomValue, actualRandomValue, testName, "actualRandomValue");
        } finally {
            configurationService().removeString(randomKey);
        }
        endTest(testType, testName);
    }

    public void setTypedConfigurationYamlTestImpl() throws Exception {
        final String testName = "setTypedConfigurationYamlTest";
        startTest(testType, testName);

        final DummyAppbaseServiceConfiguration2 dummyAppbaseServiceConfiguration2 =
                new DummyAppbaseServiceConfiguration2("dc1", "host1", 2409);
        checkFalse(configurationService().containsTypedConfiguration(
                DummyAppbaseServiceConfiguration2.class), testName, "dummyAppbaseServiceConfiguration2");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/typedConfigurations/" + DummyAppbaseServiceConfiguration2.class.getName();
        } else {
            url = servletUrl + "?operation=typedConfiguration&typeName=" +
                    DummyAppbaseServiceConfiguration2.class.getName();
        }

        try {
            final String yaml = YamlUtil
                    .getYamlMapper()
                    .writerFor(DummyAppbaseServiceConfiguration2.class)
                    .writeValueAsString(dummyAppbaseServiceConfiguration2);
            httpPostText(url, yaml, null);

            checkTrue(configurationService().containsTypedConfiguration(
                    DummyAppbaseServiceConfiguration2.class), testName, "dummyAppbaseServiceConfiguration2");
        } finally {
            configurationService().removeTypedConfiguration(DummyAppbaseServiceConfiguration2.class);
        }

        endTest(testType, testName);
    }

    public void loadTestImpl() throws Exception {
        final String testName = "loadTest";
        startTest(testType, testName);

        final Map<String, ConfigurationValue<?>> typeNameMap =
                configurationService().getLoadedConfigurationValueByTypeNameMap();
        final Map<String, ConfigurationValue<?>> propertyNameMap =
                configurationService().getLoadedConfigurationValueByPropertyNameMap();

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/load";
        } else {
            url = servletUrl + "?operation=load";
        }

        httpPostEmpty(url);

        final Map<String, ConfigurationValue<?>> typeNameMap1 =
                configurationService().getLoadedConfigurationValueByTypeNameMap();
        final Map<String, ConfigurationValue<?>> propertyNameMap1 =
                configurationService().getLoadedConfigurationValueByPropertyNameMap();

        checkEquals(typeNameMap, typeNameMap1, testName, "typeNameMap1");
        checkEquals(propertyNameMap, propertyNameMap1, testName, "propertyNameMap1");

        endTest(testType, testName);
    }


    // ..............................
    // HEAD tests .....
    // ..............................

    public void containsPropertyTestImpl() throws Exception {
        final String testName = "containsPropertyTest";
        startTest(testType, testName);

        final String randomKey = TestUtil.randomKey();
        final String randomValue = TestUtil.randomValue();

        final boolean property = configurationService().containsProperty(randomKey);
        checkFalse(property, testName, "randomKey");

        configurationService().putString(randomKey, randomValue);
        checkTrue(configurationService().containsProperty(randomKey), testName, "randomKey");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/properties/" + randomKey;
        } else {
            url = servletUrl + "?operation=property&propertyKey=" + randomKey;
        }

        try {
            final int statusCode = httpHead(url);
            checkEquals(HttpServletResponse.SC_OK, statusCode, testName, "randomKey");
        } finally {
            configurationService().removeString(randomKey);
        }

        endTest(testType, testName);
    }

    public void containsTypedConfigurationTestImpl() throws Exception {
        final String testName = "containsTypedConfigurationTest";
        startTest(testType, testName);

        final DummyAppbaseServiceConfiguration2 dummyAppbaseServiceConfiguration2 =
                new DummyAppbaseServiceConfiguration2("dc1", "host1", 2409);
        configurationService().putTypedConfiguration(
                DummyAppbaseServiceConfiguration2.class, dummyAppbaseServiceConfiguration2);
        checkTrue(configurationService().containsTypedConfiguration(
                DummyAppbaseServiceConfiguration2.class), testName, "dummyAppbaseServiceConfiguration2");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/typedConfigurations/" +
                    DummyAppbaseServiceConfiguration2.class.getName();
        } else {
            url = servletUrl + "?operation=typedConfiguration&typeName=" +
                    DummyAppbaseServiceConfiguration2.class.getName();
        }

        try {
            final int statusCode1 = httpHead(url);
            checkEquals(HttpServletResponse.SC_OK, statusCode1, testName, "dummyAppbaseServiceConfiguration2");

            configurationService().removeTypedConfiguration(DummyAppbaseServiceConfiguration2.class);
            checkFalse(configurationService().containsTypedConfiguration(
                    DummyAppbaseServiceConfiguration2.class), testName, "dummyAppbaseServiceConfiguration2");

            final int statusCode2 = httpHead(url);
            checkEquals(
                    HttpServletResponse.SC_NOT_FOUND,
                    statusCode2,
                    testName,
                    "dummyAppbaseServiceConfiguration2");
        } finally {
            configurationService().removeTypedConfiguration(DummyAppbaseServiceConfiguration2.class);
        }

        endTest(testType, testName);
    }


    // ..............................
    // DELETE tests .....
    // ..............................

    public void removePropertyTestImpl() throws Exception {
        final String testName = "removePropertyTest";
        startTest(testType, testName);

        final String randomKey = TestUtil.randomKey();
        final String randomValue = TestUtil.randomValue();
        configurationService().putString(randomKey, randomValue);
        final boolean property = configurationService().containsProperty(randomKey);
        checkTrue(property, testName, "randomKey");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/properties/" + randomKey;
        } else {
            url = servletUrl + "?operation=property&propertyKey=" + randomKey;
        }

        try {
            httpDelete(url);
            checkFalse(configurationService().containsProperty(randomKey), testName, "randomKey");
        } finally {
            configurationService().removeString(randomKey);
        }

        endTest(testType, testName);
    }

    public void removeTypedConfigurationYamlTestImpl() throws Exception {
        final String testName = "removeTypedConfigurationYamlTest";
        startTest(testType, testName);

        final DummyAppbaseServiceConfiguration2 dummyAppbaseServiceConfiguration2 =
                new DummyAppbaseServiceConfiguration2("dc1", "host1", 2409);
        configurationService().putTypedConfiguration(
                DummyAppbaseServiceConfiguration2.class, dummyAppbaseServiceConfiguration2);
        checkTrue(configurationService().containsTypedConfiguration(
                DummyAppbaseServiceConfiguration2.class), testName, "dummyAppbaseServiceConfiguration2");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/typedConfigurations/" +
                    DummyAppbaseServiceConfiguration2.class.getName();
        } else {
            url = servletUrl + "?operation=typedConfiguration&typeName=" +
                    DummyAppbaseServiceConfiguration2.class.getName();
        }

        try {
            httpDelete(url);
            checkFalse(configurationService().containsTypedConfiguration(
                    DummyAppbaseServiceConfiguration2.class), testName, "dummyAppbaseServiceConfiguration2");
        } finally {
            configurationService().removeTypedConfiguration(DummyAppbaseServiceConfiguration2.class);
        }

        endTest(testType, testName);
    }

    public void unregisterPropertyDeserializerAndConfigurationTypesByPropertyKeyTestImpl() throws Exception {
        final String testName = "unregisterPropertyDeserializerAndConfigurationTypesByPropertyKeyTest";
        startTest(testType, testName);

        final String property = "property1";
        final ConfigurationService.PropertyDeserializerAndConfigurationType propertyDeserializerAndConfigurationType =
                new ConfigurationService.PropertyDeserializerAndConfigurationType(
                        DummyPropertyDeserializer.class,
                        DummyAppbaseServiceConfiguration1.class);
        configurationService().registerPropertyDeserializer(new DummyPropertyDeserializer());
        configurationService().registerPropertyDeserializerAndConfigurationTypesByPropertyKey(
                property, propertyDeserializerAndConfigurationType);

        final ConfigurationService.PropertyDeserializerAndConfigurationType registered1 =
                configurationService().getPropertyDeserializerAndConfigurationTypesByPropertyKeyMap().get(property);
        checkNotNull(registered1, testName, "registeredPropertyDeserializerAndConfigurationType");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/propertyDeserializers/configurationTypes/" + property;
        } else {
            url = servletUrl +
                    "?operation=unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey&propertyKey=" +
                    property;
        }

        try {
            httpDelete(url);
            final ConfigurationService.PropertyDeserializerAndConfigurationType registered2 =
                    configurationService().getPropertyDeserializerAndConfigurationTypesByPropertyKeyMap().get(property);
            checkNull(registered2, testName, "registeredPropertyDeserializerAndConfigurationType");
        } finally {
            configurationService().unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(property);
        }

        endTest(testType, testName);
    }

    public void unregisterPropertyDeserializerTestImpl() throws Exception {
        final String testName = "unregisterPropertyDeserializerTest";
        startTest(testType, testName);

        final DummyPropertyDeserializer dummyPropertyDeserializer = new DummyPropertyDeserializer();
        configurationService().registerPropertyDeserializer(dummyPropertyDeserializer);
        final Collection<PropertyDeserializer> propertyDeserializers = configurationService().getPropertyDeserializers();
        checkTrue(
                propertyDeserializers.contains(dummyPropertyDeserializer),
                testName,
                "containsSamplePropertyDeserializer");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/propertyDeserializers/" + DummyPropertyDeserializer.class.getName();
        } else {
            url = servletUrl + "?operation=unregisterPropertyDeserializer&typeName=" +
                    DummyPropertyDeserializer.class.getName();
        }

        try {
            httpDelete(url);
            final Collection<PropertyDeserializer> propertyDeserializers1 = configurationService().getPropertyDeserializers();
            checkFalse(
                    propertyDeserializers1.contains(dummyPropertyDeserializer),
                    testName,
                    "containsSamplePropertyDeserializer");
        } finally {
            configurationService().unregisterPropertyDeserializer(DummyPropertyDeserializer.class);
        }

        endTest(testType, testName);
    }

    public void unregisterNonValidatingTypeTestImpl() throws Exception {
        final String testName = "unregisterNonValidatingTypeTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/nonValidatingTypes/" + DummyAppbaseServiceConfiguration1.class.getName();
        } else {
            url = servletUrl + "?operation=unregisterNonValidatingType&typeName=" +
                    DummyAppbaseServiceConfiguration1.class.getName();
        }

        configurationService().registerNonValidatingType(DummyAppbaseServiceConfiguration1.class);
        checkEquals(1, configurationService().getNonValidatingTypeNames().size(), testName, "nonValidatingTypeNames");

        try {
            httpDelete(url);
            checkEquals(0, configurationService().getNonValidatingTypeNames().size(), testName, "nonValidatingTypeNames");
        } finally {
            configurationService().unregisterNonValidatingType(DummyAppbaseServiceConfiguration1.class);
        }

        endTest(testType, testName);
    }

    public void unregisterSecurePropertyKeyTestImpl() throws Exception {
        final String testName = "unregisterSecurePropertyKeyTest";
        startTest(testType, testName);

        final String property = "property1";
        configurationService().registerSecurePropertyKey(property);
        checkTrue(
                configurationService().getSecurePropertyKeys().contains(property),
                testName,
                "containsSecurePropertyKeys");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/securePropertyKeys/" + property;
        } else {
            url = servletUrl + "?operation=unregisterSecurePropertyKey&propertyKey=" + property;
        }

        try {
            httpDelete(url);
            checkFalse(
                    configurationService().getSecurePropertyKeys().contains(property),
                    testName,
                    "containsSecurePropertyKeys");
        } finally {
            configurationService().unregisterSecurePropertyKey(property);
        }

        endTest(testType, testName);
    }

    public void unregisterSecureTypeTestImpl() throws Exception {
        final String testName = "unregisterSecureTypeTest";
        startTest(testType, testName);

        configurationService().registerSecureType(DummyAppbaseServiceConfiguration1.class);
        checkTrue(
                configurationService().getSecureTypeNames().contains(DummyAppbaseServiceConfiguration1.class.getName()),
                testName,
                "containsSecureTypeNames");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/secureTypes/" + DummyAppbaseServiceConfiguration1.class.getName();
        } else {
            url = servletUrl +
                    "?operation=unregisterSecureType&typeName=" + DummyAppbaseServiceConfiguration1.class.getName();
        }

        try {
            httpDelete(url);
            checkFalse(
                    configurationService().getSecureTypeNames().contains(DummyAppbaseServiceConfiguration1.class.getName()),
                    testName,
                    "containsSecureTypeNames");
        } finally {
            configurationService().unregisterSecureType(DummyAppbaseServiceConfiguration1.class);
        }

        endTest(testType, testName);
    }

    public void unregisterSecureFieldByConfigurationTypeTestImpl() throws Exception {
        final String testName = "unregisterSecureFieldByConfigurationTypeTest";
        startTest(testType, testName);

        final String propertyName = "hostname";
        configurationService().registerSecureFieldByConfigurationType(DummyAppbaseServiceConfiguration1.class, propertyName);
        final boolean containsKey1 = configurationService()
                .getSecureFieldNamesByConfigurationTypeNameMap()
                .containsKey(DummyAppbaseServiceConfiguration1.class.getName());
        checkTrue(containsKey1, testName, "containsKey1");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl +
                    "/secureFields/" + propertyName +
                    "/configurationType/" + DummyAppbaseServiceConfiguration1.class.getName();
        } else {
            url = servletUrl +
                    "?operation=unregisterSecureFieldByConfigurationType" +
                    "&fieldName=" + propertyName +
                    "&typeName=" + DummyAppbaseServiceConfiguration1.class.getName();
        }

        try {
            httpDelete(url);
            final boolean containsKey2 = configurationService()
                    .getSecureFieldNamesByConfigurationTypeNameMap()
                    .containsKey(DummyAppbaseServiceConfiguration1.class.getName());
            checkFalse(containsKey2, testName, "containsKey2");
        } finally {
            configurationService()
                    .unregisterSecureFieldByConfigurationType(DummyAppbaseServiceConfiguration1.class, propertyName);
        }

        endTest(testType, testName);
    }

    public void unregisterGlobalConfigurationListenerTestImpl() throws Exception {
        final String testName = "unregisterGlobalConfigurationListenerTest";
        startTest(testType, testName);

        final DummyConfigurationListener dummyConfigurationListener = new DummyConfigurationListener();
        configurationService().registerGlobalConfigurationListener(dummyConfigurationListener);
        checkTrue(
                configurationService().getGlobalConfigurationListeners().contains(dummyConfigurationListener),
                testName,
                "containsGlobalConfigurationListeners");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl +
                    "/globalConfigurationListeners/" + DummyConfigurationListener.class.getName();
        } else {
            url = servletUrl + "?operation=unregisterGlobalConfigurationListener&typeName=" +
                    DummyConfigurationListener.class.getName();
        }

        try {
            httpDelete(url);
            checkFalse(
                    configurationService().getGlobalConfigurationListeners().contains(dummyConfigurationListener),
                    testName,
                    "containsGlobalConfigurationListeners");
        } finally {
            configurationService().unregisterGlobalConfigurationListener(DummyConfigurationListener.class);
        }

        endTest(testType, testName);
    }

    public void unregisterConfigurationListenerByPropertyKeyTestImpl() throws Exception {
        final String testName = "unregisterConfigurationListenerByPropertyKeyTest";
        startTest(testType, testName);


        final ConfigurationKey configurationKey = ConfigurationKey.fromPropertyKey("property1");
        final DummyConfigurationListener dummyConfigurationListener = new DummyConfigurationListener();
        configurationService().registerConfigurationListenerByKey(dummyConfigurationListener, configurationKey);

        final ConfigurationListener configurationListener = configurationService()
                .getConfigurationListenerByKeyMap()
                .get(configurationKey);
        checkNotNull(configurationListener, testName, "configurationListener");
        checkEquals(dummyConfigurationListener, configurationListener, testName, "configurationListener");

        final String url;
        if (isRestApiTest()) {
            url = servletUrl +
                    "?operation=unregisterConfigurationListenerByPropertyKey&propertyKey=" +
                    configurationKey.getPropertyKey();
        } else {
            url = resourceUrl + "/configurationListeners/propertyKey/" + configurationKey.getPropertyKey();;
        }

        try {
            httpDelete(url);
            final ConfigurationListener configurationListener1 = configurationService()
                    .getConfigurationListenerByKeyMap()
                    .get(configurationKey);
            checkNull(configurationListener1, testName, "configurationListener1");
        } finally {
            configurationService().unregisterConfigurationListenerByKey(configurationKey);
        }

        endTest(testType, testName);
    }

    public void unregisterConfigurationListenerByTypeNameTestImpl() throws Exception {
        final String testName = "unregisterConfigurationListenerByTypeNameTest";
        startTest(testType, testName);

        final ConfigurationKey configurationKey =
                ConfigurationKey.fromTypeKey(DummyAppbaseServiceConfiguration1.class);
        final DummyConfigurationListener dummyConfigurationListener = new DummyConfigurationListener();
        configurationService().registerConfigurationListenerByKey(dummyConfigurationListener, configurationKey);

        final ConfigurationListener configurationListener = configurationService()
                .getConfigurationListenerByKeyMap()
                .get(configurationKey);
        checkNotNull(configurationListener, testName, "configurationListener");
        checkEquals(dummyConfigurationListener, configurationListener, testName, "configurationListener");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/configurationListeners/appbaseServiceConfigurationType/" +
                    DummyAppbaseServiceConfiguration1.class.getName();
        } else {
            url = servletUrl + "?operation=unregisterConfigurationListenerByTypeName&typeName=" +
                    DummyAppbaseServiceConfiguration1.class.getName();
        }

        try {
            httpDelete(url);
            final ConfigurationListener configurationListener1 = configurationService()
                    .getConfigurationListenerByKeyMap()
                    .get(configurationKey);
            checkNull(configurationListener1, testName, "configurationListener1");
        } finally {
            configurationService().unregisterConfigurationListenerByKey(configurationKey);
        }

        endTest(testType, testName);
    }

    public void clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMapTestImpl() throws Exception {
        final String testName = "clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMapTest";
        startTest(testType, testName);

        final String property = "property1";
        final ConfigurationService.PropertyDeserializerAndConfigurationType propertyDeserializerAndConfigurationType =
                new ConfigurationService.PropertyDeserializerAndConfigurationType(
                        DummyPropertyDeserializer.class,
                        DummyAppbaseServiceConfiguration1.class);
        configurationService().registerPropertyDeserializer(new DummyPropertyDeserializer());
        configurationService().registerPropertyDeserializerAndConfigurationTypesByPropertyKey(
                property, propertyDeserializerAndConfigurationType);

        final Map<String, ConfigurationService.PropertyDeserializerAndConfigurationType> map1 =
                configurationService().getPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();
        checkNotNull(map1, testName, "registeredPropertyDeserializerAndConfigurationType");
        checkEquals(map1.size(), 1, testName, "map1");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/propertyDeserializers/configurationTypes";
        } else {
            url = servletUrl + "?operation=clearPropertyDeserializerAndConfigurationTypesMap";
        }

        try {
            httpDelete(url);

            final Map<String, ConfigurationService.PropertyDeserializerAndConfigurationType> map2 =
                    configurationService().getPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();
            checkNotNull(map2, testName, "registeredPropertyDeserializerAndConfigurationType");
            checkEquals(map2.size(), 0, testName, "map1");
        } finally {
            configurationService().unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(property);
        }

        endTest(testType, testName);
    }

    public void clearPropertyDeserializersTestImpl() throws Exception {
        final String testName = "clearPropertyDeserializersTest";
        startTest(testType, testName);

        final DummyPropertyDeserializer dummyPropertyDeserializer = new DummyPropertyDeserializer();
        configurationService().registerPropertyDeserializer(dummyPropertyDeserializer);
        final Collection<PropertyDeserializer> propertyDeserializers = configurationService().getPropertyDeserializers();
        checkTrue(
                propertyDeserializers.contains(dummyPropertyDeserializer),
                testName,
                "containsSamplePropertyDeserializer");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/propertyDeserializers";
        } else {
            url = servletUrl + "?operation=clearPropertyDeserializers";
        }

        try {
            httpDelete(url);

            final Collection<PropertyDeserializer> propertyDeserializers1 = configurationService().getPropertyDeserializers();
            checkFalse(
                    propertyDeserializers1.contains(dummyPropertyDeserializer),
                    testName,
                    "containsSamplePropertyDeserializer");
        } finally {
            configurationService().unregisterPropertyDeserializer(DummyPropertyDeserializer.class);
        }

        endTest(testType, testName);
    }

    public void clearNonValidatingTypesTestImpl() throws Exception {
        final String testName = "clearSecurePropertyKeysTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/nonValidatingTypes";
        } else {
            url = servletUrl + "?operation=clearNonValidatingTypes";
        }

        configurationService().registerNonValidatingType(DummyAppbaseServiceConfiguration1.class);
        checkEquals(1, configurationService().getNonValidatingTypeNames().size(), testName, "nonValidatingTypeNames");

        try {
            httpDelete(url);
            checkEquals(0, configurationService().getNonValidatingTypeNames().size(), testName, "nonValidatingTypeNames");
        } finally {
            configurationService().unregisterNonValidatingType(DummyAppbaseServiceConfiguration1.class);
        }

        endTest(testType, testName);
    }

    public void clearSecurePropertyKeysTestImpl() throws Exception {
        final String testName = "clearSecurePropertyKeysTest";
        startTest(testType, testName);

        final String property = "property1";
        configurationService().registerSecurePropertyKey(property);
        checkTrue(
                configurationService().getSecurePropertyKeys().contains(property),
                testName,
                "containsSecurePropertyKeys");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/securePropertyKeys";
        } else {
            url = servletUrl + "?operation=clearSecurePropertyKeys";
        }

        try {
            httpDelete(url);
            checkFalse(
                    configurationService().getSecurePropertyKeys().contains(property),
                    testName,
                    "containsSecurePropertyKeys");
        } finally {
            configurationService().unregisterSecurePropertyKey(property);
        }

        endTest(testType, testName);
    }

    public void clearSecureTypesTestImpl() throws Exception {
        final String testName = "clearSecureFieldByConfigurationTypeMapTest";
        startTest(testType, testName);

        configurationService().registerSecureType(DummyAppbaseServiceConfiguration1.class);
        checkTrue(
                configurationService().getSecureTypeNames().contains(DummyAppbaseServiceConfiguration1.class.getName()),
                testName,
                "containsSecureTypeNames");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/secureTypes";
        } else {
            url = servletUrl + "?operation=clearSecureTypes";
        }

        try {
            httpDelete(url);
            checkFalse(
                    configurationService().getSecureTypeNames().contains(DummyAppbaseServiceConfiguration1.class.getName()),
                    testName,
                    "containsSecureTypeNames");
        } finally {
            configurationService().unregisterSecureType(DummyAppbaseServiceConfiguration1.class);
        }

        endTest(testType, testName);
    }

    public void clearSecureFieldByConfigurationTypeMapTestImpl() throws Exception {
        final String testName = "clearSecureFieldByConfigurationTypeMapTest";
        startTest(testType, testName);

        final String propertyName = "hostname";
        configurationService().registerSecureFieldByConfigurationType(DummyAppbaseServiceConfiguration1.class, propertyName);
        final boolean containsKey1 = configurationService()
                .getSecureFieldNamesByConfigurationTypeNameMap()
                .containsKey(DummyAppbaseServiceConfiguration1.class.getName());
        checkTrue(containsKey1, testName, "containsKey1");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/secureFields/configurationType";
        } else {
            url = servletUrl + "?operation=clearSecureFieldByConfigurationTypeMap";
        }

        try {
            httpDelete(url);
            final boolean containsKey2 = configurationService()
                    .getSecureFieldNamesByConfigurationTypeNameMap()
                    .containsKey(DummyAppbaseServiceConfiguration1.class.getName());
            checkFalse(containsKey2, testName, "containsKey2");
        } finally {
            configurationService()
                    .unregisterSecureFieldByConfigurationType(DummyAppbaseServiceConfiguration1.class, propertyName);
        }

        endTest(testType, testName);
    }

    public void clearGlobalConfigurationListenersTestImpl() throws Exception {
        final String testName = "clearGlobalConfigurationListenersTest";
        startTest(testType, testName);

        final DummyConfigurationListener dummyConfigurationListener = new DummyConfigurationListener();
        configurationService().registerGlobalConfigurationListener(dummyConfigurationListener);
        checkTrue(
                configurationService().getGlobalConfigurationListeners().contains(dummyConfigurationListener),
                testName,
                "containsGlobalConfigurationListeners");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/globalConfigurationListeners";
        } else {
            url = servletUrl + "?operation=clearGlobalConfigurationListeners";
        }

        try {
            httpDelete(url);
            checkFalse(
                    configurationService().getGlobalConfigurationListeners().contains(dummyConfigurationListener),
                    testName,
                    "containsGlobalConfigurationListeners");
        } finally {
            configurationService().unregisterGlobalConfigurationListener(DummyConfigurationListener.class);
        }

        endTest(testType, testName);
    }

    public void clearConfigurationListenersTestImpl() throws Exception {
        final String testName = "clearConfigurationListenersTest";
        startTest(testType, testName);

        final ConfigurationKey configurationKey =
                ConfigurationKey.fromTypeKey(DummyAppbaseServiceConfiguration1.class);
        final DummyConfigurationListener dummyConfigurationListener = new DummyConfigurationListener();
        configurationService().registerConfigurationListenerByKey(dummyConfigurationListener, configurationKey);

        final ConfigurationListener configurationListener = configurationService()
                .getConfigurationListenerByKeyMap()
                .get(configurationKey);
        checkNotNull(configurationListener, testName, "configurationListener");
        checkEquals(dummyConfigurationListener, configurationListener, testName, "configurationListener");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/configurationListeners";
        } else {
            url = servletUrl + "?operation=clearConfigurationListeners";
        }

        try {
            httpDelete(url);
            final Map<ConfigurationKey, ConfigurationListener> configurationListenerByKeyMap =
                    configurationService().getConfigurationListenerByKeyMap();
            checkNotNull(configurationListenerByKeyMap, testName, "configurationListenerByKeyMap");
            checkTrue(configurationListenerByKeyMap.isEmpty(), testName, "configurationListenerByKeyMap.isEmpty");
        } finally {
            configurationService().clearConfigurationListenerByKeyMap();
        }

        endTest(testType, testName);
    }


    // ..............................
    // COMMON tests .....
    // ..............................

    public void statusTestImpl() throws Exception {
        final String testName = "statusTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/status";
        } else {
            url = servletUrl + "?operation=status";
        }

        final String status = configurationService().getStatus().name();
        final String status1 = httpGetText(url, String.class);
        checkEquals(status1, status, testName, "status1");

        endTest(testType, testName);
    }

    public void reloadTestImpl() throws Exception {
        final String testName = "reloadTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/reload";
        } else {
            url = servletUrl + "?operation=reload";
        }

        final String status = configurationService().getStatus().name();
        final int statusCode = httpPutEmpty(url);
        checkEquals(HttpServletResponse.SC_NO_CONTENT, statusCode, testName, "statusCode");

        final String status1 = configurationService().getStatus().name();
        checkEquals(status1, status, testName, "status1");

        endTest(testType, testName);

    }

    public void reloadWithConfigTestImpl() throws Exception {
        final String testName = "reloadWithConfigTest";
        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/reload";
        } else {
            url = servletUrl + "?operation=reload";
        }

        final String status = configurationService().getStatus().name();
        final DefaultConfigurationServiceConfiguration configuration = configurationService().getConfiguration();
        final String json = serialize(DefaultConfigurationServiceConfiguration.class, configuration);
        httpPostJson(url, json, null);

        final String status1 = configurationService().getStatus().name();
        checkEquals(status1, status, testName, "status1");

        endTest(testType, testName);
    }

    public void shutdownTestImpl() throws Exception {
        final String testName = "shutdownTest";
        startTest(testType, testName);

        checkEquals(ServiceStatus.STARTED, configurationService().getStatus(), testName, "serviceStatus");

        final String url;
        if (isRestApiTest()) {
            url = resourceUrl + "/shutdown";
        } else {
            url = servletUrl + "?operation=shutdown";
        }

        httpPostEmpty(url);
        checkEquals(ServiceStatus.SHUTDOWN, configurationService().getStatus(), testName, "serviceStatus1");

        endTest(testType, testName);
    }


    // ..............................
    // Helpers .....
    // ..............................

    private ConfigurationService configurationService() {
        return appbaseServices.getCoreServices().appbaseCore().getConfigurationService();
    }

    private boolean isRestApiTest() {
        return ConfigurationJaxrsResourceTest.class.isAssignableFrom(testType);
    }
}
