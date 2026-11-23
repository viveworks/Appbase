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

package com.viveworks.appbase.core.util.net.ssl;

import com.viveworks.appbase.core.util.AppbaseUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;

/**
 * Wrapper for {@link io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum SslSelectedListenerFailureBehavior {
    ACCEPT(ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT),

    FATAL_ALERT(ApplicationProtocolConfig.SelectedListenerFailureBehavior.FATAL_ALERT),

    CHOOSE_MY_LAST_PROTOCOL(ApplicationProtocolConfig.SelectedListenerFailureBehavior.CHOOSE_MY_LAST_PROTOCOL);

    private final ApplicationProtocolConfig.SelectedListenerFailureBehavior selectedListenerFailureBehavior;

    SslSelectedListenerFailureBehavior(
            final ApplicationProtocolConfig.SelectedListenerFailureBehavior selectedListenerFailureBehavior)
    {
        AppbaseUtil.notNull(selectedListenerFailureBehavior, "selectedListenerFailureBehavior");

        this.selectedListenerFailureBehavior = selectedListenerFailureBehavior;
    }

    public ApplicationProtocolConfig.SelectedListenerFailureBehavior getSelectedListenerFailureBehavior() {
        return selectedListenerFailureBehavior;
    }
}
