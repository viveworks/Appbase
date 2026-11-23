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
package com.viveworks.appbase.stacktest.test;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.StackTestRuntimeException;
import com.viveworks.appbase.stacktest.service.StackTestServiceException;
import java.util.Arrays;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractTest {
    protected final AppbaseServices appbaseServices;
    protected final Logger logger;
    protected final HttpClient httpClient;
    protected final CookieStore cookieStore;
    protected final String baseUrl;

    protected AbstractTest(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        AppbaseUtil.notNull(appbaseServices, "appbaseServices");
        AppbaseUtil.notNull(httpClient, "httpClient");
        AppbaseUtil.notNull(cookieStore, "cookieStore");
        AppbaseUtil.notBlank(baseUrl, "baseUrl");

        this.appbaseServices = appbaseServices;
        this.logger = appbaseServices.getCoreServices().appbaseCore().getLogService().getLogger(getClass());
        this.httpClient = httpClient;
        this.cookieStore = cookieStore;
        this.baseUrl = baseUrl;
    }

    protected <T> T httpGetText(
            final String uri,
            @Nullable
            final Class<T> responseType)  throws Exception
    {
        final HttpGet httpGet = new HttpGet();
        httpGet.setHeader(
                "Accept",
                "text/plain");
        httpGet.setURI(URI.create(uri));

        return httpGet(httpGet, responseType);
    }

    protected String httpGetJson(final String uri) throws Exception {
        final HttpGet httpGet = new HttpGet();
        httpGet.setHeader(
                "Accept",
                "application/json");
        httpGet.setURI(URI.create(uri));

        return httpGet(httpGet, String.class);
    }

    protected <T> T httpGetJson(
            final String uri,
            @Nullable
            final Class<T> responseType) throws Exception
    {
        final HttpGet httpGet = new HttpGet();
        httpGet.setHeader(
                "Accept",
                "application/json");
        httpGet.setURI(URI.create(uri));

        return httpGet(httpGet, responseType);
    }

    protected <T> T httpGet(
            final HttpGet httpGet,
            @Nullable
            final Class<T> responseType)  throws Exception
    {
        logger.info("Sending GET request [httpGet={}, responseType={}]", httpGet, responseType);

        return sendHttpRequest(httpGet, responseType);
    }

    protected HttpEntity httpGet(final String uri) throws Exception {
        logger.info("Sending GET request [uri={}]", uri);

        final HttpGet httpGet = new HttpGet();
        httpGet.setURI(URI.create(uri));

        return sendHttpRequest(httpGet);
    }

    protected HttpResponse httpGetResponse(final String uri, final Map<String, String> headers) throws Exception {
        logger.info("Sending GET request to [uri={}, headers={}]", uri, headers);

        final HttpGet httpGet = new HttpGet();
        httpGet.setHeader(
                "Accept",
                "application/json");
        if (headers != null) {
            headers.forEach(httpGet::addHeader);
        }
        httpGet.setURI(URI.create(uri));

        return httpClient.execute(httpGet);
    }

    protected <T> T httpPostText(
            final String uri,
            final String text,
            @Nullable
            final Class<T> responseType)  throws Exception
    {
        final HttpPost httpPost = new HttpPost();
        httpPost.setHeader(
                "Accept",
                "*/*");
        httpPost.setHeader(
                "Content-Type",
                "text/plain");
        httpPost.setURI(URI.create(uri));
        httpPost.setEntity(new StringEntity(text));

        return httpPost(httpPost, responseType);
    }

    protected <T> T httpPostJson(
            final String uri,
            final String json,
            @Nullable
            final Class<T> responseType)  throws Exception
    {
        final HttpPost httpPost = new HttpPost();
        httpPost.setHeader(
                "Accept",
                "*/*");
        httpPost.setHeader(
                "Content-Type",
                "application/json");
        httpPost.setURI(URI.create(uri));
        httpPost.setEntity(new StringEntity(json));

        return httpPost(httpPost, responseType);
    }

    protected <T> T httpPostEmpty(
            final String uri,
            @Nullable
            final Class<T> responseType)
            throws Exception
    {
        final HttpPost httpPost = new HttpPost();
        httpPost.setHeader(
                "Accept",
                "*/*");
        httpPost.setHeader(
                "Content-Type",
                "application/json");
        httpPost.setURI(URI.create(uri));
        httpPost.setEntity(new StringEntity(""));

        return httpPost(httpPost, responseType);
    }

    protected void httpPostEmpty(final String uri) throws Exception {
        final HttpPost httpPost = new HttpPost();
        httpPost.setHeader(
                "Accept",
                "*/*");
        httpPost.setHeader(
                "Content-Type",
                "application/json");
        httpPost.setURI(URI.create(uri));
        httpPost.setEntity(new StringEntity(""));
        httpPost(httpPost, null);
    }

    protected <T, V> T httpPost(
            final String uri,
            V requestEntity,
            Class<V> requestEntityType)  throws Exception
    {
        final HttpPost httpPost = new HttpPost();
        httpPost.setHeader(
                "Accept",
                "application/json");
        httpPost.setHeader(
                "Content-Type",
                "application/json");
        httpPost.setURI(URI.create(uri));

        final String json = serialize(requestEntityType, requestEntity);
        httpPost.setEntity(new StringEntity(json));

        return sendHttpRequest(httpPost, null);
    }

    protected <T, V> T httpPost(
            final String uri,
            V requestEntity,
            Class<V> requestEntityType,
            @Nullable
            final Class<T> responseType)  throws Exception
    {
        final HttpPost httpPost = new HttpPost();
        httpPost.setHeader(
                "Accept",
                "application/json");
        httpPost.setHeader(
                "Content-Type",
                "application/json");
        httpPost.setURI(URI.create(uri));

        final String json = serialize(requestEntityType, requestEntity);
        logger.info("Sending json: {}", json);
        httpPost.setEntity(new StringEntity(json));

        return sendHttpRequest(httpPost, responseType);
    }

    protected <T> T httpPost(
            final HttpPost httpPost,
            @Nullable
            final Class<T> responseType)  throws Exception
    {
        logger.info("Sending POST request [httpPost={}, responseType={}]", httpPost, responseType);

        return sendHttpRequest(httpPost, responseType);
    }

    protected <T, V> T httpPut(
            final String uri,
            V requestEntity,
            Class<V> requestEntityType,
            @Nullable
            final Class<T> responseType)  throws Exception
    {
        final HttpPut httpPut = new HttpPut();
        httpPut.setHeader(
                "Accept",
                "application/json");
        httpPut.setHeader(
                "Content-Type",
                "application/json");
        httpPut.setURI(URI.create(uri));

        final String json = serialize(requestEntityType, requestEntity);
        logger.info("Sending json: {}", json);
        httpPut.setEntity(new StringEntity(json));

        final HttpEntity entity = sendHttpRequest(httpPut);
        if (entity != null) {
            if (responseType != null) {
                return json(entity, responseType);
            }
        }

        return null;
    }

    protected <T> T httpPutJson(
            final String uri,
            final String json,
            @Nullable
            final Class<T> responseType)  throws Exception
    {
        logger.info("Sending PUT request to [uri={}, json={}, responseType={}]",
                uri, json, responseType);

        final HttpPut httpPut = new HttpPut();
        httpPut.setHeader(
                "Accept",
                "*/*");
        httpPut.setHeader(
                "Content-Type",
                "application/json");
        httpPut.setURI(URI.create(uri));
        httpPut.setEntity(new StringEntity(json));

        return httpPut(httpPut, responseType);
    }

    /** A single GET's status code plus response body, captured from ONE response. */
    public record StatusAndBody(int statusCode, String body) {}

    /**
     * GET {@code uri} once and return its status code and body from the SAME response, via a
     * {@code ResponseHandler} so the entity is consumed and the connection released on both success
     * and exception. This avoids the two-request / connection-retention pattern.
     */
    protected StatusAndBody httpGetStatusAndBody(final String uri) throws Exception {
        logger.info("Sending GET request to [uri={}]", uri);

        final HttpGet httpGet = new HttpGet(URI.create(uri));
        httpGet.setHeader("Accept", "application/json");
        return httpClient.execute(httpGet, response -> {
            final int statusCode = response.getStatusLine().getStatusCode();
            final HttpEntity entity = response.getEntity();
            return new StatusAndBody(statusCode, entity != null ? EntityUtils.toString(entity) : null);
        });
    }

    /**
     * HEAD {@code uri} and return the status code, via a {@code ResponseHandler} so the connection is
     * released on both success and exception. Lets a non-2xx (e.g. 405) be observed rather than thrown.
     */
    protected int httpCodeOfHead(final String uri) throws Exception {
        logger.info("Sending HEAD request to [uri={}]", uri);

        final HttpHead httpHead = new HttpHead(URI.create(uri));
        httpHead.setHeader("Accept", "*/*");
        return httpClient.execute(httpHead, response -> response.getStatusLine().getStatusCode());
    }

    protected int httpCodeOfPost(final String uri)  throws Exception {
        logger.info("Sending PUT request to [uri={}]", uri);

        final HttpPost httpPost = new HttpPost();
        httpPost.setHeader(
                "Accept",
                "*/*");
        httpPost.setHeader(
                "Content-Type",
                "application/json");
        httpPost.setURI(URI.create(uri));
        httpPost.setEntity(new StringEntity(""));

        final HttpResponse httpResponse = httpClient.execute(httpPost);
        return httpResponse.getStatusLine().getStatusCode();
    }

    protected int httpPutEmpty(final String uri)  throws Exception {
        logger.info("Sending PUT request to [uri={}]", uri);

        final HttpPut httpPut = new HttpPut();
        httpPut.setHeader(
                "Accept",
                "*/*");
        httpPut.setHeader(
                "Content-Type",
                "application/json");
        httpPut.setURI(URI.create(uri));
        httpPut.setEntity(new StringEntity(""));

        final HttpResponse httpResponse = httpClient.execute(httpPut);
        return httpResponse.getStatusLine().getStatusCode();
    }

    protected <T> int httpCodeOfPut(
            final String uri,
            final Class<T> requestType,
            final T request)
            throws Exception
    {
        logger.info("Sending PUT request to [uri={}]", uri);

        final HttpPut httpPut = new HttpPut();
        httpPut.setHeader(
                "Accept",
                "*/*");
        httpPut.setHeader(
                "Content-Type",
                "application/json");
        httpPut.setURI(URI.create(uri));
        final String json = serialize(requestType, request);
        logger.info("Sending json: {}", json);
        httpPut.setEntity(new StringEntity(json));

        final HttpResponse httpResponse = httpClient.execute(httpPut);
        return httpResponse.getStatusLine().getStatusCode();
    }

    protected <T> T httpPutEmpty(
            final String uri,
            @Nullable
            final Class<T> responseType)  throws Exception
    {
        logger.info("Sending PUT request to [uri={}, responseType={}]", uri, responseType);

        final HttpPut httpPut = new HttpPut();
        httpPut.setHeader(
                "Accept",
                "*/*");
        httpPut.setHeader(
                "Content-Type",
                "application/json");
        httpPut.setURI(URI.create(uri));
        httpPut.setEntity(new StringEntity(""));

        return httpPut(httpPut, responseType);
    }

    protected <T> T httpPut(
            final HttpPut httpPut,
            @Nullable
            final Class<T> responseType)  throws Exception
    {
        logger.info("Sending PUT request [httpPut={}, responseType={}]", httpPut, responseType);

        return sendHttpRequest(httpPut, responseType);
    }

    protected int httpHead(final String uri) throws Exception {
        logger.info("Sending HEAD request to [uri={}]", uri);

        return httpHead(uri, Map.of("Accept", "*/*"));
    }

    protected HttpResponse httpHeadResponse(final String uri) throws Exception {
        logger.info("Sending HEAD request to [uri={}]", uri);

        return httpHeadResponse(uri, Map.of("Accept", "*/*"));
    }

    protected int httpHead(final String uri, final Map<String, String> headers) throws Exception {
        logger.info("Sending HEAD request to [uri={}, headers={}]", uri, headers);

        final HttpResponse httpResponse = httpHeadResponse(uri, headers);
        return httpResponse.getStatusLine().getStatusCode();
    }

    protected HttpResponse httpHeadResponse(final String uri, final Map<String, String> headers) throws Exception {
        logger.info("Sending HEAD request to [uri={}, headers={}]", uri, headers);

        final HttpHead httpHead = new HttpHead();
        if (headers != null) {
            headers.forEach(httpHead::addHeader);
        }
        httpHead.setURI(URI.create(uri));

        return httpClient.execute(httpHead);
    }

    protected <T> T httpHead(
            final String uri,
            @Nullable
            final Class<T> responseType)  throws Exception
    {
        logger.info("Sending HEAD request to [uri={}]", uri);

        final HttpHead httpHead = new HttpHead();
        httpHead.setHeader(
                "Accept",
                "*/*");
        httpHead.setHeader(
                "Content-Type",
                "application/json");
        httpHead.setURI(URI.create(uri));

        return sendHttpRequest(httpHead, responseType);
    }

    protected void httpDelete(final String uri)  throws Exception {
        logger.info("Sending DELETE request to [uri={}]", uri);

        final HttpDelete httpDelete = new HttpDelete();
        httpDelete.setHeader(
                "Accept",
                "*/*");
        httpDelete.setURI(URI.create(uri));

        sendHttpRequest(httpDelete, null);
    }

    protected <T> T httpDelete(
            final String uri,
            @Nullable
            final Class<T> responseType)  throws Exception
    {
        logger.info("Sending DELETE request to [uri={}, responseType={}]",
                uri, responseType);

        final HttpDelete httpDelete = new HttpDelete();
        httpDelete.setHeader(
                "Accept",
                "*/*");
        httpDelete.setHeader(
                "Content-Type",
                "application/json");
        httpDelete.setURI(URI.create(uri));

        return sendHttpRequest(httpDelete, responseType);
    }

    protected <T> T sendHttpRequest(
            final HttpUriRequest httpUriRequest,
            @Nullable
            final Class<T> responseType)
            throws Exception
    {
        final HttpEntity entity = sendHttpRequest(httpUriRequest);
        if (entity != null) {
            if (responseType != null) {
                return json(entity, responseType);
            }
        }

        return null;
    }

    protected HttpEntity sendHttpRequest(final HttpUriRequest httpUriRequest) throws Exception {
        final HttpResponse httpResponse = httpClient.execute(httpUriRequest);
        final int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode > 299 || statusCode < 200) {
            throw new StackTestServiceException("Failed to send request");
        }

        return httpResponse.getEntity();
    }

    protected HttpResponse executeHttpGet(final String url) throws Exception {
        logger.info("Sending GET request to [uri={}]", url);

        final HttpGet httpGet = new HttpGet();
        httpGet.setHeader(
                "Accept",
                "application/json");
        httpGet.setURI(URI.create(url));

        return executeHttpRequest(httpGet);
    }

    protected HttpResponse executeHttpRequest(final HttpUriRequest httpUriRequest) throws Exception {
        logger.info("Sending HTTP request to [uri={}]", httpUriRequest.getURI().toString());

        return httpClient.execute(httpUriRequest);
    }

    protected BasicClientCookie addCookie(final String name, final String value) {
        AppbaseUtil.notBlank(name, "name");
        AppbaseUtil.notBlank(value, "value");

        final BasicClientCookie cookie = new BasicClientCookie(name, value);
        cookie.setSecure(false);
        cookie.setDomain(StackTestConstants.LOCALHOST);
        cookieStore.addCookie(cookie);

        return cookie;
    }

    protected void clearCookies(final BasicClientCookie cookie) {
        cookie.setExpiryDate(Date.from(Instant.now().minusSeconds(10)));
        cookieStore.clearExpired(Date.from(Instant.now()));
    }

    @SuppressWarnings("unchecked")
    protected <T> T json(final HttpEntity entity, final Class<T> responseType) throws Exception {
        final InputStream content = entity.getContent();

        if (responseType.isAssignableFrom(String.class)) {
            final String str = new String(content.readAllBytes());
            logger.info("HTTP String Response: {}", str);
            return (T) str;
        }

        final T response = deserialize(responseType, content);
        logger.info("HTTP Response: {}", response);
        return response;
    }

    protected <T> String serialize(
            final Class<T> type,
            final T object)
            throws IOException
    {
        return JsonUtil.getJsonMapper().writerFor(type).writeValueAsString(object);
    }

    @SuppressWarnings("unchecked")
    protected <T> T deserialize(final Class<T> type, final InputStream stream) throws IOException {
        return (T) JsonUtil.getJsonMapper().readerFor(type).readValue(stream);
    }

    @SuppressWarnings("unchecked")
    protected <T> T deserialize(final Class<T> type, final String json) throws IOException {
        return (T) JsonUtil.getJsonMapper().readerFor(type).readValue(json);
    }

    protected void checkNull(
            final Object value,
            final String testName,
            final String valueName)
    {
        if (value != null) {
            throwStackTestRuntimeException(testName, valueName + " must not be null");
        }
    }

    protected void checkNotNull(
            final Object value,
            final String testName,
            final String valueName)
    {
        if (value == null) {
            throwStackTestRuntimeException(testName, valueName + " must not be null");
        }
    }

    protected void checkNotBlank(
            final String value,
            final String testName,
            final String valueName)
    {
        if (AppbaseUtil.isBlank(value)) {
            throwStackTestRuntimeException(testName, valueName + " must not be blank");
        }
    }

    protected <T> void checkNotEquals(
            final T unexpected,
            final T actual,
            final String testName,
            final String valueName)
    {
        if (actual.equals(unexpected)) {
            throwStackTestRuntimeException(
                    testName,
                    valueName + ": actual value=" + actual + " must not be equal to unexpected=" + unexpected);
        }
    }

    protected <T> void checkEquals(
            final T expected,
            final T actual,
            final String testName,
            final String valueName)
    {
        if (!actual.equals(expected)) {
            throwStackTestRuntimeException(
                    testName,
                    valueName + ": actual value=" + actual + " not equal to expected=" + expected);
        }
    }

    protected void checkFalse(
            final Boolean actual,
            final String testName,
            final String valueName)
    {
        if (actual == null || actual) {
            throwStackTestRuntimeException(testName, valueName + ": actual value=" + actual + " is not false");
        }
    }

    protected void checkTrue(
            final Boolean actual,
            final String testName,
            final String valueName)
    {
        if (actual == null || !actual) {
            throwStackTestRuntimeException(testName, valueName + ": actual value=" + actual + " is not true");
        }
    }

    protected void checkTestType(final Class<?> actualClass, final Class<?>... supportedClasses) {
        AppbaseUtil.notNull(actualClass, "actualClass");
        AppbaseUtil.notNull(supportedClasses, "supportedClasses");

        for (final Class<?> c : supportedClasses) {
            if (c.isAssignableFrom(actualClass) || actualClass.isAssignableFrom(c)) {
                return;
            }
        }

        throw new StackTestRuntimeException(
                "[Class=" + getClass().getName() +
                        ", Message=Class " + actualClass + " is not supported: " + Arrays.asList(supportedClasses) + "]");
    }

    protected void throwStackTestRuntimeException(final String testName, final String message) {
        throw new StackTestRuntimeException(
                "[Class=" + getClass().getName() + ", Test=" + testName + ", Message=" + message + "]");
    }

    protected void startTest(final Class<?> testType, final String testName) {
        logger.info("Running test: [type={}, name={}]", testType.getSimpleName(), testName);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppbaseRuntimeException(e);
        }
    }

    protected void startTest(final String testName) {
        logger.info("Running test: {}", testName);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppbaseRuntimeException(e);
        }
    }

    protected void endTest(final Class<?> testType, final String testName) {
        logger.info("Done test: [type={}, name={}]", testType.getSimpleName(), testName);
    }

    protected void endTest(final String testName) {
        logger.info("Done test: {}", testName);
    }

    protected void startChildTest(final Class<?> testType, final String testName) {
        logger.info("Running child test: [type={}, name={}]", testType.getSimpleName(), testName);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppbaseRuntimeException(e);
        }
    }

    protected void endChildTest(final Class<?> testType, final String testName) {
        logger.info("Done test: [type={}, name={}]", testType.getSimpleName(), testName);
    }
}
