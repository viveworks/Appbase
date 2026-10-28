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

package com.viveworks.appbase.core.util.net.ssl;

import com.viveworks.appbase.core.util.AppbaseUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;

/**
 * Wrapper for {@link io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum SslSelectorFailureBehavior {
    FATAL_ALERT(ApplicationProtocolConfig.SelectorFailureBehavior.FATAL_ALERT),

    NO_ADVERTISE(ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE),

    CHOOSE_MY_LAST_PROTOCOL(ApplicationProtocolConfig.SelectorFailureBehavior.CHOOSE_MY_LAST_PROTOCOL);

    private final ApplicationProtocolConfig.SelectorFailureBehavior selectorFailureBehavior;

    SslSelectorFailureBehavior(
            final ApplicationProtocolConfig.SelectorFailureBehavior selectorFailureBehavior)
    {
        AppbaseUtil.notNull(selectorFailureBehavior, "selectorFailureBehavior");

        this.selectorFailureBehavior = selectorFailureBehavior;
    }

    public ApplicationProtocolConfig.SelectorFailureBehavior getSelectorFailureBehavior() {
        return selectorFailureBehavior;
    }
}
