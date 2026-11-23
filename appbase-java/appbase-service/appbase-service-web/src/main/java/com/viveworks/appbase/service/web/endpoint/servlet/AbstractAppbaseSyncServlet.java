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

package com.viveworks.appbase.service.web.endpoint.servlet;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.Traceable;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.validation.ValidationServiceException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import jakarta.validation.ValidationException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.MDC;

import javax.annotation.Nullable;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractAppbaseSyncServlet extends HttpServlet {
    private static final long serialVersionUID = 8076778481333602428L;

    protected Logger logger;

    protected Timer headTimer;
    protected Timer getTimer;
    protected Timer postTimer;
    protected Timer putTimer;
    protected Timer deleteTimer;
    protected Timer writeUploadTimer;
    protected Timer writeDownloadTimer;

    protected Counter headNotAllowedCounter;
    protected Counter getNotAllowedCounter;
    protected Counter postNotAllowedCounter;
    protected Counter putNotAllowedCounter;
    protected Counter deleteNotAllowedCounter;

    protected Counter validationExceptionCounter;
    protected Counter unexpectedExceptionCounter;

    @Override
    public void init() {
        final AppbaseCore appbaseCore = appbaseCore();
        logger = appbaseCore.getLogService().getLogger(getClass());

        final MetricService metricService = appbaseCore.getMetricService();
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        validationExceptionCounter = metricRegistry
                .counter(metricService.name(getClass(), "validationExceptionCounter"));
        unexpectedExceptionCounter = metricRegistry
                .counter(metricService.name(getClass(), "unexpectedExceptionCounter"));

        headNotAllowedCounter = metricRegistry.counter(metricService.name(getClass(), "headNotAllowedCounter.405"));
        getNotAllowedCounter = metricRegistry.counter(metricService.name(getClass(), "getNotAllowedCounter.405"));
        postNotAllowedCounter = metricRegistry.counter(metricService.name(getClass(), "postNotAllowedCounter.405"));
        putNotAllowedCounter = metricRegistry.counter(metricService.name(getClass(), "putNotAllowedCounter.405"));
        deleteNotAllowedCounter = metricRegistry.counter(metricService.name(getClass(), "deleteNotAllowedCounter.405"));

        headTimer = metricRegistry.timer(metricService.name(getClass(), "headTimer"));
        getTimer = metricRegistry.timer(metricService.name(getClass(), "getTimer"));
        postTimer = metricRegistry.timer(metricService.name(getClass(), "postTimer"));
        putTimer = metricRegistry.timer(metricService.name(getClass(), "putTimer"));
        deleteTimer = metricRegistry.timer(metricService.name(getClass(), "deleteTimer"));
        writeUploadTimer = metricRegistry.timer(metricService.name(getClass(), "writeUploadTimer"));
        writeDownloadTimer = metricRegistry.timer(metricService.name(getClass(), "writeDownloadTimer"));
    }

    protected AppbaseCore appbaseCore() {
        return AppbaseServiceUtil.appbaseCore(getServletContext());
    }

    protected String traceIdKey() {
        return TracerService.TRACE_ID_KEY;
    }

    protected int headImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        return 0;
    }

    protected <T> T getImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        return null;
    }

    protected <T> Class<T> getResponseType() {
        return null;
    }

    protected <T extends Traceable> Class<T> postRequestType() {
        return null;
    }

    protected <T extends Traceable> Object postImpl(T traceable) throws Exception {
        return null;
    }

    protected <T> Class<T> postResponseType() {
        return null;
    }

    protected <T extends Traceable> Class<T> putRequestType() {
        return null;
    }

    protected <T extends Traceable> Object putImpl(T traceable) throws Exception {
        return null;
    }

    protected <T> Class<T> putResponseType() {
        return null;
    }

    protected <T extends Traceable> Object deleteImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        return null;
    }

    protected Path uploadDestinationFile(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        return null;
    }

    @Nullable
    protected <T> T postWriteUpload(
            final String traceId,
            final Path uploadDestinationFile,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        return null;
    }

    protected <T> Class<T> fileUploadResponseType() {
        return null;
    }

    protected <T> Class<T> deleteResponseType() {
        return null;
    }

    protected boolean isHeadSupported() {
        return true;
    }

    protected boolean isHeadResponseAutoHandled() {
        return true;
    }

    protected void handleHeadResponse(
            final int httpStatusCode,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
    }

    protected boolean isGetSupported() {
        return true;
    }

    protected boolean isGetResponseAutoHandled() {
        return true;
    }

    protected boolean isGetImplResultJson() {
        return false;
    }

    protected void handleGetResponse(
            final Object response,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
    }

    protected boolean isPostSupported() {
        return true;
    }

    protected boolean isPostResponseAutoHandled() {
        return true;
    }

    protected void handlePostResponse(
            final Object response,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
    }

    protected boolean isPutSupported() {
        return true;
    }

    protected boolean isPutResponseAutoHandled() {
        return true;
    }

    protected void handlePutResponse(
            final Object response,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
    }

    protected boolean isDeleteSupported() {
        return true;
    }

    protected boolean isDeleteResponseAutoHandled() {
        return true;
    }

    protected void handleDeleteResponse(
            final Object response,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
    }

    @Override
    protected void doHead(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws ServletException, IOException
    {
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");

        final Timer.Context timer = headTimer.time();
        try (AutoCloseable traceIdScope = preserveAmbientTraceId()) {
            if (!isHeadSupported()) {
                sendMethodNotAllowed(headNotAllowedCounter, httpServletResponse);
                return;
            }

            final String traceId = traceId(httpServletRequest, httpServletResponse);
            final int httpStatusCode =
                    headImpl(traceId, httpServletRequest, httpServletResponse);
            if (isHeadResponseAutoHandled()) {
                if (httpStatusCode <= 0) {
                    httpServletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    httpServletResponse.setStatus(httpStatusCode);
                }
            } else {
                handleHeadResponse(httpStatusCode, httpServletRequest, httpServletResponse);
            }

            httpServletResponse.flushBuffer();
        } catch (ValidationException vex) {
            validationExceptionCounter.inc();
            logger.error("Validation exception in doHead method", vex);
        } catch (Exception ex) {
            if (AuthorizationServiceException.class.isAssignableFrom(ex.getClass())) {
                handleAuthorizationServiceException("HEAD", ex.getMessage(), httpServletResponse);
            } else if (isAuthenticationServiceException(ex)) {
                handleAuthenticationServiceException("HEAD", ex.getMessage(), httpServletResponse);
            } else if (isUserAccountServiceException(ex)) {
                handleUserAccountServiceException("HEAD", ex.getMessage(), httpServletResponse);
            } else {
                logger.error("Unexpected exception in doHead method", ex);
                sendUnexpectedException(unexpectedExceptionCounter, httpServletResponse);
            }
        } finally {
            timer.stop();
        }
    }

    @Override
    protected void doGet(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");

        final Timer.Context timer = getTimer.time();
        try (AutoCloseable traceIdScope = preserveAmbientTraceId()) {
            if (!isGetSupported()) {
                sendMethodNotAllowed(getNotAllowedCounter, httpServletResponse);
                return;
            }

            final String traceId = traceId(httpServletRequest, httpServletResponse);
            final Object o = getImpl(traceId, httpServletRequest, httpServletResponse);

            if (isGetResponseAutoHandled()) {
                if (!isGetImplResultJson()) {
                    writeJson(o, getResponseType(), httpServletResponse);
                } else {
                    writeJson((String) o, httpServletResponse);
                }
            } else {
                handleGetResponse(o, httpServletRequest, httpServletResponse);
            }
        } catch (ValidationException vex) {
            validationExceptionCounter.inc();
            logger.error("Validation exception in doGet method", vex);
        } catch (Exception ex) {
            if (AuthorizationServiceException.class.isAssignableFrom(ex.getClass())) {
                handleAuthorizationServiceException("GET", ex.getMessage(), httpServletResponse);
            } else if (isAuthenticationServiceException(ex)) {
                handleAuthenticationServiceException("GET", ex.getMessage(), httpServletResponse);
            } else if (isUserAccountServiceException(ex)) {
                handleUserAccountServiceException("GET", ex.getMessage(), httpServletResponse);
            } else {
                logger.error("Unexpected exception in doGet method", ex);
                sendUnexpectedException(unexpectedExceptionCounter, httpServletResponse);
            }
        } finally {
            timer.stop();
        }
    }

    @Override
    protected void doPost(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");

        final Timer.Context timer = postTimer.time();
        try (AutoCloseable traceIdScope = preserveAmbientTraceId()) {
            if (!isPostSupported()) {
                sendMethodNotAllowed(postNotAllowedCounter, httpServletResponse);
                return;
            }

            final Traceable traceable = readJson(httpServletRequest, postRequestType());
            final String traceId = traceId(httpServletRequest, httpServletResponse, traceable);
            if (AppbaseUtil.isBlank(traceable.getTraceId())) {
                traceable.setTraceId(traceId);
            }

            final Object response = postImpl(traceable);
            if (isPostResponseAutoHandled()) {
                sendCreated(response, postResponseType(), httpServletResponse);
            } else {
                handlePostResponse(response, httpServletRequest, httpServletResponse);
            }
        } catch (ValidationException vex) {
            validationExceptionCounter.inc();
            logger.error("Validation exception in doPost method", vex);
        } catch (Exception ex) {
            traceId(httpServletRequest, httpServletResponse);
            if (AuthorizationServiceException.class.isAssignableFrom(ex.getClass())) {
                handleAuthorizationServiceException("POST", ex.getMessage(), httpServletResponse);
            } else if (isAuthenticationServiceException(ex)) {
                handleAuthenticationServiceException("POST", ex.getMessage(), httpServletResponse);
            } else if (isUserAccountServiceException(ex)) {
                handleUserAccountServiceException("POST", ex.getMessage(), httpServletResponse);
            } else {
                logger.error("Unexpected exception in doPost method", ex);
                sendUnexpectedException(unexpectedExceptionCounter, httpServletResponse);
            }
        } finally {
            timer.stop();
        }
    }

    @Override
    protected void doPut(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");

        final Timer.Context timer = putTimer.time();
        try (AutoCloseable traceIdScope = preserveAmbientTraceId()) {
            if (!isPutSupported()) {
                sendMethodNotAllowed(putNotAllowedCounter, httpServletResponse);
                return;
            }

            final Traceable traceable = readJson(httpServletRequest, putRequestType());
            AppbaseUtil.notNull(traceable, "traceable");

            final String traceId = traceId(httpServletRequest, httpServletResponse, traceable);
            if (AppbaseUtil.isBlank(traceable.getTraceId())) {
                traceable.setTraceId(traceId);
            }

            final Object response = putImpl(traceable);
            if (isPutResponseAutoHandled()) {
                sendOK(response, putResponseType(), httpServletResponse);
            } else {
                handlePutResponse(response, httpServletRequest, httpServletResponse);
            }
        } catch (ValidationException vex) {
            validationExceptionCounter.inc();
            logger.error("Validation exception during HTTP PUT", vex);
            sendBadRequest(
                    "HTTP PUT",
                    vex.getMessage() != null ? vex.getMessage() : "Invalid request",
                    httpServletResponse);
        } catch (Exception ex) {
            final Throwable cause = ex.getCause();
            if (cause != null && ValidationServiceException.class.isAssignableFrom(cause.getClass())) {
                logger.error("Validation exception during HTTP PUT", cause);
                sendBadRequest(
                        "HTTP PUT",
                        ex.getMessage() != null ? ex.getMessage() : "Invalid request",
                        httpServletResponse);
                return;
            }

            traceId(httpServletRequest, httpServletResponse);
            if (AuthorizationServiceException.class.isAssignableFrom(ex.getClass())) {
                handleAuthorizationServiceException("PUT", ex.getMessage(), httpServletResponse);
            } else if (isAuthenticationServiceException(ex)) {
                handleAuthenticationServiceException("PUT", ex.getMessage(), httpServletResponse);
            } else if (isUserAccountServiceException(ex)) {
                handleUserAccountServiceException("PUT", ex.getMessage(), httpServletResponse);
            } else {
                logger.error("Unexpected exception in doPut method", ex);
                sendUnexpectedException(unexpectedExceptionCounter, httpServletResponse);
            }
        } finally {
            timer.stop();
        }
    }

    @Override
    protected void doDelete(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");

        final Timer.Context timer = deleteTimer.time();
        try (AutoCloseable traceIdScope = preserveAmbientTraceId()) {
            if (!isDeleteSupported()) {
                sendMethodNotAllowed(deleteNotAllowedCounter, httpServletResponse);
                return;
            }

            final String traceId = traceId(httpServletRequest, httpServletResponse);
            final Object o = deleteImpl(traceId, httpServletRequest, httpServletResponse);

            if (isDeleteResponseAutoHandled()) {
                sendOK(o, deleteResponseType(), httpServletResponse);
            } else {
                handleDeleteResponse(o, httpServletRequest, httpServletResponse);
            }
        } catch (ValidationException vex) {
            validationExceptionCounter.inc();
            logger.error("Validation exception in doDelete method", vex);
        } catch (Exception ex) {
            if (AuthorizationServiceException.class.isAssignableFrom(ex.getClass())) {
                handleAuthorizationServiceException("DELETE", ex.getMessage(), httpServletResponse);
            } else if (isAuthenticationServiceException(ex)) {
                handleAuthenticationServiceException("DELETE", ex.getMessage(), httpServletResponse);
            } else if (isUserAccountServiceException(ex)) {
                handleUserAccountServiceException("DELETE", ex.getMessage(), httpServletResponse);
            } else {
                logger.error("Unexpected exception in doDelete method", ex);
                sendUnexpectedException(unexpectedExceptionCounter, httpServletResponse);
            }
        } finally {
            timer.stop();
        }
    }

    protected String downloadFileName(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
    {
        return null;
    }

    @Nullable
    protected FileDownloadInfo fileDownloadInfo(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse,
            final String traceId)
            throws ServletException, IOException
    {
        return null;
    }

    protected void doFileDownload(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");

        final Timer.Context timer = getTimer.time();
        try (AutoCloseable traceIdScope = preserveAmbientTraceId()) {
            final String traceId = traceId(httpServletRequest, httpServletResponse);
            final FileDownloadInfo fileDownloadInfo = fileDownloadInfo(
                    httpServletRequest, httpServletResponse, traceId);
            if (fileDownloadInfo != null) {
                final InputStream inputStream = fileDownloadInfo.getInputStream();
                if (inputStream != null) {
                    final String mimeType = fileDownloadInfo.getMimeType() != null ?
                            fileDownloadInfo.getMimeType() :
                            ServletConstants.DEFAULT_FILE_MIME_TYPE;
                    httpServletResponse.setHeader(
                            "Content-Type",
                            mimeType);

                    final String filename = fileDownloadInfo.getFilename() != null ?
                            fileDownloadInfo.getFilename() :
                            ServletConstants.UNKNOWN_FILENAME;
                    httpServletResponse.setHeader(
                            "Content-Disposition",
                            "attachment; filename=\"" + filename + "\"");

                    final String responseTraceId = fileDownloadInfo.getTraceId() != null ?
                            fileDownloadInfo.getTraceId() :
                            traceId;
                    httpServletResponse.setHeader(traceIdKey(), responseTraceId);

                    final ServletOutputStream outputStream = httpServletResponse.getOutputStream();
                    final Timer.Context writeTimer = writeDownloadTimer.time();
                    try (outputStream) {
                        try (inputStream) {
                            while (inputStream.available() > 0) {
                                final byte[] bytes = inputStream.readNBytes(ServletConstants.STREAM_READ_N_BYTES);
                                outputStream.write(bytes);
                            }
                        }
                    } finally {
                        writeTimer.stop();
                    }
                }
            } else {
                sendError(
                        counter("doFileDownload"),
                        httpServletResponse,
                        HttpServletResponse.SC_NOT_FOUND,
                        "Not Found");
            }

            httpServletResponse.flushBuffer();
        } catch (ValidationException vex) {
            validationExceptionCounter.inc();
            logger.error("Validation exception in doFileDownload method", vex);
        } catch (Exception ex) {
            logger.error("Unexpected exception in doFileDownload method", ex);
            sendUnexpectedException(unexpectedExceptionCounter, httpServletResponse);
        } finally {
            timer.stop();
        }
    }

    protected void doFileUpload(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");

        final Timer.Context timer = postTimer.time();
        try (AutoCloseable traceIdScope = preserveAmbientTraceId()) {
            final String traceId = traceId(httpServletRequest, httpServletResponse);
            final Path uploadDestinationFile = uploadDestinationFile(traceId, httpServletRequest, httpServletResponse);
            AppbaseServiceUtil.createTemporaryFile(uploadDestinationFile);
            writeUpload(httpServletRequest, httpServletResponse, uploadDestinationFile);

            final Object response = postWriteUpload(
                    traceId, uploadDestinationFile, httpServletRequest, httpServletResponse);
            if (response != null) {
                sendOK(response, fileUploadResponseType(), httpServletResponse);
                return;
            }
            sendOK(httpServletResponse);
        } catch (ValidationException vex) {
            validationExceptionCounter.inc();
            logger.error("Validation exception in doFileUpload method", vex);
        } catch (Exception ex) {
            traceId(httpServletRequest, httpServletResponse);
            logger.error("Unexpected exception in doFileUpload method", ex);
            sendUnexpectedException(unexpectedExceptionCounter, httpServletResponse);
        } finally {
            timer.stop();
        }
    }

    // TODO: Write to object store directly?
    protected void writeUpload(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse,
            final Path destinationFile)
            throws ServletException, IOException
    {
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");

        final Timer.Context timer = writeUploadTimer.time();
        try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(destinationFile.toFile()))) {
            for (final Part part : httpServletRequest.getParts()) {
                try (final InputStream inputStream = part.getInputStream()) {
                    while (inputStream.available() > 0) {
                        final byte[] bytes = inputStream.readNBytes(ServletConstants.STREAM_READ_N_BYTES);
                        outputStream.write(bytes);
                    }
                }
            }
        } finally {
            timer.stop();
        }
    }

    /**
     * Snapshots the current thread's ambient MDC trace-id value and returns a resource that
     * restores exactly that snapshot when closed -- putting it back if one was present, removing
     * it if it was absent. A handler wraps its whole request-processing body in a
     * try-with-resources against this so that, regardless of whether {@code traceId()} ends up
     * generating and {@code MDC.put}ting a fresh value for THIS request, the ambient state this
     * handler observed on entry (an outer {@code TraceIdServletFilter}'s own value, or nothing)
     * is exactly what remains once the handler returns -- never a value this handler never set,
     * and never nothing when an outer owner's value was there before this handler ran at all
     * (the early-return-before-{@code traceId()} case).
     */
    private AutoCloseable preserveAmbientTraceId() {
        final String traceIdKey = traceIdKey();
        final String priorTraceId = MDC.get(traceIdKey);
        return () -> {
            if (priorTraceId != null) {
                MDC.put(traceIdKey, priorTraceId);
            } else {
                MDC.remove(traceIdKey);
            }
        };
    }

    protected String traceId(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
    {
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");

        final String traceIdKey = traceIdKey();

        // Only the request attribute identifies a value already resolved for THIS request (set
        // by TraceIdServletFilter before it called the servlet, or by an earlier call within the
        // same request). Ambient thread-local MDC is deliberately never consulted here: it is
        // thread-scoped, not request-scoped, and on a reused servlet-container worker thread it
        // can carry a stale value left by an unrelated earlier request -- that stale value must
        // never be allowed to outrank this request's own explicit cookie/header/parameter.
        String traceId = (String) httpServletRequest.getAttribute(traceIdKey);
        if (AppbaseUtil.isBlank(traceId)) {
            traceId = AppbaseServiceUtil.cookie(traceIdKey, httpServletRequest);
        }
        if (AppbaseUtil.isBlank(traceId)) {
            traceId = AppbaseServiceUtil.header(traceIdKey, httpServletRequest);
        }
        if (AppbaseUtil.isBlank(traceId)) {
            traceId = AppbaseServiceUtil.parameter(traceIdKey, httpServletRequest);
        }
        if (AppbaseUtil.isBlank(traceId)) {
            traceId = generateTraceId();
        }

        // Bind the resolved CURRENT-request trace id into MDC for this handler's own scope,
        // regardless of which source resolved it -- not only when freshly generated -- so any
        // logging during this request's processing observes the right value.
        // preserveAmbientTraceId() restores whatever was ambient on entry once the handler
        // returns, so this binding never outlives the request that established it.
        MDC.put(traceIdKey, traceId);
        httpServletResponse.setHeader(traceIdKey, traceId);
        return traceId;
    }

    protected String traceId(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse,
            final Traceable traceable)
    {
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");
        AppbaseUtil.notNull(traceable, "traceable");

        String traceId = traceable.getTraceId();
        if (AppbaseUtil.isBlank(traceId)) {
            if (AppbaseUtil.isNotBlank(traceId)) {
                return traceId;
            }
        }

        return traceId(httpServletRequest, httpServletResponse);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    protected int intParameter(
            final String name,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final String parameter = nonBlankParameter(name, httpServletRequest, httpServletResponse);
        return Integer.parseInt(parameter);
    }

    protected Long longParameter(
            final String name,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final String parameter = nonBlankParameter(name, httpServletRequest, httpServletResponse);
        return Long.parseLong(parameter);
    }

    protected String nonBlankParameter(
            final String name,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws ValidationException, IOException
    {
        final String parameter = AppbaseServiceUtil.parameter(name, httpServletRequest);
        if (AppbaseUtil.isBlank(parameter)) {
            sendInvalidParameter(httpServletRequest.getPathInfo(), name, httpServletResponse);
            return null;
        }

        return parameter;
    }

    @Nullable
    protected String parameter(
            final String name,
            final HttpServletRequest httpServletRequest)
    {
        return AppbaseServiceUtil.parameter(name, httpServletRequest);
    }

    protected void writeJson(
            final String json,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        httpServletResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        httpServletResponse.setHeader(
                "Content-Type",
                "application/json");
        write(json, httpServletResponse);
    }

    protected <T> void writeInternalServerError(
            final String operation,
            final Object object,
            final Class<T> type,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final Counter counter = counter(operation + "." + HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        counter.inc();

        final String json = json(type, object);
        httpServletResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        httpServletResponse.setHeader(
                "Content-Type",
                "application/json");
        httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        final PrintWriter writer = httpServletResponse.getWriter();
        writer.write(json);
        writer.flush();
        httpServletResponse.flushBuffer();
    }

    protected <T> void writeJson(
            final Object object,
            final Class<T> type,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final String json = json(type, object);
        writeJson(json, httpServletResponse);
    }

    protected <T> void writeOkJson(
            final Object object,
            final Class<T> type,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        final String json = json(type, object);
        writeJson(json, httpServletResponse);
    }

    protected <T> String json(final Class<T> type, final Object object) throws IOException {
        return JsonUtil.getJsonMapper().writerFor(type).writeValueAsString(object);
    }

    protected void writeOkText(
            final String string,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        httpServletResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        httpServletResponse.setHeader(
                "Content-Type",
                "application/json");

        writeOk(string, httpServletResponse);
    }

    protected void writeOk(
            final String string,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);

        final PrintWriter writer = httpServletResponse.getWriter();
        writer.write(string);
        writer.flush();
        httpServletResponse.flushBuffer();
    }

    protected void write(
            final String string,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final PrintWriter writer = httpServletResponse.getWriter();
        writer.write(string);
        writer.flush();
        httpServletResponse.flushBuffer();
    }

    protected String readString(final HttpServletRequest httpServletRequest) throws IOException {
        final byte[] bytes = httpServletRequest.getInputStream().readAllBytes();
        if (bytes != null) {
            // AUD-20 (CWE-838): decoding with no explicit charset used the JVM's PLATFORM
            // DEFAULT (`file.encoding`) -- an environment property, not a wire contract. The
            // same request body could decode to a different string on a different deployment
            // (or JVM flag), and the request body's own encoding is never negotiated here. Fixed
            // to a single explicit charset so decoding is deterministic across environments.
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected <T> T readJson(
            final HttpServletRequest httpServletRequest,
            final Class<T> type)
            throws IOException
    {
        final byte[] bytes = httpServletRequest.getInputStream().readAllBytes();
        return (T) JsonUtil.getJsonMapper().readerFor(type).readValue(bytes);
    }

    protected void sendNoContent(final HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
        httpServletResponse.flushBuffer();
    }

    protected void sendCreated(
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        httpServletResponse.setStatus(HttpServletResponse.SC_CREATED);
        httpServletResponse.flushBuffer();
    }

    protected <T> void sendCreated(
            final Object object,
            final Class<T> type,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        httpServletResponse.setStatus(HttpServletResponse.SC_CREATED);
        writeJson(object, type, httpServletResponse);
    }

    protected <T> void sendOK(
            final Object object,
            final Class<T> type,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        writeJson(object, type, httpServletResponse);
    }

    protected <T> void sendOK(
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.flushBuffer();
    }

    protected <T> void sendOKNoContent(
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        httpServletResponse.setHeader("Content-Length", "0");
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.flushBuffer();
    }

    protected <T> void sendOKJson(
            final String json,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        writeJson(json, httpServletResponse);
        httpServletResponse.flushBuffer();
    }

    protected void sendServiceUnavailable(
            final HttpServletResponse httpServletResponse,
            final String operation)
            throws IOException
    {
        sendError(
                counter(operation + "." + HttpServletResponse.SC_SERVICE_UNAVAILABLE),
                httpServletResponse,
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "Service Unavailable");
    }

    private void sendMethodNotAllowed(
            final Counter counter,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        sendError(
                counter,
                httpServletResponse,
                HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "Method Not Allowed");
    }

    protected void sendInvalidParameter(
            final String path,
            final String name,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        AppbaseUtil.notBlank(name, "name");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");

        logger.error("Invalid parameter: [path={}, name={}]", path, name);

        String counterName = path;
        if (counterName == null) {
            counterName = name;
        } else {
            counterName += "." + name;
        }

        sendError(
                counter(counterName),
                httpServletResponse,
                HttpServletResponse.SC_BAD_REQUEST,
                "Invalid parameter: " + name);
    }

    protected void sendUnexpectedException(
            final Counter unexpectedExceptionCounter,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        sendError(
                unexpectedExceptionCounter,
                httpServletResponse,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unexpected Exception");
    }

    protected void sendForbidden(
            final String operation,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        sendError(
                counter(operation),
                httpServletResponse,
                HttpServletResponse.SC_FORBIDDEN,
                "Forbidden");
    }

    protected void sendInternalServerError(
            final String operation,
            final String message,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final String correlationId = UUID.randomUUID().toString();
        logger.error("Internal server error [{}]: {}", correlationId, message);

        sendError(
                counter(operation),
                httpServletResponse,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Internal server error (reference " + correlationId + ")");
    }

    protected void sendInternalServerError(
            final String operation,
            final Throwable failure,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final String correlationId = UUID.randomUUID().toString();
        logger.error("Internal server error [{}]", correlationId, failure);
        sendError(
                counter(operation),
                httpServletResponse,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Internal server error (reference " + correlationId + ")");
    }

    protected void sendBadRequest(
            final String operation,
            final String message,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        logger.error("Bad request: {}", message);

        sendError(
                counter(operation),
                httpServletResponse,
                HttpServletResponse.SC_BAD_REQUEST,
                message);
    }

    protected void sendNotFound(
            final String operation,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        httpServletResponse.setHeader("Content-Length", "0");
        sendError(
                counter(operation),
                httpServletResponse,
                HttpServletResponse.SC_NOT_FOUND,
                "Not Found");
    }

    protected void sendError(
            final Counter counter,
            final HttpServletResponse httpServletResponse,
            final int statusCode,
            final String message)
            throws IOException
    {
        counter.inc();
        httpServletResponse.sendError(statusCode, message);
        httpServletResponse.flushBuffer();
    }


    protected void handleUserAccountServiceException(
            final String operation,
            final String message,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        counter("userAccountServiceException").inc();
        sendInternalServerError(operation, message, httpServletResponse);
    }

    protected void handleAuthorizationServiceException(
            final String operation,
            final String message,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        counter("authorizationServiceException").inc();
        sendInternalServerError(operation, message, httpServletResponse);
    }

    protected void handleAuthenticationServiceException(
            final String operation,
            final String message,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        counter("authenticationServiceException").inc();
        logger.warn("Authentication failed during {}", operation);
        sendError(
                counter(operation),
                httpServletResponse,
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized");
    }

    protected Counter counter(final String name) {
        final MetricService metricService = appbaseCore().getMetricService();
        final String metricName = metricService.name(
                getClass(),
                name + MetricService.METRIC_NAME_SEPARATOR + "counter");
        return metricService
                .getMetricRegistry()
                .counter(metricName);
    }

    protected Timer timer(final String operation) {
        final MetricService metricService = appbaseCore().getMetricService();
        final String metricName = metricService
                .name(getClass(), operation + MetricService.METRIC_NAME_SEPARATOR + "timer");
        return metricService.getMetricRegistry().timer(metricName);
    }

    private static boolean isAuthenticationServiceException(final Exception ex) {
        Class<?> c = ex.getClass();
        while (c != null) {
            if ("com.viveworks.appbase.service.user.account.AuthenticationServiceException".equals(c.getName())) {
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }

    private static boolean isUserAccountServiceException(final Exception ex) {
        Class<?> c = ex.getClass();
        while (c != null) {
            if ("com.viveworks.appbase.service.user.account.UserAccountServiceException".equals(c.getName())) {
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }
}
