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

package com.viveworks.appbase.service.web.exceptionhandler.jaxrs;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.jmx.PermissionDeniedException;
import com.viveworks.appbase.core.service.validation.ValidationServiceException;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Provider
public class JaxrsExceptionHandlerMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(final Throwable exception) {
        if (exception instanceof PermissionDeniedException) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        if (exception instanceof AuthorizationServiceException) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        if (exception instanceof ValidationServiceException) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (exception instanceof ValidationException) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (exception instanceof AppbaseRuntimeException) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (exception instanceof WebApplicationException) {
            final Response response = ((WebApplicationException) exception).getResponse();
            if (response != null) {
                return response;
            }
        }

        return Response.serverError().build();
    }
}
