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
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.supportmessage.data.DeleteSupportMessageByIdResponse;
import com.viveworks.appbase.service.supportmessage.data.ExistsSupportMessageByIdResponse;
import com.viveworks.appbase.service.supportmessage.data.ReadSupportMessageByIdResponse;
import com.viveworks.appbase.service.supportmessage.data.SupportMessageIdRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class SupportMessageByIdServlet extends AbstractSupportMessageServlet {
    private static final long serialVersionUID = -4825963867301661415L;

    public static final String DEFAULT_PATH = DEFAULT_PATH_PREFIX;

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        final String messageIdStr = parameter(AppbaseServiceConstants.MESSAGE_ID_PARAM, httpServletRequest);
        if (messageIdStr == null) {
            throw new IllegalArgumentException(AppbaseServiceConstants.MESSAGE_ID_PARAM + " query parameters required");
        }

        final SupportMessageIdRequest supportMessageIdRequest = new SupportMessageIdRequest(messageIdStr, traceId);
        return (T) supportMessageService().readSupportMessageById(supportMessageIdRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> Class<T> getResponseType() {
        return (Class<T>) ReadSupportMessageByIdResponse.class;
    }

    @Override
    protected boolean isGetSupported() {
        return true;
    }

    @Override
    protected boolean isGetResponseAutoHandled() {
        return true;
    }

    @Override
    protected boolean isGetImplResultJson() {
        return false;
    }

    @Override
    protected int headImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse) throws Exception
    {
        final String messageIdStr = parameter(AppbaseServiceConstants.MESSAGE_ID_PARAM, httpServletRequest);
        if (messageIdStr == null) {
            throw new IllegalArgumentException(AppbaseServiceConstants.MESSAGE_ID_PARAM + " query parameters required");
        }

        final SupportMessageIdRequest supportMessageIdRequest = new SupportMessageIdRequest(messageIdStr, traceId);
        final ExistsSupportMessageByIdResponse existsSupportMessageByIdResponse =
                supportMessageService().existsSupportMessageById(supportMessageIdRequest);
        if (existsSupportMessageByIdResponse == null || !existsSupportMessageByIdResponse.isExists()) {
            return HttpServletResponse.SC_NOT_FOUND;
        }
        return HttpServletResponse.SC_NO_CONTENT;
    }

    @Override
    protected boolean isHeadSupported() {
        return true;
    }

    @Override
    protected boolean isHeadResponseAutoHandled() {
        return true;
    }

    @Override
    protected <T extends Traceable> Object deleteImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse) throws Exception
    {
        final String messageIdStr = parameter(AppbaseServiceConstants.MESSAGE_ID_PARAM, httpServletRequest);
        if (messageIdStr == null) {
            throw new IllegalArgumentException(AppbaseServiceConstants.MESSAGE_ID_PARAM + " query parameters required");
        }

        final SupportMessageIdRequest supportMessageIdRequest = new SupportMessageIdRequest(messageIdStr, traceId);
        return supportMessageService().deleteSupportMessageById(supportMessageIdRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> Class<T> deleteResponseType() {
        return (Class<T>) DeleteSupportMessageByIdResponse.class;
    }

    @Override
    protected boolean isDeleteResponseAutoHandled() {
        return true;
    }

    @Override
    protected boolean isPutSupported() {
        return false;
    }

    @Override
    protected boolean isPostSupported() {
        return false;
    }
}
