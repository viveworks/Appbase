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

package com.viveworks.appbase.stacktest.test.service.user.session.testImpl;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.user.account.UserAccountServiceException;
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordResponse;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.test.service.user.account.AbstractUserAccountApiTest;
import com.viveworks.appbase.stacktest.test.service.user.session.jaxrs.IdpUserTokenCheckJaxrsRequestFilterTest;
import com.viveworks.appbase.stacktest.test.service.user.session.servlet.IdpUserTokenCheckServletFilterTest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class IdpUserTokenCheckFilterTestImpl extends AbstractUserAccountApiTest {
    private final Class<?> testType;

    public IdpUserTokenCheckFilterTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        this.testType = testType;
        checkTestType(
                testType,
                IdpUserTokenCheckJaxrsRequestFilterTest.class,
                IdpUserTokenCheckServletFilterTest.class);
    }

    // Depends on UserSessionFilters, i.e. UserSession being set by request filters
    // Set AppbaseServiceConstants.IDP_USER_TOKEN_PARAM cookie, header and query param
    // Success: If userSession.idpUserToken matches IDP_USER_TOKEN_PARAM
    // Failure: Uses IdpUserTokenCheckFilterConfiguration and responds as configured
    public void filterTestImpl() throws Exception {
        final String testName = "filterTestImpl";

        startTest(testType, testName);

        final String url;
        if (isRestApiTest()) {
            url = baseUrl + StackTestConstants.IDP_USER_TOKEN_CHECK_FILTER_TEST_RESOURCE_PATH;
        } else {
            url = baseUrl + StackTestConstants.IDP_USER_TOKEN_CHECK_FILTER_TEST_SERVLET_PATH;
        }

        matchIdpUserTokenTest(url);
        invalidIdpUserTokenTest(url);
        noSessionIdpUserTokenTest(url);

        endTest(testType, testName);
    }

    private void matchIdpUserTokenTest(final String url) throws Exception {
        startTest(getClass(), "matchIdpUserTokenTest");

        String appbaseUserId = null;
        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            final UserAccount userAccount = userSession.getUserAccount();
            appbaseUserId = userAccount.getAppbaseUserId();

            matchIdpUserTokenCookieTest(url, userSession, appbaseUserId);
            matchIdpUserTokenHeaderTest(url, userSession, appbaseUserId);
            matchIdpUserTokenQueryParamTest(url, userSession, appbaseUserId);
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

        endTest(getClass(), "matchIdpUserTokenTest");
    }

    private void matchIdpUserTokenCookieTest(
            final String url,
            final UserSession userSession,
            final String appbaseUserId) throws Exception
    {
        startTest(getClass(), "matchIdpUserTokenCookieTest");

        final BasicClientCookie cookie1 =
                addCookie(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, userSession.getIdpUserToken());
        final BasicClientCookie cookie2 =
                addCookie(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, userSession.getAppbaseSessionId());
        final BasicClientCookie cookie3 =
                addCookie(AppbaseServiceConstants.APPBASE_USER_ID_PARAM, appbaseUserId);

        try {
            executeIdpUserTokenCheckRequest(url, "matchIdpUserTokenCookieTest", true);
        } finally {
            clearCookies(cookie1);
            clearCookies(cookie2);
            clearCookies(cookie3);
        }
    }

    private void matchIdpUserTokenHeaderTest(
            final String url,
            final UserSession userSession,
            final String appbaseUserId) throws Exception
    {
        startTest(getClass(), "matchIdpUserTokenHeaderTest");

        final HttpResponse httpResponse = httpHeadResponse(
                url,
                Map.of(
                        AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, userSession.getIdpUserToken(),
                        AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, userSession.getAppbaseSessionId(),
                        AppbaseServiceConstants.APPBASE_USER_ID_PARAM, appbaseUserId));
        final int statusCode = httpResponse.getStatusLine().getStatusCode();
        checkSuccessStatusCode(statusCode, "matchIdpUserTokenHeaderTest");

        endTest(getClass(), "matchIdpUserTokenHeaderTest");
    }

    private void matchIdpUserTokenQueryParamTest(
            final String url,
            final UserSession userSession,
            final String appbaseUserId) throws Exception
    {
        startTest(getClass(), "matchIdpUserTokenQueryParamTest");

        final String finalUrl = url +
                "?" + AppbaseServiceConstants.IDP_USER_TOKEN_PARAM + "=" + userSession.getIdpUserToken() +
                "&" + AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM + "=" + userSession.getAppbaseSessionId() +
                "&" + AppbaseServiceConstants.APPBASE_USER_ID_PARAM + "=" + appbaseUserId;
        final int statusCode = httpHead(finalUrl);
        checkQueryIdentityRejectedStatusCode(statusCode, "matchIdpUserTokenQueryParamTest");

        endTest(getClass(), "matchIdpUserTokenQueryParamTest");
    }

    private void invalidIdpUserTokenTest(final String url) throws Exception {
        startTest(getClass(), "randomIdpUserTokenTest");

        String appbaseUserId = null;
        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            final UserAccount userAccount = userSession.getUserAccount();
            appbaseUserId = userAccount.getAppbaseUserId();

            final String invalidIdpUserToken = UUID.randomUUID().toString();
            invalidIdpUserTokenCookieTest(url, invalidIdpUserToken, userSession, appbaseUserId);
            invalidIdpUserTokenHeaderTest(url, invalidIdpUserToken, userSession, appbaseUserId);
            invalidIdpUserTokenQueryParamTest(url, invalidIdpUserToken, userSession, appbaseUserId);
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

        endTest(getClass(), "randomIdpUserTokenTest");
    }

    private void invalidIdpUserTokenCookieTest(
            final String url,
            final String invalidIdpUserToken,
            final UserSession userSession,
            final String appbaseUserId) throws Exception
    {
        startTest(getClass(), "invalidIdpUserTokenCookieTest");

        final BasicClientCookie cookie1 =
                addCookie(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, invalidIdpUserToken);
        final BasicClientCookie cookie2 =
                addCookie(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, userSession.getAppbaseSessionId());
        final BasicClientCookie cookie3 =
                addCookie(AppbaseServiceConstants.APPBASE_USER_ID_PARAM, appbaseUserId);

        try {
            executeIdpUserTokenCheckRequest(url, "invalidIdpUserTokenCookieTest", false);
        } finally {
            clearCookies(cookie1);
            clearCookies(cookie2);
            clearCookies(cookie3);
        }

        endTest(getClass(), "invalidIdpUserTokenCookieTest");
    }

    private void invalidIdpUserTokenHeaderTest(
            final String url,
            final String invalidIdpUserToken,
            final UserSession userSession,
            final String appbaseUserId) throws Exception
    {
        startTest(getClass(), "invalidIdpUserTokenHeaderTest");

        final HttpResponse httpResponse = httpHeadResponse(
                url,
                Map.of(
                        AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, invalidIdpUserToken,
                        AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, userSession.getAppbaseSessionId(),
                        AppbaseServiceConstants.APPBASE_USER_ID_PARAM, appbaseUserId));
        final int statusCode = httpResponse.getStatusLine().getStatusCode();
        checkFailedStatusCode(statusCode, "invalidIdpUserTokenHeaderTest");

        endTest(getClass(), "invalidIdpUserTokenHeaderTest");
    }

    private void invalidIdpUserTokenQueryParamTest(
            final String url,
            final String invalidIdpUserToken,
            final UserSession userSession,
            final String appbaseUserId) throws Exception
    {
        startTest(getClass(), "invalidIdpUserTokenQueryParamTest");

        final String finalUrl = url +
                "?" + AppbaseServiceConstants.IDP_USER_TOKEN_PARAM + "=" + invalidIdpUserToken +
                "&" + AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM + "=" + userSession.getAppbaseSessionId() +
                "&" + AppbaseServiceConstants.APPBASE_USER_ID_PARAM + "=" + appbaseUserId;
        final int statusCode = httpHead(finalUrl);
        checkQueryIdentityRejectedStatusCode(statusCode, "invalidIdpUserTokenQueryParamTest");

        endTest(getClass(), "invalidIdpUserTokenQueryParamTest");
    }

    private void noSessionIdpUserTokenTest(final String url) throws Exception {
        startTest(getClass(), "noSessionTest");

        final String randomIdpUserToken = UUID.randomUUID().toString();
        noSessionIdpUserTokenCookieTest(url, randomIdpUserToken);
        noSessionIdpUserTokenHeaderTest(url, randomIdpUserToken);
        noSessionIdpUserTokenQueryParamTest(url, randomIdpUserToken);

        endTest(getClass(), "noSessionTest");
    }

    private void noSessionIdpUserTokenCookieTest(final String url, final String randomIdpUserToken) throws Exception {
        final BasicClientCookie cookie =
                addCookie(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, randomIdpUserToken);
        try {
            executeIdpUserTokenCheckRequest(url, "noSessionIdpUserTokenCookieTest", true);
        } finally {
            clearCookies(cookie);
        }
    }

    private void noSessionIdpUserTokenHeaderTest(final String url, final String randomIdpUserToken) throws Exception {
        final HttpResponse httpResponse = httpHeadResponse(
                url, Map.of(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, randomIdpUserToken));
        final int statusCode = httpResponse.getStatusLine().getStatusCode();
        checkSuccessStatusCode(statusCode, "noSessionIdpUserTokenHeaderTest");
    }

    private void noSessionIdpUserTokenQueryParamTest(
            final String url,
            final String randomIdpUserToken) throws Exception
    {
        final String finalUrl = url + "?" + AppbaseServiceConstants.IDP_USER_TOKEN_PARAM + "=" + randomIdpUserToken;
        executeIdpUserTokenCheckRequest(finalUrl, "noSessionIdpUserTokenQueryParamTest", true);
    }

    private boolean isRestApiTest() {
        return IdpUserTokenCheckJaxrsRequestFilterTest.class.isAssignableFrom(testType);
    }

    private void executeIdpUserTokenCheckRequest(
            final String url,
            final String testName,
            final boolean expectedSuccess) throws Exception
    {
        final HttpHead httpHead = new HttpHead();
        httpHead.setURI(URI.create(url));
        final int statusCode = httpHead(url);
        if (expectedSuccess) {
            checkSuccessStatusCode(statusCode, testName);
        }
    }

    private void checkSuccessStatusCode(final int statusCode, final String testName) {
        checkEquals(HttpServletResponse.SC_NO_CONTENT, statusCode, testName, "success.statusCode");
    }

    private void checkFailedStatusCode(final int statusCode, final String testName) {
        checkEquals(HttpServletResponse.SC_FORBIDDEN, statusCode, testName, "failed.statusCode");
    }

    private void checkQueryIdentityRejectedStatusCode(final int statusCode, final String testName) {
        checkEquals(HttpServletResponse.SC_BAD_REQUEST, statusCode, testName, "queryIdentity.statusCode");
    }

    @Path(StackTestConstants.IDP_USER_TOKEN_CHECK_FILTER_TEST_RESOURCE_PATH)
    public static class IdpUserTokenCheckTestResource extends AbstractAppbaseJaxrsEntity {
        @HEAD
        public Response ping() {
            return Response.noContent().build();
        }
    }

    public static class IdpUserTokenCheckTestServlet extends AbstractAppbaseSyncServlet {
        private static final long serialVersionUID = 7602044345510435090L;

        @Override
        protected int headImpl(
                final String traceId,
                final HttpServletRequest httpServletRequest,
                final HttpServletResponse httpServletResponse)
                throws Exception
        {
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
