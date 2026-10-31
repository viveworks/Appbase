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

package com.viveworks.appbase.client.core;

import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link AppbaseClientService#getClient()} default method.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class AppbaseClientServiceTest {

    @SuppressWarnings("unchecked")
    @Test
    void getClient_delegatesToGetClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException {
        // Arrange
        final AppbaseClientService<Object, ComparableValue> service =
                mock(AppbaseClientService.class, CALLS_REAL_METHODS);
        final AppbaseClientIntegrationService<Object, ComparableValue> integrationService =
                mock(AppbaseClientIntegrationService.class);
        final Object expectedClient = new Object();

        when(service.getClientIntegrationService()).thenReturn(integrationService);
        when(integrationService.getClient()).thenReturn(expectedClient);

        // Act
        final Object actualClient = service.getClient();

        // Assert
        assertSame(expectedClient, actualClient);
        verify(service).getClientIntegrationService();
        verify(integrationService).getClient();
    }

    @SuppressWarnings("unchecked")
    @Test
    void getClient_propagatesNotFoundException()
            throws AppbaseClientIntegrationService.NotFoundException {
        // Arrange
        final AppbaseClientService<Object, ComparableValue> service =
                mock(AppbaseClientService.class, CALLS_REAL_METHODS);
        when(service.getClientIntegrationService())
                .thenThrow(AppbaseClientIntegrationService.NotFoundException.class);

        // Act + Assert
        assertThrows(AppbaseClientIntegrationService.NotFoundException.class,
                service::getClient);
    }
}
