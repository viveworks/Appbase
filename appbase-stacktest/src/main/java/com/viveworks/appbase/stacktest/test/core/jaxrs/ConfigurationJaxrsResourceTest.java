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
package com.viveworks.appbase.stacktest.test.core.jaxrs;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractResourceTest;
import com.viveworks.appbase.stacktest.test.core.testimpl.ConfigurationEndpointTestImpl;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ConfigurationJaxrsResourceTest extends AbstractResourceTest {
    private final ConfigurationEndpointTestImpl configurationEndpointTestImpl;

    public ConfigurationJaxrsResourceTest(
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
    protected void runResourceTestsImpl() throws Exception {
        getPropertyTest();
        setPropertyTest();
        removePropertyTest();
        containsPropertyTest();

        getTypedConfigurationYamlTest();
        setTypedConfigurationYamlTest();
        removeTypedConfigurationYamlTest();
        containsTypedConfigurationTest();

        unregisterPropertyDeserializerAndConfigurationTypesByPropertyKeyTest();
        clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMapTest();

        unregisterPropertyDeserializerTest();
        clearPropertyDeserializersTest();

        getNonValidatingTypesTest();
        unregisterNonValidatingTypeTest();
        clearNonValidatingTypesTest();

        getSecurePropertyKeysTest();
        unregisterSecurePropertyKeyTest();
        clearSecurePropertyKeysTest();

        getSecureTypesTest();
        unregisterSecureTypeTest();
        clearSecureTypesTest();

        unregisterSecureFieldByConfigurationTypeTest();
        clearSecureFieldByConfigurationTypeMapTest();

        unregisterGlobalConfigurationListenerTest();
        clearGlobalConfigurationListenersTest();
        unregisterConfigurationListenerByPropertyKeyTest();
        unregisterConfigurationListenerByTypeNameTest();
        clearConfigurationListenersTest();

        getConfigurationsAsJsonTest();
        loadTest();

        statusTest();
        reloadTest();
        reloadWithConfigTest();
    }

    private void getPropertyTest() throws Exception {
        configurationEndpointTestImpl.getPropertyTestImpl();
    }

    private void setPropertyTest() throws Exception {
        configurationEndpointTestImpl.setPropertyTestImpl();
    }

    private void getTypedConfigurationYamlTest() throws Exception {
        configurationEndpointTestImpl.getTypedConfigurationYamlTestImpl();
    }

    private void removePropertyTest() throws Exception {
        configurationEndpointTestImpl.removePropertyTestImpl();
    }

    private void containsPropertyTest() throws Exception {
        configurationEndpointTestImpl.containsPropertyTestImpl();
    }

    private void setTypedConfigurationYamlTest() throws Exception {
        configurationEndpointTestImpl.setTypedConfigurationYamlTestImpl();
    }

    private void removeTypedConfigurationYamlTest() throws Exception {
        configurationEndpointTestImpl.removeTypedConfigurationYamlTestImpl();
    }

    private void containsTypedConfigurationTest() throws Exception {
        configurationEndpointTestImpl.containsTypedConfigurationTestImpl();
    }

    private void unregisterPropertyDeserializerAndConfigurationTypesByPropertyKeyTest() throws Exception {
        configurationEndpointTestImpl.unregisterPropertyDeserializerAndConfigurationTypesByPropertyKeyTestImpl();
    }

    private void clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMapTest() throws Exception {
        configurationEndpointTestImpl.clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMapTestImpl();
    }

    private void unregisterPropertyDeserializerTest() throws Exception {
        configurationEndpointTestImpl.unregisterPropertyDeserializerTestImpl();
    }

    private void clearPropertyDeserializersTest() throws Exception {
        configurationEndpointTestImpl.clearPropertyDeserializersTestImpl();
    }

    private void getNonValidatingTypesTest() throws Exception {
        configurationEndpointTestImpl.getNonValidatingTypesTestImpl();
    }

    private void unregisterNonValidatingTypeTest() throws Exception {
        configurationEndpointTestImpl.unregisterNonValidatingTypeTestImpl();
    }

    private void clearNonValidatingTypesTest() throws Exception {
        configurationEndpointTestImpl.clearNonValidatingTypesTestImpl();
    }

    private void getSecurePropertyKeysTest() throws Exception {
        configurationEndpointTestImpl.getSecurePropertyKeysTestImpl();
    }

    private void unregisterSecurePropertyKeyTest() throws Exception {
        configurationEndpointTestImpl.unregisterSecurePropertyKeyTestImpl();
    }

    private void clearSecurePropertyKeysTest() throws Exception {
        configurationEndpointTestImpl.clearSecurePropertyKeysTestImpl();
    }

    private void getSecureTypesTest() throws Exception {
        configurationEndpointTestImpl.getSecureTypesTestImpl();
    }

    private void unregisterSecureTypeTest() throws Exception {
        configurationEndpointTestImpl.unregisterSecureTypeTestImpl();
    }

    private void clearSecureTypesTest() throws Exception {
        configurationEndpointTestImpl.clearSecureTypesTestImpl();
    }

    private void unregisterSecureFieldByConfigurationTypeTest() throws Exception {
        configurationEndpointTestImpl.unregisterSecureFieldByConfigurationTypeTestImpl();
    }

    private void clearSecureFieldByConfigurationTypeMapTest() throws Exception {
        configurationEndpointTestImpl.clearSecureFieldByConfigurationTypeMapTestImpl();
    }

    private void unregisterGlobalConfigurationListenerTest() throws Exception {
        configurationEndpointTestImpl.unregisterGlobalConfigurationListenerTestImpl();
    }

    private void clearGlobalConfigurationListenersTest() throws Exception {
        configurationEndpointTestImpl.clearGlobalConfigurationListenersTestImpl();
    }

    private void unregisterConfigurationListenerByPropertyKeyTest() throws Exception {
        configurationEndpointTestImpl.unregisterConfigurationListenerByPropertyKeyTestImpl();
    }

    private void unregisterConfigurationListenerByTypeNameTest() throws Exception {
        configurationEndpointTestImpl.unregisterConfigurationListenerByTypeNameTestImpl();
    }

    private void clearConfigurationListenersTest() throws Exception {
        configurationEndpointTestImpl.clearConfigurationListenersTestImpl();
    }

    private void getConfigurationsAsJsonTest() throws Exception {
        configurationEndpointTestImpl.getConfigurationsAsJsonTestImpl();
    }

    private void loadTest() throws Exception {
        configurationEndpointTestImpl.loadTestImpl();
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
