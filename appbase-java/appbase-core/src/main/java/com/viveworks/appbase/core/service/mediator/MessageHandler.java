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

package com.viveworks.appbase.core.service.mediator;

import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageType;

/**
 * Handler for message.
 *
 * <p>
 *     A handler needs to be registered with the mediator (using
 *     {@link Mediator#registerMessageHandlerByMessageType(MessageType, MessageHandler)}.
 *     The mediator will then call the handler when there are message for the registered type.
 * </p>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface MessageHandler {
    /**
     * Handle the message from mediator.
     *
     * @param message Message to handle
     * @throws MessageHandlerException if there were any errors while handling the message
     */
    void onMessage(Message message) throws MessageHandlerException;
}
