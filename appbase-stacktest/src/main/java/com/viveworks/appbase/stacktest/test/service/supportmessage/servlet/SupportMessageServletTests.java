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

package com.viveworks.appbase.stacktest.test.service.supportmessage.servlet;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class SupportMessageServletTests extends AbstractServletTest {
    private final ReadSupportMessagesServletTest readSupportMessagesServletTest;
    private final ReadSupportMessageByIdServletTest readSupportMessageByIdServletTest;
    private final SendSupportMessageServletTest sendSupportMessageServletTest;
    private final SupportMessageByIdServletTest supportMessageByIdServletTest;
    private final UpdateMessageServletTest updateMessageServletTest;
    private final UpdateStatusServletTest updateStatusServletTest;
    private final DeleteSupportMessageByIdServletTest deleteSupportMessageByIdServletTest;

    public SupportMessageServletTests(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        this.readSupportMessagesServletTest = new ReadSupportMessagesServletTest(
                appbaseServices, httpClient, cookieStore, baseUrl);
        this.readSupportMessageByIdServletTest = new ReadSupportMessageByIdServletTest(
                appbaseServices, httpClient, cookieStore, baseUrl);
        this.sendSupportMessageServletTest = new SendSupportMessageServletTest(
                appbaseServices, httpClient, cookieStore, baseUrl);
        this.supportMessageByIdServletTest = new SupportMessageByIdServletTest(
                appbaseServices, httpClient, cookieStore, baseUrl);
        this.updateMessageServletTest = new UpdateMessageServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.updateStatusServletTest = new UpdateStatusServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.deleteSupportMessageByIdServletTest = new DeleteSupportMessageByIdServletTest(
                appbaseServices, httpClient, cookieStore, baseUrl);
    }

    @Override
    protected void runServletTestsImpl() throws Exception {
        readSupportMessagesServletTest.runServletTests();
        readSupportMessageByIdServletTest.runServletTests();
        sendSupportMessageServletTest.runServletTests();
        supportMessageByIdServletTest.runServletTests();
        updateMessageServletTest.runServletTests();
        updateStatusServletTest.runServletTests();
        deleteSupportMessageByIdServletTest.runServletTests();
    }
}
