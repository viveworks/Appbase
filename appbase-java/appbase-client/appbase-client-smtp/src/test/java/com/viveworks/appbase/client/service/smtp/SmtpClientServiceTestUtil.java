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

package com.viveworks.appbase.client.service.smtp;

import com.hubspot.smtp.client.SmtpSession;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.MutableReference;
import org.junit.jupiter.api.Assertions;

import java.util.concurrent.CompletableFuture;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class SmtpClientServiceTestUtil {
    private SmtpClientServiceTestUtil() {
    }

    public static void smtpClientServiceTest(
            final SmtpClientService smtpClientService,
            final SmtpClientIntegrationService smtpClientIntegrationService)
    {
        smtpClientServiceTestImpl(smtpClientService, smtpClientIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>();
        smtpClientIntegrationService.shutdown();
        smtpClientIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
            @Override
            public void onSuccess() {
                repaired.setReference(true);
            }

            @Override
            public void onFailure() {
                repaired.setReference(false);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                throw new AppbaseRuntimeException(throwable);
            }
        });
        Assertions.assertTrue(repaired.getReference());

        smtpClientServiceTestImpl(smtpClientService, smtpClientIntegrationService);
        smtpClientService.shutdown();

        Assertions.assertNull(smtpClientIntegrationService.getClient());
        Assertions.assertFalse(smtpClientIntegrationService.isHealthy());
    }

    private static void smtpClientServiceTestImpl(
            final SmtpClientService smtpClientService,
            final SmtpClientIntegrationService smtpClientIntegrationService)
    {
        Assertions.assertNotNull(smtpClientService);
        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = smtpClientService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);


        Assertions.assertNotNull(smtpClientIntegrationService);

        final SmtpSession smtpSession = smtpClientIntegrationService.getClient();
        Assertions.assertNotNull(smtpSession);

        final CompletableFuture<Void> closeFuture = smtpSession.getCloseFuture();
        Assertions.assertNotNull(closeFuture);
        Assertions.assertFalse(closeFuture.isDone());

        Assertions.assertTrue(smtpClientIntegrationService.isHealthy());
        Assertions.assertTrue(smtpClientIntegrationService.isRepairEnabled());
        Assertions.assertEquals(SmtpSession.class, smtpClientIntegrationService.getClientType());
    }
}
