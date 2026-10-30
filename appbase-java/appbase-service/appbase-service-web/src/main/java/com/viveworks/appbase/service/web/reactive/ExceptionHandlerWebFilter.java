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

package com.viveworks.appbase.service.web.reactive;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.jmx.PermissionDeniedException;
import com.viveworks.appbase.core.service.validation.ValidationServiceException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilter;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterOrder;
import jakarta.validation.ValidationException;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Outermost filter: maps a failed chain to an HTTP status.
 *
 * <p>The status mapping mirrors {@code ServletExceptionHandlerFilter} exactly -- {@link
 * PermissionDeniedException} and {@link AuthorizationServiceException} map to 403, {@link
 * ValidationServiceException}, {@link ValidationException} and {@link AppbaseRuntimeException} map
 * to 400, and everything else falls back to 500 -- so both faces report identical error semantics
 * for identical failures. {@link TimeoutException} additionally maps to 503; that class of failure
 * has no blocking-servlet analogue because a blocking call cannot time out inside the filter chain
 * the way a reactive one can.
 *
 * <p>An error raised AFTER the response has begun streaming cannot change the status -- the status
 * line is already on the wire. Such a failure terminates the stream and is left to the transport to
 * log; that is inherent to chunked responses, not a defect to paper over.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ExceptionHandlerWebFilter implements AppbaseServiceFilter {

    /**
     * Stable, status-specific public error codes. Deliberately NOT the throwable's class name or
     * message -- see {@link #filter(AppbaseServiceExchange, AppbaseServiceFilterChain)}.
     */
    private static final Map<Integer, String> PUBLIC_ERROR_CODES = Map.of(
            400, "BAD_REQUEST",
            403, "FORBIDDEN",
            503, "SERVICE_UNAVAILABLE");

    private static final String DEFAULT_PUBLIC_ERROR_CODE = "INTERNAL_SERVER_ERROR";

    private final AppbaseCore appbaseCore;

    private final Logger logger;

    /**
     * @param appbaseCore Appbase core, providing the log and exception services
     */
    public ExceptionHandlerWebFilter(final AppbaseCore appbaseCore) {
        this.appbaseCore = AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        this.logger = appbaseCore.getLogService().getLogger(getClass());
    }

    @Override
    public Mono<Void> filter(final AppbaseServiceExchange exchange, final AppbaseServiceFilterChain chain) {
        return chain.next(exchange)
                .onErrorResume(throwable -> {
                    final int status = statusFor(throwable);
                    return correlationIdFor(exchange).flatMap(correlationId -> {
                        // L2-09 (CWE-209): never echo the throwable's class or message to a client.
                        // Keep full details server-side under the same correlation id.
                        logger.error("Unhandled request failure [correlationId={}, status={}]",
                                correlationId, status, throwable);

                        exchange.status(status);
                        return exchange.writeJson(Map.of(
                                "error", PUBLIC_ERROR_CODES.getOrDefault(status, DEFAULT_PUBLIC_ERROR_CODE),
                                "correlationId", correlationId,
                                "status", status));
                    });
                });
    }

    /**
     * Resolves a correlation id for this failure: the request's own trace id when {@link
     * TraceIdWebFilter} has already run and set it on the exchange (it runs before this filter's
     * chain unwinds for every failure reachable past it, since {@link
     * AppbaseServiceFilterOrder#TRACE_ID} &gt; {@link AppbaseServiceFilterOrder#EXCEPTION_HANDLER}),
     * falling back to a fresh one otherwise -- a failure must always carry SOME correlation id,
     * never none.
     *
     * @param exchange the transport-neutral service exchange
     * @return a non-blank correlation id
     */
    private static Mono<String> correlationIdFor(final AppbaseServiceExchange exchange) {
        final Object traceId = exchange.attributes().get(TraceIdWebFilter.TRACE_ID_ATTRIBUTE);
        if (traceId instanceof String traceIdString && AppbaseUtil.isNotBlank(traceIdString)) {
            return Mono.just(traceIdString);
        }
        return Mono.fromCallable(() -> UUID.randomUUID().toString())
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Map a throwable to an HTTP status, mirroring {@code ServletExceptionHandlerFilter}.
     *
     * @param throwable the raised throwable
     * @return the HTTP status code
     */
    protected int statusFor(final Throwable throwable) {
        if (throwable instanceof PermissionDeniedException || throwable instanceof AuthorizationServiceException) {
            return 403;
        }

        if (throwable instanceof ValidationServiceException
                || throwable instanceof ValidationException
                || throwable instanceof AppbaseRuntimeException) {
            return 400;
        }

        if (throwable instanceof TimeoutException) {
            return 503;
        }

        return 500;
    }

    @Override
    public int order() {
        return AppbaseServiceFilterOrder.EXCEPTION_HANDLER;
    }
}
