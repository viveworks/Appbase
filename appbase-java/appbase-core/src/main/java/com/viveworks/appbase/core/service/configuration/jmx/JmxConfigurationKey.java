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

package com.viveworks.appbase.core.service.configuration.jmx;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.beans.ConstructorProperties;
import java.util.Objects;

/**
 * {@link javax.management.MXBean} compliant {@link com.viveworks.appbase.core.service.configuration.ConfigurationKey}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JmxConfigurationKey {
    private final String propertyKey;
    private final String appbaseServiceConfigurationTypeName;

    @JsonCreator
    @ConstructorProperties({"propertyKey", "appbaseServiceConfigurationTypeName"})
    public JmxConfigurationKey(
            @JsonProperty("propertyKey")
            final String propertyKey,
            @JsonProperty("appbaseServiceConfigurationTypeName")
            final String appbaseServiceConfigurationTypeName)
    {
        if (AppbaseUtil.isBlank(propertyKey) && AppbaseUtil.isBlank(appbaseServiceConfigurationTypeName)) {
            throw new AppbaseRuntimeException(
                    "Either propertyKey or appbaseServiceConfigurationTypeName must not be null");
        }

        if (AppbaseUtil.isBlank(propertyKey)) {
            AppbaseUtil.notBlank(appbaseServiceConfigurationTypeName, "appbaseServiceConfigurationTypeName");
        }

        if (AppbaseUtil.isBlank(appbaseServiceConfigurationTypeName)) {
            AppbaseUtil.notBlank(propertyKey, "propertyKey");
        }

        this.propertyKey = propertyKey;
        this.appbaseServiceConfigurationTypeName = appbaseServiceConfigurationTypeName;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public String getAppbaseServiceConfigurationTypeName() {
        return appbaseServiceConfigurationTypeName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JmxConfigurationKey that = (JmxConfigurationKey) o;
        return Objects.equals(propertyKey, that.propertyKey) &&
                Objects.equals(appbaseServiceConfigurationTypeName, that.appbaseServiceConfigurationTypeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyKey, appbaseServiceConfigurationTypeName);
    }

    @Override
    public String toString() {
        return "JmxConfigurationKey{" +
                "propertyKey='" + propertyKey + '\'' +
                ", appbaseServiceConfigurationTypeName='" + appbaseServiceConfigurationTypeName + '\'' +
                '}';
    }
}
