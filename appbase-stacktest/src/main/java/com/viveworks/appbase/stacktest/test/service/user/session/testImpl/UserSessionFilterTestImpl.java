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
package com.viveworks.appbase.stacktest.test.service.user.session.testImpl;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.user.account.UserAccountServiceException;
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordResponse;
import com.viveworks.appbase.service.user.account.data.CreateUnauthenticatedSessionResponse;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.store.UserSessionStore;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import com.viveworks.appbase.service.web.filter.servlet.AppbaseHttpServletRequestWrapper;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.test.service.user.account.AbstractUserAccountApiTest;
import com.viveworks.appbase.stacktest.test.service.user.session.jaxrs.UserSessionJaxrsFilterTest;
import com.viveworks.appbase.stacktest.test.service.user.session.servlet.UserSessionServletFilterTest;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class UserSessionFilterTestImpl extends AbstractUserAccountApiTest {
    //public static final String TEST_NAME = "filterTestImpl";

    private final Class<?> testType;

    public UserSessionFilterTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        checkTestType(testType, UserSessionJaxrsFilterTest.class, UserSessionServletFilterTest.class);
        this.testType = testType;
    }

    public void filterTestImpl() throws Exception {
        startTest(getClass(), "filterTestImpl");

        final String url;
        if (isRestApiTest()) {
            url = baseUrl + StackTestConstants.USER_SESSION_FILTER_TEST_RESOURCE_PATH;
        } else {
            url = baseUrl + StackTestConstants.USER_SESSION_FILTER_TEST_SERVLET_PATH;
        }

        try {
            authenticatedUserSessionTest(url);
            unauthenticatedUserSessionTest(url);
            notFoundUserSessionTest(url);
            newSessionTest(url);
            expiredSessionTest(url);
        } finally {
            endTest(getClass(), "filterTestImpl");
        }

    }

    // `appbaseSessionId` null, new session is created
    // Set `UserSessionFilterConfiguration.createSessionIfAbsentByPathMap`:
    // `WILDCARD`, Servlet `requestURI` or Resource `path`
    private void newSessionTest(final String url) throws Exception {
        startTest("newSessionTest");

        String appbaseUserId = null;
        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, "newSessionTest", "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, "newSessionTest", "userAccount");
            appbaseUserId = userAccount.getAppbaseUserId();

            newSessionCookieTest(url, appbaseUserId);
            newSessionHeaderTest(url, appbaseUserId);
            newSessionQueryParamTest(url, appbaseUserId);
        } finally {
            if (userSession != null) {
                deleteSession(userSession);
            }

            if (appbaseUserId != null) {
                try {
                    deleteAccountByAppbaseUserId(appbaseUserId);
                } catch (UserAccountServiceException.EmptyResultException e) {
                    // ignore, it is deleted
                }
            }
        }

        endTest("newSessionTest");
    }

    // Create session and update last access time to past (hack)
    // Set UserSessionExpiredExceptionConfiguration.renewSession
    // Set `DefaultUserSessionStoreConfiguration.sessionExpirationSeconds` appropriately
    private void expiredSessionTest(final String url) throws Exception {
        startTest("expiredSessionTest");

        expiredSessionCookieTest(url);
        expiredSessionHeaderTest(url);
        expiredSessionQueryParamTest(url);

        endTest("expiredSessionTest");
    }

    private void expiredSessionCookieTest(final String url) throws Exception {
        final CreateUnauthenticatedSessionResponse createUnauthenticatedSessionResponse =
                createUnauthenticatedSession();
        checkNotNull(
                createUnauthenticatedSessionResponse,
                "expiredSessionCookieTest",
                "createUnauthenticatedSessionResponse");

        UserSession userSession = null;
        BasicClientCookie cookie1 = null;
        try {
            userSession = createUnauthenticatedSessionResponse.getUserSession();

            final UserSessionStore userSessionStore = appbaseServices.get(UserSessionStore.class);
            final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
            userSession.setCreationTimestamp(now.minusMonths(10));
            userSession.setLastAccessTimestamp(now.minusMonths(5));
            userSessionStore.updateSession(userSession);

            cookie1 = addCookie(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, userSession.getAppbaseSessionId());
            final HttpHead httpHead = new HttpHead();
            httpHead.setURI(URI.create(url));
            final HttpResponse httpResponse = executeHttpRequest(httpHead);
            checkNewAppbaseSessionId(userSession, httpResponse, "expiredSessionCookieTest");
            checkStatusCode(httpResponse.getStatusLine().getStatusCode(), "expiredSessionCookieTest");
        } finally {
            if (cookie1 != null) {
                clearCookies(cookie1);
            }

            if (userSession != null) {
                deleteUnauthenticatedSession(userSession);
            }
        }
    }

    private void expiredSessionHeaderTest(final String url) throws Exception {
        final CreateUnauthenticatedSessionResponse createUnauthenticatedSessionResponse =
                createUnauthenticatedSession();
        checkNotNull(
                createUnauthenticatedSessionResponse,
                "expiredSessionHeaderTest",
                "createUnauthenticatedSessionResponse");

        UserSession userSession = null;
        try {
            userSession = createUnauthenticatedSessionResponse.getUserSession();

            final UserSessionStore userSessionStore = appbaseServices.get(UserSessionStore.class);
            final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
            userSession.setCreationTimestamp(now.minusMonths(10));
            userSession.setLastAccessTimestamp(now.minusMonths(5));
            userSessionStore.updateSession(userSession);

            final HttpResponse httpResponse = httpHeadResponse(
                    url, Map.of(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, userSession.getAppbaseSessionId()));
            checkNewAppbaseSessionId(userSession, httpResponse, "expiredSessionCookieTest");
            checkStatusCode(httpResponse.getStatusLine().getStatusCode(), "expiredSessionCookieTest");
        } finally {
            if (userSession != null) {
                deleteUnauthenticatedSession(userSession);
            }
        }
    }

    private void expiredSessionQueryParamTest(final String url) throws Exception {
        final CreateUnauthenticatedSessionResponse createUnauthenticatedSessionResponse =
                createUnauthenticatedSession();
        checkNotNull(
                createUnauthenticatedSessionResponse,
                "expiredSessionQueryParamTest",
                "createUnauthenticatedSessionResponse");

        UserSession userSession = null;
        try {
            userSession = createUnauthenticatedSessionResponse.getUserSession();

            final UserSessionStore userSessionStore = appbaseServices.get(UserSessionStore.class);
            final LocalDateTime now = AppbaseServiceUtil.nowSeconds();
            userSession.setCreationTimestamp(now.minusMonths(10));
            userSession.setLastAccessTimestamp(now.minusMonths(5));
            userSessionStore.updateSession(userSession);

            final String finalUrl = url +
                    "?" + AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM + "=" + userSession.getAppbaseSessionId();
            final int statusCode = httpHead(finalUrl);
            checkNotFoundStatusCode(statusCode, "expiredSessionQueryParamTest");
        } finally {
            if (userSession != null) {
                deleteUnauthenticatedSession(userSession);
            }
        }
    }

    private void newSessionCookieTest(
            final String url,
            final String appbaseUserId)
            throws Exception
    {
        final BasicClientCookie cookie2 =
                addCookie(AppbaseServiceConstants.APPBASE_USER_ID_PARAM, appbaseUserId);

        final HttpHead httpHead = new HttpHead();
        httpHead.setURI(URI.create(url));
        try {
            final HttpResponse httpResponse = executeHttpRequest(httpHead);
            checkResponse(httpResponse, "newSessionCookieTest");
        } finally {
            clearCookies(cookie2);
        }
    }

    private void newSessionHeaderTest(
            final String url,
            final String appbaseUserId)
            throws Exception
    {
        final HttpResponse httpResponse = httpHeadResponse(
                url, Map.of(AppbaseServiceConstants.APPBASE_USER_ID_PARAM, appbaseUserId));
        checkResponse(httpResponse, "newSessionHeaderTest");
    }

    private void newSessionQueryParamTest(
            final String url,
            final String appbaseUserId)
            throws Exception
    {
        final String finalUrl = url + "?" + AppbaseServiceConstants.APPBASE_USER_ID_PARAM + "=" + appbaseUserId;
        final int statusCode = httpHead(finalUrl);
        checkNotFoundStatusCode(statusCode, "newSessionQueryParamTest");
    }

    // Random `appbaseSessionId` and `appbaseUserId`
    private void notFoundUserSessionTest(final String url) throws Exception {
        startTest("notFoundSessionTest");

        notFoundSessionUserIdCookieTest(url);
        notFoundSessionUserIdHeaderTest(url);
        notFoundSessionUserIdQueryParamTest(url);

        notFoundSessionCookieTest(url);
        notFoundSessionHeaderTest(url);
        notFoundSessionQueryParamTest(url);

        endTest("notFoundSessionTest");
    }

    private void notFoundSessionUserIdCookieTest(final String url) throws Exception {
        final String randomId1 = UUID.randomUUID().toString();
        Thread.sleep(50);
        final String randomId2 = UUID.randomUUID().toString();

        final BasicClientCookie cookie1 =
                addCookie(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, randomId1);
        final BasicClientCookie cookie2 =
                addCookie(AppbaseServiceConstants.APPBASE_USER_ID_PARAM, randomId2);

        final HttpHead httpHead = new HttpHead();
        httpHead.setURI(URI.create(url));
        try {
            final HttpResponse httpResponse = executeHttpRequest(httpHead);
            checkNotFoundResponse(httpResponse, "notFoundSessionUserIdCookieTest");
        } finally {
            clearCookies(cookie1);
            clearCookies(cookie2);
        }
    }

    private void notFoundSessionUserIdHeaderTest(final String url) throws Exception {
        final String randomId1 = UUID.randomUUID().toString();
        Thread.sleep(50);
        final String randomId2 = UUID.randomUUID().toString();

        final HttpResponse httpResponse = httpHeadResponse(
                url,
                Map.of(
                        AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, randomId1,
                        AppbaseServiceConstants.APPBASE_USER_ID_PARAM, randomId2));
        checkNotFoundResponse(httpResponse, "notFoundSessionUserIdHeaderTest");
    }

    private void notFoundSessionUserIdQueryParamTest(final String url) throws Exception {
        final String randomId1 = UUID.randomUUID().toString();
        Thread.sleep(50);
        final String randomId2 = UUID.randomUUID().toString();

        final String finalUrl = url +
                "?" + AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM + "=" + randomId1 +
                "&" + AppbaseServiceConstants.APPBASE_USER_ID_PARAM + "=" + randomId2;
        final int statusCode = httpHead(finalUrl);
        checkNotFoundStatusCode(statusCode, "notFoundSessionUserIdQueryParamTest");
    }

    private void notFoundSessionCookieTest(final String url) throws Exception {
        final String randomId = UUID.randomUUID().toString();

        BasicClientCookie cookie1 = null;
        try {
            cookie1 = addCookie(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, randomId);
            final HttpHead httpHead = new HttpHead();
            httpHead.setURI(URI.create(url));
            final HttpResponse httpResponse = executeHttpRequest(httpHead);
            checkNotFoundResponse(httpResponse, "notFoundSessionCookieTest");
        } finally {
            if (cookie1 != null) {
                clearCookies(cookie1);
            }
        }
    }

    private void notFoundSessionHeaderTest(final String url) throws Exception {
        final String randomId = UUID.randomUUID().toString();

        final HttpResponse httpResponse = httpHeadResponse(
                url, Map.of(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, randomId));
        checkNotFoundResponse(httpResponse, "notFoundSessionHeaderTest");
    }

    private void notFoundSessionQueryParamTest(final String url) throws Exception {
        final String randomId = UUID.randomUUID().toString();

        final String finalUrl = url + "?" + AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM + "=" + randomId;
        final int statusCode = httpHead(finalUrl);
        checkNotFoundStatusCode(statusCode, "notFoundSessionQueryParamTest");
    }

    // No `appbaseUserId`, read unauthenticated session
    private void unauthenticatedUserSessionTest(final String url) throws Exception {
        startTest("unauthenticatedUserSessionTest");

        final CreateUnauthenticatedSessionResponse createUnauthenticatedSessionResponse =
                createUnauthenticatedSession();
        checkNotNull(
                createUnauthenticatedSessionResponse,
                "unauthenticatedUserSessionTest",
                "createUnauthenticatedSessionResponse");

        UserSession userSession = null;
        try {
            userSession = createUnauthenticatedSessionResponse.getUserSession();

            unauthenticatedSessionCookieTest(url, userSession);
            unauthenticatedSessionHeaderTest(url, userSession);
            unauthenticatedSessionQueryParamTest(url, userSession);
        } finally {
            if (userSession != null) {
                deleteUnauthenticatedSession(userSession);
            }
        }

        endTest("unauthenticatedUserSessionTest");
    }

    private void unauthenticatedSessionCookieTest(final String url, UserSession userSession) throws Exception {
        BasicClientCookie cookie1 = null;
        try {
            cookie1 = addCookie(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, userSession.getAppbaseSessionId());
            final HttpHead httpHead = new HttpHead();
            httpHead.setURI(URI.create(url));
            final HttpResponse httpResponse = executeHttpRequest(httpHead);
            checkResponse(httpResponse, "unauthenticatedSessionCookieTest");
        } finally {
            if (cookie1 != null) {
                clearCookies(cookie1);
            }
        }
    }

    private void unauthenticatedSessionHeaderTest(
            final String url,
            final UserSession userSession)
            throws Exception
    {
        final HttpResponse httpResponse = httpHeadResponse(
                url, Map.of(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, userSession.getAppbaseSessionId()));
        checkResponse(httpResponse, "unauthenticatedSessionHeaderTest");
    }

    private void unauthenticatedSessionQueryParamTest(
            final String url,
            final UserSession userSession)
            throws Exception
    {
        final String finalUrl = url +
                "?" + AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM + "=" + userSession.getAppbaseSessionId();
        final int statusCode = httpHead(finalUrl);
        checkNotFoundStatusCode(statusCode, "unauthenticatedSessionQueryParamTest");
    }

    private void authenticatedUserSessionTest(final String url) throws Exception {
        startTest("authenticatedUserSessionTest");

        String appbaseUserId = null;
        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            checkNotNull(userSession, "authenticatedUserSessionTest", "userSession");

            final UserAccount userAccount = userSession.getUserAccount();
            checkNotNull(userAccount, "authenticatedUserSessionTest", "userAccount");
            appbaseUserId = userAccount.getAppbaseUserId();

            authenticatedSessionCookieTest(url, userSession, appbaseUserId);
            authenticatedSessionHeaderTest(url, userSession, appbaseUserId);
            authenticatedSessionQueryParamTest(url, userSession, appbaseUserId);
        } finally {
            if (userSession != null) {
                deleteSession(userSession);
            }

            if (appbaseUserId != null) {
                try {
                    deleteAccountByAppbaseUserId(appbaseUserId);
                } catch (UserAccountServiceException.EmptyResultException e) {
                    // ignore, it is deleted
                }
            }
        }

        endTest("authenticatedUserSessionTest");
    }

    private void authenticatedSessionCookieTest(
            final String url,
            final UserSession userSession,
            final String appbaseUserId) throws Exception
    {
        final BasicClientCookie cookie1 =
                addCookie(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, userSession.getAppbaseSessionId());
        final BasicClientCookie cookie2 =
                addCookie(AppbaseServiceConstants.APPBASE_USER_ID_PARAM, appbaseUserId);
        final BasicClientCookie cookie3 =
                addCookie(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, userSession.getIdpUserToken());

        final HttpHead httpHead = new HttpHead();
        httpHead.setURI(URI.create(url));
        try {
            final HttpResponse httpResponse = executeHttpRequest(httpHead);
            checkResponse(httpResponse, "authenticatedSessionHeaderTest");
        } finally {
            clearCookies(cookie1);
            clearCookies(cookie2);
            clearCookies(cookie3);
        }
    }

    private void authenticatedSessionHeaderTest(
            final String url,
            final UserSession userSession,
            final String appbaseUserId) throws Exception
    {
        final HttpResponse httpResponse = httpHeadResponse(
                url,
                Map.of(
                        AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, userSession.getAppbaseSessionId(),
                        AppbaseServiceConstants.APPBASE_USER_ID_PARAM, appbaseUserId,
                        AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, userSession.getIdpUserToken()));
        checkResponse(httpResponse, "authenticatedSessionHeaderTest");
    }

    private void authenticatedSessionQueryParamTest(
            final String url,
            final UserSession userSession,
            final String appbaseUserId) throws Exception
    {
        final String finalUrl = url +
                "?" + AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM + "=" + userSession.getAppbaseSessionId() +
                "&" + AppbaseServiceConstants.APPBASE_USER_ID_PARAM + "=" + appbaseUserId;
        final int statusCode = httpHead(finalUrl);
        checkNotFoundStatusCode(statusCode, "authenticatedSessionQueryParamTest");
    }

    private void checkResponse(
            final HttpResponse httpResponse,
            final String testName)
            throws Exception
    {
        checkNotNull(httpResponse, testName, "httpResponse");

        final HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            final String response = EntityUtils.toString(entity);
            if (response != null) {
                logger.info(">>>> Response: {}", response);
            }
        }
        final int statusCode = httpResponse.getStatusLine().getStatusCode();
        checkStatusCode(statusCode, testName);
    }

    private void checkNotFoundResponse(
            final HttpResponse httpResponse,
            final String testName)
            throws Exception
    {
        checkNotNull(httpResponse, testName, "httpResponse");

        final HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            final String response = EntityUtils.toString(entity);
            if (response != null) {
                logger.info(">>>> Response: {}", response);
            }
        }
        final int statusCode = httpResponse.getStatusLine().getStatusCode();
        checkNotFoundStatusCode(statusCode, testName);
    }

    private void checkNotFoundStatusCode(final int statusCode, final String testName) {
        checkEquals(HttpServletResponse.SC_BAD_REQUEST, statusCode, testName, "statusCode");
    }

    private void checkStatusCode(final int statusCode, final String testName) {
        checkEquals(HttpServletResponse.SC_NO_CONTENT, statusCode, testName, "statusCode");
    }

    private void checkNewAppbaseSessionId(
            final UserSession userSession,
            final HttpResponse httpResponse,
            final String testName)
    {
        final Header header = httpResponse.getFirstHeader(AppbaseServiceConstants.NEW_APPBASE_SESSION_ID_PARAM);
        checkNotNull(header, testName, "header");

        final String value = header.getValue();
        checkNotNull(value, testName, "header.value");
        checkNotEquals(value, userSession.getAppbaseSessionId(), testName, "header.value.equals");
    }

    private boolean isRestApiTest() {
        return UserSessionJaxrsFilterTest.class.isAssignableFrom(testType);
    }

    @Path(StackTestConstants.USER_SESSION_FILTER_TEST_RESOURCE_PATH)
    public static class UserSessionTestResource extends AbstractAppbaseJaxrsEntity {
        @HEAD
        public Response ping(
                @Context
                final ServletContext servletContext,
                @Context
                final HttpServletRequest httpServletRequest)
        {
            final UserSession userSession = (UserSession)
                    httpServletRequest.getAttribute(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY);
            if (userSession == null) {
                return Response.serverError().build();
            }

            logger(servletContext).info(">>>> User session={}", userSession);
            return Response.noContent().build();
        }
    }

    public static class UserSessionTestServlet extends AbstractAppbaseSyncServlet {
        private static final long serialVersionUID = -2747001738625692588L;

        @Override
        protected int headImpl(
                final String traceId,
                final HttpServletRequest httpServletRequest,
                final HttpServletResponse httpServletResponse)
                throws Exception
        {
            final UserSession userSession = (UserSession)
                    httpServletRequest.getAttribute(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY);
            if (userSession == null) {
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }

            if (AppbaseHttpServletRequestWrapper.class.isAssignableFrom(httpServletRequest.getClass())) {
                final UserSession userSession1 =
                        ((AppbaseHttpServletRequestWrapper) httpServletRequest).getUserSession();
                if (userSession1 == null) {
                    return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                }
            }

            logger.info(">>>>> User session={}", userSession);
            return HttpServletResponse.SC_NO_CONTENT;
        }

        @Override
        protected boolean isHeadSupported() {
            return true;
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
        protected boolean isPutSupported() {
            return false;
        }

        @Override
        protected boolean isDeleteSupported() {
            return false;
        }
    }
}
