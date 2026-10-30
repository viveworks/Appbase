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
package com.viveworks.appbase.stacktest.test.core.servlet;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractServletTest;
import com.viveworks.appbase.stacktest.test.core.testimpl.ConfigurationEndpointTestImpl;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ConfigurationServletTest extends AbstractServletTest {
    private final ConfigurationEndpointTestImpl configurationEndpointTestImpl;

    public ConfigurationServletTest(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        this.configurationEndpointTestImpl = new ConfigurationEndpointTestImpl(
                getClass(), appbaseServices, httpClient, cookieStore, baseUrl);
    }

    @Override
    protected void runServletTestsImpl() throws Exception {
        getPropertyTest();
        getTypedConfigurationYamlTest();
        getNonValidatingTypesTest();
        getSecurePropertyKeysTest();
        getSecureTypesTest();
        getConfigurationsAsJsonTest();

        setPropertyTest();
        setTypedConfigurationYamlTest();
        loadTest();

        containsPropertyTest();
        containsTypedConfigurationTest();

        removePropertyTest();
        removeTypedConfigurationYamlTest();
        unregisterPropertyDeserializerAndConfigurationTypesByPropertyKeyTest();
        unregisterPropertyDeserializerTest();
        unregisterNonValidatingTypeTest();
        unregisterSecurePropertyKeyTest();
        unregisterSecureTypeTest();
        unregisterSecureFieldByConfigurationTypeTest();
        unregisterGlobalConfigurationListenerTest();
        unregisterConfigurationListenerByPropertyKeyTest();
        unregisterConfigurationListenerByTypeNameTest();
        clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMapTest();
        clearPropertyDeserializersTest();
        clearNonValidatingTypesTest();
        clearSecurePropertyKeysTest();
        clearSecureTypesTest();
        clearSecureFieldByConfigurationTypeMapTest();
        clearGlobalConfigurationListenersTest();
        clearConfigurationListenersTest();

        statusTest();
        reloadTest();
        reloadWithConfigTest();
    }

    private void getPropertyTest() throws Exception {
        configurationEndpointTestImpl.getPropertyTestImpl();
    }

    private void getTypedConfigurationYamlTest() throws Exception {
        configurationEndpointTestImpl.getTypedConfigurationYamlTestImpl();
    }

    private void getNonValidatingTypesTest() throws Exception {
        configurationEndpointTestImpl.getNonValidatingTypesTestImpl();
    }

    private void getSecurePropertyKeysTest() throws Exception {
        configurationEndpointTestImpl.getSecurePropertyKeysTestImpl();
    }

    private void getSecureTypesTest() throws Exception {
        configurationEndpointTestImpl.getSecureTypesTestImpl();
    }

    private void getConfigurationsAsJsonTest() throws Exception {
        configurationEndpointTestImpl.getConfigurationsAsJsonTestImpl();
    }


    private void setPropertyTest() throws Exception {
        configurationEndpointTestImpl.setPropertyTestImpl();
    }

    private void setTypedConfigurationYamlTest() throws Exception {
        configurationEndpointTestImpl.setTypedConfigurationYamlTestImpl();
    }

    private void loadTest() throws Exception {
        configurationEndpointTestImpl.loadTestImpl();
    }

    private void containsPropertyTest() throws Exception {
        configurationEndpointTestImpl.containsPropertyTestImpl();
    }

    private void containsTypedConfigurationTest() throws Exception {
        configurationEndpointTestImpl.containsTypedConfigurationTestImpl();
    }

    private void removePropertyTest() throws Exception {
        configurationEndpointTestImpl.removePropertyTestImpl();
    }

    private void removeTypedConfigurationYamlTest() throws Exception {
        configurationEndpointTestImpl.removeTypedConfigurationYamlTestImpl();
    }

    private void unregisterPropertyDeserializerAndConfigurationTypesByPropertyKeyTest() throws Exception {
        configurationEndpointTestImpl.unregisterPropertyDeserializerAndConfigurationTypesByPropertyKeyTestImpl();
    }

    private void unregisterPropertyDeserializerTest() throws Exception {
        configurationEndpointTestImpl.unregisterPropertyDeserializerTestImpl();
    }

    private void unregisterNonValidatingTypeTest() throws Exception {
        configurationEndpointTestImpl.unregisterNonValidatingTypeTestImpl();
    }

    private void unregisterSecurePropertyKeyTest() throws Exception {
        configurationEndpointTestImpl.unregisterSecurePropertyKeyTestImpl();
    }

    private void unregisterSecureTypeTest() throws Exception {
        configurationEndpointTestImpl.unregisterSecureTypeTestImpl();
    }

    private void unregisterSecureFieldByConfigurationTypeTest() throws Exception {
        configurationEndpointTestImpl.unregisterSecureFieldByConfigurationTypeTestImpl();
    }

    private void unregisterGlobalConfigurationListenerTest() throws Exception {
        configurationEndpointTestImpl.unregisterGlobalConfigurationListenerTestImpl();
    }

    private void unregisterConfigurationListenerByPropertyKeyTest() throws Exception {
        configurationEndpointTestImpl.unregisterConfigurationListenerByPropertyKeyTestImpl();
    }

    private void unregisterConfigurationListenerByTypeNameTest() throws Exception {
        configurationEndpointTestImpl.unregisterConfigurationListenerByTypeNameTestImpl();
    }

    private void clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMapTest() throws Exception {
        configurationEndpointTestImpl.clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMapTestImpl();
    }

    private void clearPropertyDeserializersTest() throws Exception {
        configurationEndpointTestImpl.clearPropertyDeserializersTestImpl();
    }

    private void clearNonValidatingTypesTest() throws Exception {
        configurationEndpointTestImpl.clearNonValidatingTypesTestImpl();
    }

    private void clearSecurePropertyKeysTest() throws Exception {
        configurationEndpointTestImpl.clearSecurePropertyKeysTestImpl();
    }

    private void clearSecureTypesTest() throws Exception {
        configurationEndpointTestImpl.clearSecureTypesTestImpl();
    }

    private void clearSecureFieldByConfigurationTypeMapTest() throws Exception {
        configurationEndpointTestImpl.clearSecureFieldByConfigurationTypeMapTestImpl();
    }

    private void clearGlobalConfigurationListenersTest() throws Exception {
        configurationEndpointTestImpl.clearGlobalConfigurationListenersTestImpl();
    }

    private void clearConfigurationListenersTest() throws Exception {
        configurationEndpointTestImpl.clearConfigurationListenersTestImpl();
    }

    private void statusTest() throws Exception {
        configurationEndpointTestImpl.statusTestImpl();
    }

    private void reloadTest() throws Exception {
        configurationEndpointTestImpl.reloadTestImpl();
    }

    private void reloadWithConfigTest() throws Exception {
        configurationEndpointTestImpl.reloadWithConfigTestImpl();
    }

}
