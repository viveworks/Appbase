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

package com.viveworks.appbase.stacktest.test.service.user.account.servlet;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.stacktest.test.AbstractServletTest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class UserAccountServletTests extends AbstractServletTest {
    private final AfterIdpCreateUserWithEmailPasswordServletTest afterIdpCreateUserWithEmailPasswordServletTest;
    private final AfterIdpEmailPasswordSignInServletTest afterIdpEmailPasswordSignInServletTest;
    private final AfterIdpGuestSignInServletTest afterIdpGuestSignInServletTest;
    private final AfterIdpPasswordResetSubmitServletTest afterIdpPasswordResetSubmitServletTest;
    private final AfterIdpConfirmPasswordResetServletTest afterIdpConfirmPasswordResetServletTest;
    private final AfterIdpRefreshAuthenticationTokenServletTest afterIdpRefreshAuthenticationTokenServletTest;
    private final AfterIdpSignOutServletTest afterIdpSignOutServletTest;
    private final CheckTokenServletTest checkTokenServletTest;
    private final CreateUnauthenticatedSessionServletTest createUnauthenticatedSessionServletTest;
    private final DeleteAccountByAppbaseUserIdServletTest deleteAccountByAppbaseUserIdServletTest;
    private final DeleteAccountByIdpUserIdServletTest deleteAccountByIdpUserIdServletTest;
    private final DeleteGuestUserServletTest deleteGuestUserServletTest;
    private final DeleteSessionServletTest deleteSessionServletTest;
    private final DeleteUnauthenticatedSessionServletTest deleteUnauthenticatedSessionServletTest;
    private final ExistsAccountByAppbaseUserIdServletTest existsAccountByAppbaseUserIdServletTest;
    private final ExistsAccountByEmailServletTest existsAccountByEmailServletTest;
    private final ExistsAccountByIdpUserIdServletTest existsAccountByIdpUserIdServletTest;
    private final IsExpiredSessionServletTest isExpiredSessionServletTest;
    private final ReadAccountByAppbaseUserIdServletTest readAccountByAppbaseUserIdServletTest;
    private final ReadAccountByEmailServletTest readAccountByEmailServletTest;
    private final ReadAccountByIdpUserIdServletTest readAccountByIdpUserIdServletTest;
    private final ReadGuestUserSessionServletTest readGuestUserSessionServletTest;
    private final ReadSessionServletTest readSessionServletTest;
    private final ReadUnauthenticatedSessionServletTest readUnauthenticatedSessionServletTest;
    private final RenewSessionServletTest renewSessionServletTest;
    private final UpdateAccountServletTest updateAccountServletTest;
    private final UpdateGuestSessionServletTest updateGuestSessionServletTest;
    private final UpdateSessionServletTest updateSessionServletTest;
    private final UpdateUnauthenticatedSessionServletTest updateUnauthenticatedSessionServletTest;

    public UserAccountServletTests(
            final AppbaseServices appbaseServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(appbaseServices, httpClient, cookieStore, baseUrl);

        this.afterIdpCreateUserWithEmailPasswordServletTest =
                new AfterIdpCreateUserWithEmailPasswordServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.afterIdpEmailPasswordSignInServletTest =
                new AfterIdpEmailPasswordSignInServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.afterIdpGuestSignInServletTest =
                new AfterIdpGuestSignInServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.afterIdpPasswordResetSubmitServletTest =
                new AfterIdpPasswordResetSubmitServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.afterIdpConfirmPasswordResetServletTest =
                new AfterIdpConfirmPasswordResetServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.afterIdpRefreshAuthenticationTokenServletTest =
                new AfterIdpRefreshAuthenticationTokenServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.afterIdpSignOutServletTest =
                new AfterIdpSignOutServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.checkTokenServletTest = new CheckTokenServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.createUnauthenticatedSessionServletTest =
                new CreateUnauthenticatedSessionServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.deleteAccountByAppbaseUserIdServletTest =
                new DeleteAccountByAppbaseUserIdServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.deleteAccountByIdpUserIdServletTest =
                new DeleteAccountByIdpUserIdServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.deleteGuestUserServletTest =
                new DeleteGuestUserServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.deleteSessionServletTest =
                new DeleteSessionServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.deleteUnauthenticatedSessionServletTest =
                new DeleteUnauthenticatedSessionServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.existsAccountByAppbaseUserIdServletTest =
                new ExistsAccountByAppbaseUserIdServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.existsAccountByEmailServletTest =
                new ExistsAccountByEmailServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.existsAccountByIdpUserIdServletTest =
                new ExistsAccountByIdpUserIdServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.isExpiredSessionServletTest =
                new IsExpiredSessionServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.readAccountByAppbaseUserIdServletTest =
                new ReadAccountByAppbaseUserIdServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.readAccountByEmailServletTest =
                new ReadAccountByEmailServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.readAccountByIdpUserIdServletTest =
                new ReadAccountByIdpUserIdServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.readGuestUserSessionServletTest =
                new ReadGuestUserSessionServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.readSessionServletTest =
                new ReadSessionServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.readUnauthenticatedSessionServletTest =
                new ReadUnauthenticatedSessionServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.renewSessionServletTest =
                new RenewSessionServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.updateAccountServletTest =
                new UpdateAccountServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.updateGuestSessionServletTest =
                new UpdateGuestSessionServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.updateSessionServletTest =
                new UpdateSessionServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
        this.updateUnauthenticatedSessionServletTest =
                new UpdateUnauthenticatedSessionServletTest(appbaseServices, httpClient, cookieStore, baseUrl);
    }

    @Override
    protected void runServletTestsImpl() throws Exception {
        this.afterIdpCreateUserWithEmailPasswordServletTest.runServletTests();
        this.afterIdpEmailPasswordSignInServletTest.runServletTests();
        this.afterIdpGuestSignInServletTest.runServletTests();
        this.afterIdpPasswordResetSubmitServletTest.runServletTests();
        this.afterIdpConfirmPasswordResetServletTest.runServletTestsImpl();
        this.afterIdpRefreshAuthenticationTokenServletTest.runServletTests();
        this.afterIdpSignOutServletTest.runServletTests();
        this.checkTokenServletTest.runServletTests();
        this.createUnauthenticatedSessionServletTest.runServletTests();
        this.deleteAccountByAppbaseUserIdServletTest.runServletTests();
        this.deleteAccountByIdpUserIdServletTest.runServletTests();
        this.deleteGuestUserServletTest.runServletTests();
        this.deleteSessionServletTest.runServletTests();
        this.deleteUnauthenticatedSessionServletTest.runServletTests();
        this.existsAccountByAppbaseUserIdServletTest.runServletTests();
        this.existsAccountByEmailServletTest.runServletTests();
        this.existsAccountByIdpUserIdServletTest.runServletTests();
        this.isExpiredSessionServletTest.runServletTests();
        this.readAccountByAppbaseUserIdServletTest.runServletTests();
        this.readAccountByEmailServletTest.runServletTests();
        this.readAccountByIdpUserIdServletTest.runServletTests();
        this.readGuestUserSessionServletTest.runServletTests();
        this.readSessionServletTest.runServletTests();
        this.readUnauthenticatedSessionServletTest.runServletTests();
        this.renewSessionServletTest.runServletTests();
        this.updateAccountServletTest.runServletTests();
        this.updateGuestSessionServletTest.runServletTests();
        this.updateSessionServletTest.runServletTests();
        this.updateUnauthenticatedSessionServletTest.runServletTests();
    }
}
