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

package com.viveworks.appbase.core.service.mediator.message;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.ServiceStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MessageValidatorTest {
    @Test
    void testValidator() {
        final MessageValidator messageValidator = new MessageValidator();

        Assertions.assertFalse(messageValidator.isValid(null, null));

        final Message message1 = new Message();
        Assertions.assertThrows(
                AppbaseRuntimeException.class, () -> message1.setTimestamp(-2));

        final Message message2 = new Message();
        Assertions.assertThrows(
                AppbaseRuntimeException.class, () -> message2.setMessageType(null));

        final Message message3 = new Message();
        message3.setMessageType(MessageType.EXCEPTION);
        Assertions.assertThrows(
                AppbaseRuntimeException.class, () -> message3.getExceptionMessage().setSourceType(null));
        Assertions.assertFalse(messageValidator.isValid(message3, null));

        final Message message4 = new Message();
        message4.setMessageType(MessageType.EXCEPTION);
        message4.getExceptionMessage().setSourceType(getClass());
        Assertions.assertThrows(
                AppbaseRuntimeException.class, () -> message4.getExceptionMessage().setThrowable(null));
        Assertions.assertFalse(messageValidator.isValid(message4, null));

        final Message message5 = new Message();
        message5.setMessageType(MessageType.SERVICE_STATUS);
        Assertions.assertThrows(
                AppbaseRuntimeException.class, () -> message5.getServiceStatusMessage().setServiceStatus(null));
        Assertions.assertFalse(messageValidator.isValid(message5, null));

        final Message message6 = new Message();
        message6.setMessageType(MessageType.SERVICE_STATUS);
        message6.getServiceStatusMessage().setServiceStatus(ServiceStatus.STARTED);
        Assertions.assertThrows(
                AppbaseRuntimeException.class, () -> message6.getServiceStatusMessage().setServiceType(null));
        Assertions.assertFalse(messageValidator.isValid(message6, null));
    }
}
