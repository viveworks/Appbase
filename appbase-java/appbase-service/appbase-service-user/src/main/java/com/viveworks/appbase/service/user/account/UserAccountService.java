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

package com.viveworks.appbase.service.user.account;

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.user.account.data.AfterIdpConfirmPasswordResetRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpConfirmPasswordResetResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpEmailPasswordSignInRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpEmailPasswordSignInResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpGuestSignInRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpGuestSignInResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpPasswordResetEmailSentRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpPasswordResetEmailSentResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpPasswordResetSubmitRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpPasswordResetSubmitResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpRefreshAuthenticationTokenRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpRefreshAuthenticationTokenResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpSignOutRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpSignOutResponse;
import com.viveworks.appbase.service.user.account.data.CheckTokenRequest;
import com.viveworks.appbase.service.user.account.data.CheckTokenResponse;
import com.viveworks.appbase.service.user.account.data.CreateUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.CreateUnauthenticatedSessionResponse;
import com.viveworks.appbase.service.user.account.data.DeleteAccountByAppbaseUserIdRequest;
import com.viveworks.appbase.service.user.account.data.DeleteAccountByAppbaseUserIdResponse;
import com.viveworks.appbase.service.user.account.data.DeleteAccountByIdpUserIdRequest;
import com.viveworks.appbase.service.user.account.data.DeleteAccountByIdpUserIdResponse;
import com.viveworks.appbase.service.user.account.data.DeleteGuestUserRequest;
import com.viveworks.appbase.service.user.account.data.DeleteGuestUserResponse;
import com.viveworks.appbase.service.user.account.data.DeleteSessionRequest;
import com.viveworks.appbase.service.user.account.data.DeleteSessionResponse;
import com.viveworks.appbase.service.user.account.data.DeleteUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.DeleteUnauthenticatedSessionResponse;
import com.viveworks.appbase.service.user.account.data.ExistsAccountByIdRequest;
import com.viveworks.appbase.service.user.account.data.ExistsAccountByIdResponse;
import com.viveworks.appbase.service.user.account.data.IsExpiredSessionRequest;
import com.viveworks.appbase.service.user.account.data.IsExpiredSessionResponse;
import com.viveworks.appbase.service.user.account.data.ReadAccountByIdRequest;
import com.viveworks.appbase.service.user.account.data.ReadAccountByIdResponse;
import com.viveworks.appbase.service.user.account.data.ReadGuestUserSessionRequest;
import com.viveworks.appbase.service.user.account.data.ReadGuestUserSessionResponse;
import com.viveworks.appbase.service.user.account.data.ReadSessionRequest;
import com.viveworks.appbase.service.user.account.data.ReadSessionResponse;
import com.viveworks.appbase.service.user.account.data.ReadUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.ReadUnauthenticatedSessionResponse;
import com.viveworks.appbase.service.user.account.data.RenewSessionRequest;
import com.viveworks.appbase.service.user.account.data.RenewSessionResponse;
import com.viveworks.appbase.service.user.account.data.SyncIdpUserRequest;
import com.viveworks.appbase.service.user.account.data.SyncIdpUserResponse;
import com.viveworks.appbase.service.user.account.data.UpdateAccountRequest;
import com.viveworks.appbase.service.user.account.data.UpdateAccountResponse;
import com.viveworks.appbase.service.user.account.data.UpdateGuestSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateGuestSessionResponse;
import com.viveworks.appbase.service.user.account.data.UpdateSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateSessionResponse;
import com.viveworks.appbase.service.user.account.data.UpdateUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateUnauthenticatedSessionResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface UserAccountService extends AppbaseService {
    String APP_NAME_KEY = "APP_NAME";

    AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPassword(
            AfterIdpCreateUserWithEmailPasswordRequest afterIdpCreateUserWithEmailPasswordRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException;

    AfterIdpEmailPasswordSignInResponse afterIdpEmailPasswordSignIn(
            AfterIdpEmailPasswordSignInRequest afterIdpEmailPasswordSignInRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException;

    AfterIdpGuestSignInResponse afterIdpGuestSignIn(
            AfterIdpGuestSignInRequest afterIdpGuestSignInRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException;

    AfterIdpRefreshAuthenticationTokenResponse afterIdpRefreshAuthenticationToken(
            AfterIdpRefreshAuthenticationTokenRequest afterIdpRefreshAuthenticationTokenRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException;

    AfterIdpPasswordResetSubmitResponse afterIdpPasswordResetSubmit(
            AfterIdpPasswordResetSubmitRequest afterIdpPasswordResetSubmitRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    AfterIdpPasswordResetEmailSentResponse afterIdpPasswordResetEmailSent(
            AfterIdpPasswordResetEmailSentRequest afterIdpPasswordResetEmailSentRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException;

    AfterIdpConfirmPasswordResetResponse afterIdpConfirmPasswordReset(
            AfterIdpConfirmPasswordResetRequest afterIdpConfirmPasswordResetRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    AfterIdpSignOutResponse afterIdpSignOut(AfterIdpSignOutRequest afterIdpSignOutRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    CreateUnauthenticatedSessionResponse createUnauthenticatedSession(
            CreateUnauthenticatedSessionRequest createUnauthenticatedSessionRequest)
            throws UserAccountServiceException;

    ExistsAccountByIdResponse existsAccountByAppbaseUserId(
            ExistsAccountByIdRequest existsAccountByIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    ExistsAccountByIdResponse existsAccountByIdpUserId(
            ExistsAccountByIdRequest existsAccountByIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    ExistsAccountByIdResponse existsAccountByEmail(
            ExistsAccountByIdRequest existsAccountByIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    ReadAccountByIdResponse readAccountByAppbaseUserId(
            ReadAccountByIdRequest readAccountByIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    ReadAccountByIdResponse readAccountByIdpUserId(
            ReadAccountByIdRequest readAccountByIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    ReadAccountByIdResponse readAccountByEmail(
            ReadAccountByIdRequest readAccountByIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    ReadSessionResponse readSession(ReadSessionRequest readSessionRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    ReadGuestUserSessionResponse readGuestUserSession(
            ReadGuestUserSessionRequest readGuestUserSessionRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    ReadUnauthenticatedSessionResponse readUnauthenticatedSession(
            ReadUnauthenticatedSessionRequest readUnauthenticatedSessionRequest)
            throws UserAccountServiceException;

    CheckTokenResponse checkToken(CheckTokenRequest checkTokenRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException;

    IsExpiredSessionResponse isExpiredSession(IsExpiredSessionRequest isExpiredSessionRequest)
            throws UserAccountServiceException;

    RenewSessionResponse renewSession(RenewSessionRequest renewSessionRequest)
            throws UserAccountServiceException;

    UpdateAccountResponse updateAccount(UpdateAccountRequest updateAccountRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    UpdateSessionResponse updateSession(UpdateSessionRequest updateSessionRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException;

    UpdateGuestSessionResponse updateGuestSession(
            UpdateGuestSessionRequest updateGuestSessionRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException;

    UpdateUnauthenticatedSessionResponse updateUnauthenticatedSession(
            UpdateUnauthenticatedSessionRequest updateUnauthenticatedSessionRequest)
            throws UserAccountServiceException;

    DeleteSessionResponse deleteSession(DeleteSessionRequest deleteSessionRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    DeleteAccountByAppbaseUserIdResponse deleteAccountByAppbaseUserId(
            DeleteAccountByAppbaseUserIdRequest deleteAccountByAppbaseUserIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    DeleteAccountByIdpUserIdResponse deleteAccountByIdpUserId(
            DeleteAccountByIdpUserIdRequest deleteAccountByIdpUserIdRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    DeleteGuestUserResponse deleteGuestUser(
            DeleteGuestUserRequest deleteGuestUserRequest)
            throws AuthorizationServiceException, UserAccountServiceException;

    DeleteUnauthenticatedSessionResponse deleteUnauthenticatedSession(
            DeleteUnauthenticatedSessionRequest deleteUnauthenticatedSessionRequest)
            throws UserAccountServiceException;

    SyncIdpUserResponse syncIdpUser(
            SyncIdpUserRequest syncIdpUserRequest)
            throws AuthorizationServiceException, AuthenticationServiceException, UserAccountServiceException;
}
