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
package com.viveworks.appbase.service.web.cors;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.MapOfStringStringValue;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultCorsFilter implements CorsFilter {
    @Override
    public Map<String, String> corsHeaders(
            final AppbaseCore appbaseCore,
            final String requestUri,
            @Nullable
            final Map<String, String> requestHeaders)
    {
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        AppbaseUtil.notBlank(requestUri, "requestUri");

        final Map<String, String> responseHeaders = new ConcurrentHashMap<>();
        final CorsFilterConfiguration corsFilterConfiguration =
                appbaseCore.getConfigurationService().getTypedConfiguration(CorsFilterConfiguration.class);

        if (corsFilterConfiguration != null) {
            final Map<String, MapOfStringStringValue> pathByCorsHeadersMap =
                    corsFilterConfiguration.getPathByCorsHeadersMap();

            MapOfStringStringValue corsHeadersMapValue = pathByCorsHeadersMap.get(requestUri);
            if (corsHeadersMapValue == null) {
                corsHeadersMapValue = pathByCorsHeadersMap.get(AppbaseServiceConstants.WILDCARD);
            }

            if (corsHeadersMapValue != null) {
                final Map<String, String> corsHeadersMap = corsHeadersMapValue.getValue();
                if (corsHeadersMap != null) {
                    final String allowOrigin =
                            corsHeadersMap.get("Access-Control-Allow-Origin");
                    if (!AppbaseUtil.isBlank(allowOrigin)) {
                        responseHeaders.put("Access-Control-Allow-Origin", allowOrigin);
                    }

                    final String allowMethods =
                            corsHeadersMap.get("Access-Control-Allow-Methods");
                    if (!AppbaseUtil.isBlank(allowMethods)) {
                        responseHeaders.put("Access-Control-Allow-Methods", allowMethods);
                    }

                    final String allowHeaders =
                            corsHeadersMap.get("Access-Control-Allow-Headers");
                    if (allowHeaders != null) {
                        responseHeaders.put("Access-Control-Allow-Headers", allowHeaders);
                    }
                }
            }
        }

        return responseHeaders;
    }
}
