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
package com.viveworks.appbase.service.supportmessage.endpoint.servlet;

import com.viveworks.appbase.core.service.tracer.Traceable;
import com.viveworks.appbase.service.supportmessage.data.UpdateStatusRequest;
import com.viveworks.appbase.service.supportmessage.data.UpdateSupportMessageResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class UpdateStatusServlet extends AbstractSupportMessageServlet {
    private static final long serialVersionUID = 1092738175486771561L;

    public static final String DEFAULT_PATH = DEFAULT_PATH_PREFIX + "/status";

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Traceable> Class<T> putRequestType() {
        return (Class<T>) UpdateStatusRequest.class;
    }

    @Override
    protected <T extends Traceable> Object putImpl(final T traceable) throws Exception {
        final UpdateStatusRequest updateStatusRequest = (UpdateStatusRequest) traceable;
        return supportMessageService().updateStatus(updateStatusRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> Class<T> putResponseType() {
        return (Class<T>) UpdateSupportMessageResponse.class;
    }

    @Override
    protected boolean isPutSupported() {
        return true;
    }

    @Override
    protected boolean isPutResponseAutoHandled() {
        return true;
    }

    @Override
    protected boolean isHeadSupported() {
        return false;
    }

    @Override
    protected boolean isGetSupported() {
        return false;
    }

    @Override
    protected boolean isPostSupported() {
        return false;
    }

    @Override
    protected boolean isDeleteSupported() {
        return false;
    }
}
