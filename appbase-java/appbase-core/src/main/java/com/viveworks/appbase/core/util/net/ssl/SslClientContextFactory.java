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
 * Provider for client {@link SslContext}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class SslClientContextFactory implements AppbaseFactory<SslContext> {
    private final SslClientContextConfiguration sslClientContextConfiguration;

    @Inject
    public SslClientContextFactory(final SslClientContextConfiguration sslClientContextConfiguration) {
        AppbaseUtil.notNull(sslClientContextConfiguration, "sslClientContextConfiguration");

        this.sslClientContextConfiguration = sslClientContextConfiguration;
    }

    @Override
    public SslContext getInstance() {
        final SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();

        final SslContextCommonConfiguration sslContextCommonConfiguration =
                sslClientContextConfiguration.getSslContextCommonConfiguration();
        AppbaseNetUtil.sslContext(sslContextBuilder, sslContextCommonConfiguration);

        final SslKeyManagerConfiguration sslKeyManagerConfiguration =
                sslClientContextConfiguration.getSslKeyManagerConfiguration();
        if (sslKeyManagerConfiguration != null) {
            final String privateKeyCertificateChainFilename =
                    sslKeyManagerConfiguration.getPrivateKeyCertificateChainFilename();
            final String privateKeyFile =
                    sslKeyManagerConfiguration.getPrivateKeyFile();
            final String privateKeyFilePassword =
                    sslKeyManagerConfiguration.getPrivateKeyFilePassword();

            if (privateKeyCertificateChainFilename != null &&
                    privateKeyFile != null &&
                    privateKeyFilePassword != null)
            {
                sslContextBuilder.keyManager(
                        new File(privateKeyCertificateChainFilename),
                        new File(privateKeyFile),
                        privateKeyFilePassword);
            } else if (privateKeyCertificateChainFilename != null && privateKeyFile != null) {
                sslContextBuilder.keyManager(
                        new File(privateKeyCertificateChainFilename),
                        new File(privateKeyFile));
            }
        }

        try {
            return sslContextBuilder.build();
        } catch (SSLException ssle) {
            throw new AppbaseRuntimeException("Error building " +
                    SslContext.class.getName() + sslClientContextConfiguration, ssle);
        }
    }
}
