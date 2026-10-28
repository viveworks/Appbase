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

import com.viveworks.appbase.core.util.AppbaseFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.net.AppbaseNetUtil;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.inject.Inject;
import javax.net.ssl.SSLException;
import java.io.File;

/**
 * Provider for server {@link SslContext}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class SslServerContextFactory implements AppbaseFactory<SslContext> {
    private final SslServerContextConfiguration sslServerContextConfiguration;

    @Inject
    public SslServerContextFactory(final SslServerContextConfiguration sslServerContextConfiguration) {
        AppbaseUtil.notNull(sslServerContextConfiguration, "sslServerContextConfiguration");

        this.sslServerContextConfiguration = sslServerContextConfiguration;
    }

    @Override
    public SslContext getInstance() {
        final SslContextBuilder sslContextBuilder;
        if (!AppbaseUtil.isBlank(sslServerContextConfiguration.getPrivateKeyFilePassword())) {
            sslContextBuilder = SslContextBuilder.forServer(
                    new File(sslServerContextConfiguration.getPrivateKeyCertificateChainFilename()),
                    new File(sslServerContextConfiguration.getPrivateKeyFile()),
                    sslServerContextConfiguration.getPrivateKeyFilePassword());
        } else {
            sslContextBuilder = SslContextBuilder.forServer(
                    new File(sslServerContextConfiguration.getPrivateKeyCertificateChainFilename()),
                    new File(sslServerContextConfiguration.getPrivateKeyFile()));
        }

        final SslContextCommonConfiguration sslContextCommonConfiguration =
                sslServerContextConfiguration.getSslContextCommonConfiguration();
        AppbaseNetUtil.sslContext(sslContextBuilder, sslContextCommonConfiguration);

        try {
            return sslContextBuilder.build();
        } catch (SSLException ssle) {
            throw new AppbaseRuntimeException("Error building " +
                    SslContext.class.getName() + sslServerContextConfiguration, ssle);
        }
    }
}
