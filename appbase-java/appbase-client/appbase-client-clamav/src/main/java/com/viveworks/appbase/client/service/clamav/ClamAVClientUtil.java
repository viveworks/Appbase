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

package com.viveworks.appbase.client.service.clamav;

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.service.clamav.configuration.DefaultClamAVClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;

/**
 * Utilities for this module.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class ClamAVClientUtil {
    private ClamAVClientUtil() {
    }

    /**
     * Generate {@link AppbaseClientKey} used by {@link ClamAVClientService}.
     *
     * @param defaultClamAVClientIntegrationServiceConfiguration {@link DefaultClamAVClientIntegrationServiceConfiguration}
     * @return {@link AppbaseClientKey}
     */
    public static AppbaseClientKey<TextValue> appbaseClientKey(
            final DefaultClamAVClientIntegrationServiceConfiguration defaultClamAVClientIntegrationServiceConfiguration)
    {
        AppbaseUtil.notNull(
                defaultClamAVClientIntegrationServiceConfiguration, "defaultClamAVClientIntegrationServiceConfiguration");

        final TextValueConfiguration textValueKeyConfiguration =
                defaultClamAVClientIntegrationServiceConfiguration.getTextValueKeyConfiguration();
        if (textValueKeyConfiguration != null) {
            return AppbaseClientUtil.textValueKey(textValueKeyConfiguration);
        }

        final HostPortConfiguration hostPortConfiguration = defaultClamAVClientIntegrationServiceConfiguration
                .getDefaultClamAVClientConfiguration()
                .getHostPortConfiguration();
        final String text = hostPortConfiguration.getHost() + AppbaseUtil.COLON + hostPortConfiguration.getPort();
        final TextValue textValue = new TextValue(text);
        return AppbaseClientUtil.textValueKey(textValue);
    }
}
