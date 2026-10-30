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

package com.viveworks.appbase.service;



/**
 * Canonical servlet, REST, request-field, and dispatch names shared across service transports.
 * Path constants beginning with {@code DEFAULT_} form the built-in routing layout and may be used
 * when assembling deployment-specific endpoint registrations.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class AppbaseServiceConstants {
    /** Servlet-context attribute under which the running Appbase core is stored. */
    public static final String APPBASE_CORE_ATTRIBUTE = "APPBASE_CORE_ATTRIBUTE";
    /** API-version path segment used by all default REST prefixes. */
    public static final String VERSION_PATH = "/v1";

    /** Default servlet path for the configuration endpoint. */
    public static final String DEFAULT_CONFIGURATION_SERVLET_PATH = "/configuration";
    /** Default servlet path for the core endpoint. */
    public static final String DEFAULT_CORE_SERVLET_PATH = "/core";
    /** Default servlet path for the exception endpoint. */
    public static final String DEFAULT_EXCEPTION_SERVLET_PATH = "/exception";
    /** Default servlet path for the jmx endpoint. */
    public static final String DEFAULT_JMX_SERVLET_PATH = "/jmx";
    /** Default servlet path for the log endpoint. */
    public static final String DEFAULT_LOG_SERVLET_PATH = "/log";
    /** Default servlet path for the metric endpoint. */
    public static final String DEFAULT_METRIC_SERVLET_PATH = "/metric";
    /** Default servlet path for the mediator endpoint. */
    public static final String DEFAULT_MEDIATOR_SERVLET_PATH = "/mediator";
    /** Default servlet path for the service registry endpoint. */
    public static final String DEFAULT_SERVICE_REGISTRY_SERVLET_PATH = "/serviceRegistry";
    /** Default servlet path for the status endpoint. */
    public static final String DEFAULT_STATUS_SERVLET_PATH = "/status";

    /** Default user session read path. */
    public static final String DEFAULT_USER_SESSION_READ_PATH = "/userSession";
    /** Default user account read path. */
    public static final String DEFAULT_USER_ACCOUNT_READ_PATH = "/userAccount";

    /**
     * Separate servlet context used to isolate core management endpoints and their Swagger UI while
     * retaining {@code /appbase-core} in the externally visible REST prefix.
     */
    public static final String DEFAULT_APPBASE_CORE_RESTAPI_SERVLET_CONTEXT = "/appbase-core-context";
    /** Versioned REST prefix for appbase core endpoints. */
    public static final String DEFAULT_APPBASE_CORE_RESTAPI_PREFIX = "/appbase-core/restapi" + VERSION_PATH;
    /** Versioned REST prefix for appbase service endpoints. */
    public static final String DEFAULT_APPBASE_SERVICE_RESTAPI_PREFIX = "/appbase-service/restapi" + VERSION_PATH;
    /** Default versioned REST path for the configuration resource. */
    public static final String DEFAULT_CONFIGURATION_RESTAPI_PATH =
            DEFAULT_APPBASE_CORE_RESTAPI_PREFIX + "/configurations";
    /** Default versioned REST path for the core resource. */
    public static final String DEFAULT_CORE_RESTAPI_PATH =
            DEFAULT_APPBASE_CORE_RESTAPI_PREFIX + "/core";
    /** Default versioned REST path for the exception resource. */
    public static final String DEFAULT_EXCEPTION_RESTAPI_PATH =
            DEFAULT_APPBASE_CORE_RESTAPI_PREFIX + "/exceptions";
    /** Default versioned REST path for the jmx resource. */
    public static final String DEFAULT_JMX_RESTAPI_PATH =
            DEFAULT_APPBASE_CORE_RESTAPI_PREFIX + "/jmx";
    /** Default versioned REST path for the log resource. */
    public static final String DEFAULT_LOG_RESTAPI_PATH =
            DEFAULT_APPBASE_CORE_RESTAPI_PREFIX + "/logs";
    /** Default versioned REST path for the mediator resource. */
    public static final String DEFAULT_MEDIATOR_RESTAPI_PATH =
            DEFAULT_APPBASE_CORE_RESTAPI_PREFIX + "/mediator";
    /** Default versioned REST path for the metric resource. */
    public static final String DEFAULT_METRIC_RESTAPI_PATH =
            DEFAULT_APPBASE_CORE_RESTAPI_PREFIX + "/metrics";
    /** Default versioned REST path for the service registry resource. */
    public static final String DEFAULT_SERVICE_REGISTRY_RESTAPI_PATH =
            DEFAULT_APPBASE_CORE_RESTAPI_PREFIX + "/serviceRegistries";
    /** Default versioned REST path for the status resource. */
    public static final String DEFAULT_STATUS_RESTAPI_PATH =
            DEFAULT_APPBASE_CORE_RESTAPI_PREFIX + "/status";

    /** Default versioned REST path for the user accounts resource. */
    public static final String DEFAULT_USER_ACCOUNTS_RESTAPI_PATH =
            DEFAULT_APPBASE_SERVICE_RESTAPI_PREFIX + "/userAccounts";
    /** Default versioned REST path for the health check resource. */
    public static final String DEFAULT_HEALTH_CHECK_RESTAPI_PATH =
            DEFAULT_APPBASE_SERVICE_RESTAPI_PREFIX + "/healthChecks";

    /** Default versioned REST path for the support message resource. */
    public static final String DEFAULT_SUPPORT_MESSAGE_RESTAPI_PATH =
            DEFAULT_APPBASE_SERVICE_RESTAPI_PREFIX + "/supportMessage";

    /** Request property used to carry the metric filter timer context. */
    public static final String METRIC_FILTER_TIMER_CONTEXT_PROPERTY =
            "MetricJaxrsRequestFilter-timerContext";
    /** Query parameter selecting a management operation. */
    public static final String OPERATION_PARAMETER = "operation";
    /** Operation name for counter metrics. */
    public static final String COUNTER = "counter";
    /** Configuration token matching every path or HTTP method. */
    public static final String WILDCARD = "*";

    /** Request parameter carrying the appbase session id. */
    public static final String APPBASE_SESSION_ID_PARAM = "appbaseSessionId";
    /** Request parameter carrying the new appbase session id. */
    public static final String NEW_APPBASE_SESSION_ID_PARAM = "newAppbaseSessionId";
    /** Request parameter carrying the appbase user id. */
    public static final String APPBASE_USER_ID_PARAM = "appbaseUserId";
    /** Request parameter carrying the appbase requester id. */
    public static final String APPBASE_REQUESTER_ID_PARAM = "appbaseRequesterId";
    /** Request parameter carrying the idp user token. */
    public static final String IDP_USER_TOKEN_PARAM = "idpUserToken";
    /** Request parameter controlling whether an operation is forced. */
    public static final String FORCE_PARAM = "force";
    /** Request parameter controlling whether only status data is returned. */
    public static final String STATUS_ONLY_PARAM = "statusOnly";
    /** Request parameter carrying the offset. */
    public static final String OFFSET_PARAM = "offset";
    /** Request parameter carrying the limit. */
    public static final String LIMIT_PARAM = "limit";
    /** Request parameter carrying the message id. */
    public static final String MESSAGE_ID_PARAM = "messageId";

    /**
     * Upper bound on a single page requested via a {@code limit} query parameter (Lens 4 / L4-03,
     * CWE-400: an unbounded limit lets a caller amplify database, heap and response work with one
     * request). Set by the owner to 500 (L4-03 delta) -- deliberately tighter than the generic
     * page-size precedent, {@code ReactiveReadBoundaryConfiguration.DEFAULT_MAX_PAGE_SIZE}.
     * Enforced both on the JAX-RS {@code @Max} annotation and at the service boundary
     * ({@code ReadSupportMessagesRequest.Validator}), so the servlet face and direct service
     * callers cannot bypass it.
     */
    public static final int MAX_SUPPORT_MESSAGE_READ_LIMIT = 500;

    /** Client-facing message used when no session can be resolved. */
    public static final String SESSION_NOT_FOUND = "Session not found";
    /** Client-facing message used when an unclassified failure escapes. */
    public static final String UNEXPECTED_EXCEPTION = "Unexpected Exception";
    /** Display name used for a user authentication token. */
    public static final String USER_AUTHENTICATION_TOKEN = "User authentication token";
    /** Dispatch key for the redirect response action. */
    public static final String SEND_REDIRECT = "sendRedirect";
    /** Dispatch key for the net http response status response action. */
    public static final String SEND_NET_HTTP_RESPONSE_STATUS = "sendNetHttpResponseStatus";

    /** Dispatch key for the user account service exception exception handler. */
    public static final String HANDLE_USER_ACCOUNT_SERVICE_EXCEPTION = "handleUserAccountServiceException";
    /** Dispatch key for the session expired exception handler. */
    public static final String HANDLE_SESSION_EXPIRED = "handleSessionExpired";
    /** Dispatch key for the session not found exception handler. */
    public static final String HANDLE_SESSION_NOT_FOUND = "handleSessionNotFound";
    /** Request property key under which the resolved user session is stored. */
    public static final String USER_SESSION_PROPERTY_KEY = "com.viveworks.appbase.service.user.session.data.UserSession";

    /** Base priority assigned to filters that must run early. */
    public static final int PRIORITY_HIGH = 10;
    /** Priority assigned to filters intended to run after high-priority filters. */
    public static final int PRIORITY_LOW = PRIORITY_HIGH + 10;

    /** Pattern used when service date-times are serialized without an offset. */
    public static final String DATE_TIME_SERIALIZATION_FORMAT = "yyyy-MM-dd HH:mm:ss";
}
