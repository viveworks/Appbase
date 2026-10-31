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

package com.viveworks.appbase.stacktest.test.service.authorization.testimpl;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.Value;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.user.account.UserAccountServiceException;
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordResponse;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.test.service.authorization.jaxrs.AuthorizationJaxrsFilterTest;
import com.viveworks.appbase.stacktest.test.service.authorization.servlet.AuthorizationServletFilterTest;
import com.viveworks.appbase.stacktest.test.service.user.account.AbstractUserAccountApiTest;
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

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AuthorizationFilterTestImpl extends AbstractUserAccountApiTest {
    private final Class<?> testType;

    public AuthorizationFilterTestImpl(
            final Class<?> testType,
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        this.testType = testType;
        checkTestType(testType, AuthorizationJaxrsFilterTest.class, AuthorizationServletFilterTest.class);
    }

    public void filterTestImpl() throws Exception {
        final String testName = "filterTestImpl";

        startTest(testType, testName);

        final AuthorizationService authorizationService = appbaseServices.get(AuthorizationService.class);
        authorizationService.addAllowedServiceName(AppbaseServiceConstants.WILDCARD);

        authorizationService.addAllowedService(AuthorizedResourceTest.class);
        authorizationService.addAllowedServiceOperation(AuthorizedResourceTest.class, "ping");
        authorizationService.addAllowedServiceName(StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_PATH);
        authorizationService.addAllowedServiceName(StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_PATH);
        authorizationService.addAllowedServiceNameOperation(
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_PATH, "HEAD");

        authorizationService.addAllowedService(AuthorizedResourceUnauthorizedMethodTest.class);
        authorizationService.addAllowedServiceName(
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_UNAUTHORIZED_METHOD_PATH);
        authorizationService.addAllowedServiceName(
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_UNAUTHORIZED_METHOD_PATH);
        authorizationService.addAllowedServiceNameOperation(
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_UNAUTHORIZED_METHOD_PATH, "GET");

        authorizationService.addBlockedServiceOperation(
                AuthorizedResourceUnauthorizedMethodTest.class, "ping1");
        authorizationService.addBlockedServiceNameOperation(
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_UNAUTHORIZED_METHOD_PATH, "ping1");
        authorizationService.addBlockedServiceNameOperation(
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_UNAUTHORIZED_METHOD_PATH, "HEAD");
        authorizationService.addBlockedServiceNameOperation(
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_UNAUTHORIZED_METHOD_PATH, "HEAD");

        authorizationService.addBlockedService(UnauthorizedResourceTest.class);
        authorizationService.addBlockedServiceName(StackTestConstants.AUTHORIZATION_FILTER_UNAUTHORIZED_RESOURCE_PATH);
        authorizationService.addBlockedServiceName(StackTestConstants.AUTHORIZATION_FILTER_UNAUTHORIZED_SERVLET_PATH);

        authorizationService.addAllowedServiceOperation(AuthorizedResourceMethodTest.class, "ping");
        authorizationService.addAllowedServiceNameOperation(
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_METHOD_PATH, "ping");
        authorizationService.addAllowedServiceNameOperation(
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_METHOD_PATH, "HEAD");
        authorizationService.addAllowedServiceNameOperation(
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_METHOD_PATH, "HEAD");

        if (isRestApiTest()) {
            authorizedResourceTest();
            unauthorizedResourceTest();
            authorizedResourceUnauthorizedMethodTest();
            authorizedResourceMethodTest();
        } else {
            authorizedServletTest();
            unauthorizedServletTest();
            authorizedServletUnauthorizedMethodTest();
            authorizedServletMethodTest();
        }

        endTest(testType, testName);
    }

    private void authorizedResourceTest() throws Exception {
        startTest(testType, "authorizedResourceTest");

        final String url = baseUrl + StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_PATH;
        allTestsWithNewSession(url, true);

        endTest(testType, "authorizedResourceTest");
    }

    private void unauthorizedResourceTest() throws Exception {
        startTest(testType, "unauthorizedResourceTest");

        final String url = baseUrl + StackTestConstants.AUTHORIZATION_FILTER_UNAUTHORIZED_RESOURCE_PATH;
        allTestsWithNewSession(url, false);

        endTest(testType, "unauthorizedResourceTest");
    }

    private void authorizedResourceUnauthorizedMethodTest() throws Exception {
        startTest(testType, "authorizedResourceUnauthorizedMethodTest");

        final String url =
                baseUrl + StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_UNAUTHORIZED_METHOD_PATH;
        allTestsWithNewSession(url, false);
        allTestsWithNewSession(url + "/ping2", true, false);

        endTest(testType, "authorizedResourceUnauthorizedMethodTest");
    }

    private void authorizedResourceMethodTest() throws Exception {
        startTest(testType, "authorizedResourceMethodTest");

        final String url = baseUrl + StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_METHOD_PATH;
        allTestsWithNewSession(url, true);

        endTest(testType, "authorizedResourceMethodTest");
    }

    private void authorizedServletTest() throws Exception {
        startTest(testType, "authorizedServletTest");

        final String url = baseUrl + StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_PATH;
        allTestsWithNewSession(url, true);

        endTest(testType, "authorizedServletTest");
    }

    private void unauthorizedServletTest() throws Exception {
        startTest(testType, "unauthorizedServletTest");

        final String url = baseUrl + StackTestConstants.AUTHORIZATION_FILTER_UNAUTHORIZED_SERVLET_PATH;
        allTestsWithNewSession(url, false);

        endTest(testType, "unauthorizedServletTest");
    }

    private void authorizedServletUnauthorizedMethodTest() throws Exception {
        startTest(testType, "authorizedServletUnauthorizedMethodTest");

        final String url =
                baseUrl + StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_UNAUTHORIZED_METHOD_PATH;
        allTestsWithNewSession(url, false);

        endTest(testType, "authorizedServletUnauthorizedMethodTest");
    }

    private void authorizedServletMethodTest() throws Exception {
        startTest(testType, "authorizedServletMethodTest");

        final String url = baseUrl + StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_METHOD_PATH;
        allTestsWithNewSession(url, true);

        endTest(testType, "authorizedServletMethodTest");
    }

    private void allTestsWithNewSession(final String url, final boolean expectAuthorized) throws Exception {
        allTestsWithNewSession(url, expectAuthorized, true);
    }

    private void allTestsWithNewSession(
            final String url,
            final boolean expectAuthorized,
            final boolean expectQueryCredentialRejection) throws Exception
    {
        String appbaseUserId = null;
        UserSession userSession = null;
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    createUserWithEmailPassword();
            userSession = afterIdpCreateUserWithEmailPasswordResponse.getUserSession();
            final UserAccount userAccount = userSession.getUserAccount();
            appbaseUserId = userAccount.getAppbaseUserId();

            allTests(
                    url,
                    userSession.getAppbaseSessionId(),
                    appbaseUserId,
                    appbaseUserId,
                    userSession.getIdpUserToken(),
                    expectAuthorized,
                    expectQueryCredentialRejection);
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
    }

    private void allTests(
            final String url,
            final String appbaseSessionId,
            final String appbaseUserId,
            final String appbaseRequesterId,
            final String idpUserToken,
            final boolean expectAuthorized,
            final boolean expectQueryCredentialRejection)
            throws Exception
    {
        urlTest(url, expectAuthorized);
        cookieTest(url, appbaseSessionId, appbaseUserId, appbaseRequesterId, idpUserToken, expectAuthorized);
        headerTest(url, appbaseSessionId, appbaseUserId, appbaseRequesterId, idpUserToken, expectAuthorized);
        queryParamTest(
                url,
                appbaseSessionId,
                appbaseUserId,
                appbaseRequesterId,
                expectAuthorized,
                expectQueryCredentialRejection);
    }

    private void urlTest(final String url, final boolean expectAuthorized) throws Exception {
        httpHeadTest(url, expectAuthorized);
    }

    private void cookieTest(
            final String url,
            @Nullable
            final String appbaseSessionId,
            @Nullable
            final String appbaseUserId,
            @Nullable
            final String appbaseRequesterId,
            @Nullable
            final String idpUserToken,
            final boolean expectAuthorized)
            throws Exception
    {
        BasicClientCookie cookie1 = null;
        BasicClientCookie cookie2 = null;
        BasicClientCookie cookie3 = null;
        BasicClientCookie cookie4 = null;

        if (appbaseSessionId != null) {
            cookie1 = addCookie(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, appbaseSessionId);
        }

        if (appbaseUserId != null) {
            cookie2 = addCookie(AppbaseServiceConstants.APPBASE_USER_ID_PARAM, appbaseUserId);
        }

        if (appbaseRequesterId != null) {
            cookie3 = addCookie(AppbaseServiceConstants.APPBASE_REQUESTER_ID_PARAM, appbaseRequesterId);
        }

        if (idpUserToken != null) {
            cookie4 = addCookie(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, idpUserToken);
        }

        try {
            httpHeadTest(url, expectAuthorized);
        } finally {
            if (cookie1 != null) {
                clearCookies(cookie1);
            }

            if (cookie2 != null) {
                clearCookies(cookie2);
            }

            if (cookie3 != null) {
                clearCookies(cookie3);
            }

            if (cookie4 != null) {
                clearCookies(cookie4);
            }
        }
    }

    private void headerTest(
            final String url,
            @Nullable
            final String appbaseSessionId,
            @Nullable
            final String appbaseUserId,
            @Nullable
            final String appbaseRequesterId,
            @Nullable
            final String idpUserToken,
            final boolean expectAuthorized)
            throws Exception
    {
        final Map<String, String> headers = new HashMap<>();

        if (appbaseSessionId != null) {
            headers.put(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, appbaseSessionId);
        }

        if (appbaseUserId != null) {
            headers.put(AppbaseServiceConstants.APPBASE_USER_ID_PARAM, appbaseUserId);
        }

        if (appbaseRequesterId != null) {
            headers.put(AppbaseServiceConstants.APPBASE_REQUESTER_ID_PARAM, appbaseRequesterId);
        }

        if (idpUserToken != null) {
            headers.put(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, idpUserToken);
        }

        httpHeadTest(url, headers, expectAuthorized);
    }

    private void queryParamTest(
            final String url,
            @Nullable
            final String appbaseSessionId,
            @Nullable
            final String appbaseUserId,
            @Nullable
            final String appbaseRequesterId,
            final boolean expectAuthorized,
            final boolean expectQueryCredentialRejection)
            throws Exception
    {
        final String finalUrl;
        if (appbaseSessionId != null && appbaseUserId != null && appbaseRequesterId != null) {
            finalUrl = url +
                    "?" + AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM + "=" + appbaseSessionId +
                    "&" + AppbaseServiceConstants.APPBASE_USER_ID_PARAM + "=" + appbaseUserId +
                    "&" + AppbaseServiceConstants.APPBASE_REQUESTER_ID_PARAM + "=" + appbaseRequesterId;
        } else if (appbaseSessionId != null && appbaseUserId != null) {
            finalUrl = url + "?" + AppbaseServiceConstants.APPBASE_USER_ID_PARAM + "=" + appbaseUserId +
                    "&" + AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM + "=" + appbaseSessionId;
        } else if (appbaseRequesterId != null) {
            finalUrl = url + "?" + AppbaseServiceConstants.APPBASE_REQUESTER_ID_PARAM + "=" + appbaseRequesterId;
        } else {
            finalUrl = url;
        }

        if (expectQueryCredentialRejection && (appbaseSessionId != null || appbaseUserId != null)) {
            httpHeadStatusTest(finalUrl, HttpServletResponse.SC_BAD_REQUEST);
        } else {
            httpHeadTest(finalUrl, expectAuthorized);
        }
    }

    private void httpHeadTest(final String url, final boolean expectAuthorized) throws Exception {
        final HttpHead httpHead = new HttpHead();
        httpHead.setURI(URI.create(url));
        httpHeadTest(httpHead, expectAuthorized);
    }

    private void httpHeadStatusTest(final String url, final int expectedStatusCode) throws Exception {
        final HttpHead httpHead = new HttpHead();
        httpHead.setURI(URI.create(url));
        final HttpResponse httpResponse = executeHttpRequest(httpHead);
        checkEquals(
                expectedStatusCode,
                httpResponse.getStatusLine().getStatusCode(),
                "httpHeadStatusTest",
                "statusCode");
    }

    private void httpHeadTest(
            final String url,
            final Map<String, String> headers,
            final boolean expectAuthorized)
            throws Exception
    {
        final HttpHead httpHead = new HttpHead();
        if (headers != null) {
            headers.forEach(httpHead::addHeader);
        }
        httpHead.setURI(URI.create(url));
        httpHeadTest(httpHead, expectAuthorized);
    }

    private void httpHeadTest(
            final HttpHead httpHead,
            final boolean expectAuthorized)
            throws Exception
    {
        final HttpResponse httpResponse = executeHttpRequest(httpHead);
        final int statusCode = httpResponse.getStatusLine().getStatusCode();
        checkStatusCode(statusCode, expectAuthorized, "httpHeadTest");
    }

    private void checkStatusCode(final int statusCode, final boolean expectAuthorized, final String testName) {
        if (expectAuthorized) {
            checkEquals(HttpServletResponse.SC_NO_CONTENT, statusCode, testName, "statusCode");
        } else {
            checkEquals(HttpServletResponse.SC_FORBIDDEN, statusCode, testName, "statusCode");
        }
    }

    private boolean isRestApiTest() {
        return AuthorizationJaxrsFilterTest.class.isAssignableFrom(testType);
    }

    @Path(StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_PATH)
    public static class AuthorizedResourceTest extends AbstractAppbaseJaxrsEntity {
        @HEAD
        public Response ping() {
            return Response.noContent().build();
        }
    }

    public static class AuthorizedServletTest extends AbstractAppbaseSyncServlet {
        private static final long serialVersionUID = -2507143240359141287L;

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

    @Path(StackTestConstants.AUTHORIZATION_FILTER_UNAUTHORIZED_RESOURCE_PATH)
    public static class UnauthorizedResourceTest extends AbstractAppbaseJaxrsEntity {
        @HEAD
        public Response ping() {
            return Response.noContent().build();
        }
    }

    public static class UnauthorizedServletTest extends AbstractAppbaseSyncServlet {
        private static final long serialVersionUID = 8378461445899900941L;

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

    @Path(StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_UNAUTHORIZED_METHOD_PATH)
    public static class AuthorizedResourceUnauthorizedMethodTest extends AbstractAppbaseJaxrsEntity {
        @HEAD
        public Response ping1() {
            return Response.noContent().build();
        }

        @HEAD
        @Path("/ping2")
        public Response ping2() {
            return Response.noContent().build();
        }
    }

    public static class AuthorizedServletUnauthorizedMethodTest extends AbstractAppbaseSyncServlet {
        private static final long serialVersionUID = -7876168866614037042L;

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
        @SuppressWarnings("unchecked")
        protected <T> T getImpl(
                final String traceId,
                final HttpServletRequest httpServletRequest,
                final HttpServletResponse httpServletResponse)
                throws Exception
        {
            return (T) new Value(true);
        }

        @Override
        protected boolean isGetSupported() {
            return true;
        }

        @Override
        protected boolean isGetResponseAutoHandled() {
            return false;
        }

        @Override
        protected void handleGetResponse(
                final Object response,
                final HttpServletRequest httpServletRequest,
                final HttpServletResponse httpServletResponse)
                throws IOException
        {
            httpServletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
            httpServletResponse.flushBuffer();
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

    @Path(StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_METHOD_PATH)
    public static class AuthorizedResourceMethodTest extends AbstractAppbaseJaxrsEntity {
        @HEAD
        public Response ping() {
            return Response.noContent().build();
        }
    }

    public static class AuthorizedServletMethodTest extends AbstractAppbaseSyncServlet {
        private static final long serialVersionUID = 2911330624258511152L;

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
