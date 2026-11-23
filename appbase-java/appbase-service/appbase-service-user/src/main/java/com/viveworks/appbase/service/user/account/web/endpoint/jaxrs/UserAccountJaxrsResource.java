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

package com.viveworks.appbase.service.user.account.web.endpoint.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.user.account.AuthenticationServiceException;
import com.viveworks.appbase.service.user.account.UserAccountService;
import com.viveworks.appbase.service.user.account.UserAccountServiceException;
import com.viveworks.appbase.service.user.account.data.AfterIdpConfirmPasswordResetRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpConfirmPasswordResetResponse;
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordResponse;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.session.data.UserSession;
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
import com.viveworks.appbase.service.user.account.data.UpdateAccountRequest;
import com.viveworks.appbase.service.user.account.data.UpdateAccountResponse;
import com.viveworks.appbase.service.user.account.data.UpdateGuestSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateGuestSessionResponse;
import com.viveworks.appbase.service.user.account.data.UpdateSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateSessionResponse;
import com.viveworks.appbase.service.user.account.data.UpdateUnauthenticatedSessionRequest;
import com.viveworks.appbase.service.user.account.data.UpdateUnauthenticatedSessionResponse;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.ServletContext;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.annotation.Timed;

import javax.annotation.Nullable;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Path(AppbaseServiceConstants.DEFAULT_USER_ACCOUNTS_RESTAPI_PATH)
public class UserAccountJaxrsResource extends AbstractAppbaseJaxrsEntity {
    public static final String AFTER_IDP_CREATE_USER_WITH_EMAIL_PASSWORD_PATH = "/afterIdpCreateUserWithEmailPassword";
    public static final String AFTER_IDP_EMAIL_PASSWORD_SIGN_IN_PATH = "/afterIdpEmailPasswordSignIn";
    public static final String AFTER_IDP_GUEST_SIGN_IN_PATH = "/afterIdpGuestSignIn";
    public static final String AFTER_IDP_PASSWORD_RESET_SUBMIT_PATH = "/afterIdpPasswordResetSubmit";
    public static final String AFTER_IDP_PASSWORD_RESET_EMAIL_SENT_PATH = "/afterIdpPasswordResetSent";
    public static final String AFTER_IDP_REFRESH_AUTHENTICATION_TOKEN_PATH = "/afterIdpRefreshAuthenticationToken";
    public static final String AFTER_IDP_CONFIRM_PASSWORD_RESET_PATH = "/afterIdpConfirmPasswordReset";
    public static final String AFTER_IDP_SIGN_OUT_PATH = "/afterIdpSignOut";
    public static final String UNAUTHENTICATED_SESSION_PATH = "/unauthenticatedSession";
    public static final String ACCOUNT_BY_APPBASE_USER_ID_PATH = "/accountByAppbaseUserId";
    public static final String ACCOUNT_BY_IDP_USER_ID_PATH = "/accountByIdpUserId";
    public static final String ACCOUNT_BY_EMAIL_PATH = "/accountByEmail";
    public static final String SESSION_PATH = "/session";
    public static final String GUEST_USER_PATH = "/guestUser";
    public static final String GUEST_USER_SESSION_PATH = "/guestUserSession";
    public static final String CHECK_TOKEN_PATH = "/checkToken";
    public static final String IS_EXPIRED_SESSION_PATH = "/isExpiredSession";
    public static final String RENEW_SESSION_PATH = "/renewSession";
    public static final String ACCOUNT_PATH = "/account";


    @POST
    @Path(AFTER_IDP_CREATE_USER_WITH_EMAIL_PASSWORD_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Called after the client (a web or mobile application) creates a user using " +
            "email and password with an external IDS service (Firebase Auth, Okta, ...)")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = AfterIdpCreateUserWithEmailPasswordResponse.class)),
            description = "After IDP create user with email and password response",
            responseCode = "201")
    @Timed
    public Response afterIdpCreateUserWithEmailPassword(
            @NotNull
            @AfterIdpCreateUserWithEmailPasswordRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = AfterIdpCreateUserWithEmailPasswordRequest.class)),
                    description = "After IDP create user with email and password request",
                    required = true)
            final AfterIdpCreateUserWithEmailPasswordRequest afterIdpCreateUserWithEmailPasswordRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final AfterIdpCreateUserWithEmailPasswordResponse afterIdpCreateUserWithEmailPasswordResponse =
                    userAccountService.afterIdpCreateUserWithEmailPassword(afterIdpCreateUserWithEmailPasswordRequest);
            return okResponse(withoutPasswordHashes(afterIdpCreateUserWithEmailPasswordResponse));
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (AuthenticationServiceException e) {
            handleAuthenticationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    private AfterIdpCreateUserWithEmailPasswordResponse withoutPasswordHashes(
            final AfterIdpCreateUserWithEmailPasswordResponse response)
    {
        final UserSession userSession = response.getUserSession();
        final UserAccount sanitizedUserAccount = new UserAccount(userSession.getUserAccount(), null);
        final UserSession sanitizedUserSession = new UserSession(userSession, sanitizedUserAccount);
        return new AfterIdpCreateUserWithEmailPasswordResponse(response.getTraceId(), sanitizedUserSession);
    }

    @POST
    @Path(AFTER_IDP_EMAIL_PASSWORD_SIGN_IN_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Called after the client (a web or mobile application) signs in using email and password " +
            "with an external IDS service (Firebase Auth, Okta, ...)")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = AfterIdpEmailPasswordSignInResponse.class)),
            description = "After IDP email and password sign in response",
            responseCode = "200")
    @Timed
    public Response afterIdpEmailPasswordSignIn(
            @NotNull
            @AfterIdpEmailPasswordSignInRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = AfterIdpEmailPasswordSignInRequest.class)),
                    description = "After IDP email and password sign in request",
                    required = true)
            final AfterIdpEmailPasswordSignInRequest afterIdpEmailPasswordSignInRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final AfterIdpEmailPasswordSignInResponse afterIdpEmailPasswordSignInResponse =
                    userAccountService.afterIdpEmailPasswordSignIn(afterIdpEmailPasswordSignInRequest);
            return okResponse(afterIdpEmailPasswordSignInResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (AuthenticationServiceException e) {
            handleAuthenticationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @POST
    @Path(AFTER_IDP_GUEST_SIGN_IN_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Called after the client (a web or mobile application) signs in as guest " +
            "with an external IDS service (Firebase Auth, Okta, ...)")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = AfterIdpGuestSignInResponse.class)),
            description = "After IDP guest sign in response",
            responseCode = "200")
    @Timed
    public Response afterIdpGuestSignIn(
            @NotNull
            @AfterIdpGuestSignInRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = AfterIdpGuestSignInRequest.class)),
                    description = "After IDP guest sign in request",
                    required = true)
            final AfterIdpGuestSignInRequest afterIdpGuestSignInRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final AfterIdpGuestSignInResponse afterIdpGuestSignInResponse =
                    userAccountService.afterIdpGuestSignIn(afterIdpGuestSignInRequest);
            return okResponse(afterIdpGuestSignInResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (AuthenticationServiceException e) {
            handleAuthenticationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @POST
    @Path(AFTER_IDP_PASSWORD_RESET_EMAIL_SENT_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Called after the client (a web or mobile application) submits a password reset request " +
            "with the external IDS service (Firebase Auth, Okta, ...) and an email is sent with the code. " +
            "Requires a verified IDP token bound to the target account.")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = AfterIdpPasswordResetEmailSentResponse.class)),
            description = "After IDP password reset email response",
            responseCode = "200")
    @Timed
    public Response afterIdpPasswordResetEmailSent(
            @NotNull
            @AfterIdpPasswordResetEmailSentRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = AfterIdpPasswordResetEmailSentRequest.class)),
                    description = "After IDP password email sent request",
                    required = true)
            final AfterIdpPasswordResetEmailSentRequest afterIdpPasswordResetEmailSentRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final AfterIdpPasswordResetEmailSentResponse afterIdpPasswordResetEmailSentResponse =
                    userAccountService.afterIdpPasswordResetEmailSent(afterIdpPasswordResetEmailSentRequest);
            return okResponse(afterIdpPasswordResetEmailSentResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (AuthenticationServiceException e) {
            handleAuthenticationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @POST
    @Path(AFTER_IDP_PASSWORD_RESET_SUBMIT_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Called after the client (a web or mobile application) submits a password reset request " +
            "with the external IDS service (Firebase Auth, Okta, ...)")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = AfterIdpPasswordResetSubmitResponse.class)),
            description = "After IDP password reset submit response",
            responseCode = "200")
    @Timed
    public Response afterIdpPasswordResetSubmit(
            @NotNull
            @AfterIdpPasswordResetSubmitRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = AfterIdpPasswordResetSubmitRequest.class)),
                    description = "After IDP guest sign in request",
                    required = true)
            final AfterIdpPasswordResetSubmitRequest afterIdpPasswordResetSubmitRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final AfterIdpPasswordResetSubmitResponse afterIdpPasswordResetSubmitResponse =
                    userAccountService.afterIdpPasswordResetSubmit(afterIdpPasswordResetSubmitRequest);
            return okResponse(afterIdpPasswordResetSubmitResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @POST
    @Path(AFTER_IDP_CONFIRM_PASSWORD_RESET_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Called after the client (a web or mobile application) confirms " +
            "password reset with a code and new password with an external IDS service (Firebase Auth, Okta, ...)")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = AfterIdpConfirmPasswordResetResponse.class)),
            description = "After IDP confirm password reset response",
            responseCode = "201")
    @Timed
    public Response afterIdpConfirmPasswordReset(
            @NotNull
            @AfterIdpConfirmPasswordResetRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = AfterIdpConfirmPasswordResetRequest.class)),
                    description = "After IDP create user with email and password request",
                    required = true)
            final AfterIdpConfirmPasswordResetRequest afterIdpConfirmPasswordResetRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);

        try {
            final AfterIdpConfirmPasswordResetResponse afterIdpConfirmPasswordResetResponse =
                    userAccountService.afterIdpConfirmPasswordReset(afterIdpConfirmPasswordResetRequest);
            return okResponse(afterIdpConfirmPasswordResetResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @POST
    @Path(AFTER_IDP_REFRESH_AUTHENTICATION_TOKEN_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Called after the client (a web or mobile application) refreshes a user token " +
            "with the external IDS service (Firebase Auth, Okta, ...)")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = AfterIdpRefreshAuthenticationTokenResponse.class)),
            description = "After IDP refresh authentication token response",
            responseCode = "200")
    @Timed
    public Response afterIdpRefreshAuthenticationToken(
            @NotNull
            @AfterIdpRefreshAuthenticationTokenRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = AfterIdpRefreshAuthenticationTokenRequest.class)),
                    description = "After IDP guest sign in request",
                    required = true)
            final AfterIdpRefreshAuthenticationTokenRequest afterIdpRefreshAuthenticationTokenRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final AfterIdpRefreshAuthenticationTokenResponse afterIdpRefreshAuthenticationTokenResponse =
                    userAccountService.afterIdpRefreshAuthenticationToken(afterIdpRefreshAuthenticationTokenRequest);
            return okResponse(afterIdpRefreshAuthenticationTokenResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (AuthenticationServiceException e) {
            handleAuthenticationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @POST
    @Path(AFTER_IDP_SIGN_OUT_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Called after the client (a web or mobile application) signs out " +
            "with the external IDS service (Firebase Auth, Okta, ...)")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = AfterIdpSignOutResponse.class)),
            description = "After IDP sign out response",
            responseCode = "200")
    @Timed
    public Response afterIdpSignOut(
            @NotNull
            @AfterIdpSignOutRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = AfterIdpSignOutRequest.class)),
                    description = "After IDP sign out request",
                    required = true)
            final AfterIdpSignOutRequest afterIdpSignOutRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final AfterIdpSignOutResponse afterIdpSignOutResponse =
                    userAccountService.afterIdpSignOut(afterIdpSignOutRequest);
            return okResponse(afterIdpSignOutResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @POST
    @Path(UNAUTHENTICATED_SESSION_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create unauthenticated session")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = CreateUnauthenticatedSessionResponse.class)),
            description = "Create unauthenticated session response",
            responseCode = "200")
    @Timed
    public Response createUnauthenticatedSession(
            @NotNull
            @CreateUnauthenticatedSessionRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = CreateUnauthenticatedSessionRequest.class)),
                    description = "Create unauthenticated session request",
                    required = true)
            final CreateUnauthenticatedSessionRequest createUnauthenticatedSessionRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final CreateUnauthenticatedSessionResponse createUnauthenticatedSessionResponse =
                    userAccountService.createUnauthenticatedSession(createUnauthenticatedSessionRequest);
            return okResponse(createUnauthenticatedSessionResponse);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @HEAD
    @Path(ACCOUNT_BY_APPBASE_USER_ID_PATH)
    @Operation(summary = "Check if account exists by appbaseUserId")
    @ApiResponse(
            responseCode = "204",
            description = "Returns 204 if the account exists, otherwise 404")
    @Timed
    public Response existsAccountByAppbaseUserId(
            @NotNull
            @QueryParam("appbaseUserId")
            final String appbaseUserId,
            @NotNull
            @QueryParam("appbaseRequesterId")
            final String appbaseRequesterId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final ExistsAccountByIdRequest existsAccountByIdRequest = new ExistsAccountByIdRequest(
                    traceId, appbaseRequesterId, appbaseUserId);
            final ExistsAccountByIdResponse existsAccountByIdResponse =
                    userAccountService.existsAccountByAppbaseUserId(existsAccountByIdRequest);
            if (existsAccountByIdResponse != null && existsAccountByIdResponse.isExists()) {
                return noContentResponse();
            }
            return notFoundResponse();
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @HEAD
    @Path(ACCOUNT_BY_IDP_USER_ID_PATH)
    @Operation(summary = "Check if account exists by idpUserId")
    @ApiResponse(
            responseCode = "204",
            description = "Returns 204 if the account exists, otherwise 404")
    @Timed
    public Response existsAccountByIdpUserId(
            @NotNull
            @QueryParam("idpUserId")
            final String idpUserId,
            @NotNull
            @QueryParam("appbaseRequesterId")
            final String appbaseRequesterId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final ExistsAccountByIdRequest existsAccountByIdRequest = new ExistsAccountByIdRequest(
                    traceId, appbaseRequesterId, idpUserId);
            final ExistsAccountByIdResponse existsAccountByIdResponse =
                    userAccountService.existsAccountByIdpUserId(existsAccountByIdRequest);
            if (existsAccountByIdResponse != null && existsAccountByIdResponse.isExists()) {
                return noContentResponse();
            }
            return notFoundResponse();
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @HEAD
    @Path(ACCOUNT_BY_EMAIL_PATH)
    @Operation(summary = "Check if account exists by email")
    @ApiResponse(
            responseCode = "204",
            description = "Returns 204 if the account exists, otherwise 404")
    @Timed
    public Response existsAccountByEmail(
            @NotNull
            @QueryParam("email")
            final String email,
            @NotNull
            @QueryParam("appbaseRequesterId")
            final String appbaseRequesterId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final ExistsAccountByIdRequest existsAccountByIdRequest = new ExistsAccountByIdRequest(
                    traceId, appbaseRequesterId, email);
            final ExistsAccountByIdResponse existsAccountByIdResponse =
                    userAccountService.existsAccountByEmail(existsAccountByIdRequest);
            if (existsAccountByIdResponse != null && existsAccountByIdResponse.isExists()) {
                return noContentResponse();
            }
            return notFoundResponse();
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @GET
    @Path(ACCOUNT_BY_APPBASE_USER_ID_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read account by appbaseUserId")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ReadAccountByIdResponse.class)),
            description = "Read account by appbaseUserId response",
            responseCode = "200")
    @Timed
    public Response readAccountByAppbaseUserId(
            @NotNull
            @QueryParam("appbaseUserId")
            final String appbaseUserId,
            @NotNull
            @QueryParam("appbaseRequesterId")
            final String appbaseRequesterId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final ReadAccountByIdRequest readAccountByIdRequest = new ReadAccountByIdRequest(
                    traceId, appbaseRequesterId, appbaseUserId);
            final ReadAccountByIdResponse readAccountByIdResponse =
                    userAccountService.readAccountByAppbaseUserId(readAccountByIdRequest);
            return okResponse(readAccountByIdResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @GET
    @Path(ACCOUNT_BY_IDP_USER_ID_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read account by idpUserId")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ReadAccountByIdResponse.class)),
            description = "Read account by idpUserId response",
            responseCode = "200")
    @Timed
    public Response readAccountByIdpUserId(
            @NotNull
            @QueryParam("idpUserId")
            final String idpUserId,
            @NotNull
            @QueryParam("appbaseRequesterId")
            final String appbaseRequesterId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final ReadAccountByIdRequest readAccountByIdRequest = new ReadAccountByIdRequest(
                    traceId, appbaseRequesterId, idpUserId);
            final ReadAccountByIdResponse readAccountByIdResponse =
                    userAccountService.readAccountByIdpUserId(readAccountByIdRequest);
            return okResponse(readAccountByIdResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @GET
    @Path(ACCOUNT_BY_EMAIL_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read account by email")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ReadAccountByIdResponse.class)),
            description = "Read account by email response",
            responseCode = "200")
    @Timed
    public Response readAccountByEmail(
            @NotNull
            @QueryParam("email")
            final String email,
            @NotNull
            @QueryParam("appbaseRequesterId")
            final String appbaseRequesterId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final ReadAccountByIdRequest readAccountByIdRequest = new ReadAccountByIdRequest(
                    traceId, appbaseRequesterId, email);
            final ReadAccountByIdResponse readAccountByIdResponse =
                    userAccountService.readAccountByEmail(readAccountByIdRequest);
            return okResponse(readAccountByIdResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @GET
    @Path(SESSION_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read session")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ReadSessionResponse.class)),
            description = "Read session response",
            responseCode = "200")
    @Timed
    public Response readSession(
            @NotNull
            @QueryParam("appbaseUserId")
            final String appbaseUserId,
            @NotNull
            @QueryParam("appbaseSessionId")
            final String appbaseSessionId,
            @NotNull
            @QueryParam("appbaseRequesterId")
            final String appbaseRequesterId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final ReadSessionRequest readSessionRequest = new ReadSessionRequest(
                    traceId, appbaseRequesterId, appbaseUserId, appbaseSessionId);
            final ReadSessionResponse readSessionResponse = userAccountService.readSession(readSessionRequest);
            return okResponse(readSessionResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @GET
    @Path(GUEST_USER_SESSION_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read guest user session")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ReadGuestUserSessionResponse.class)),
            description = "Read guest user session response",
            responseCode = "200")
    @Timed
    public Response readGuestUserSession(
            @NotNull
            @QueryParam("appbaseUserId")
            final String appbaseUserId,
            @NotNull
            @QueryParam("appbaseSessionId")
            final String appbaseSessionId,
            @NotNull
            @QueryParam("appbaseRequesterId")
            final String appbaseRequesterId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final ReadGuestUserSessionRequest readGuestUserSessionRequest =
                    new ReadGuestUserSessionRequest(traceId, appbaseRequesterId, appbaseUserId, appbaseSessionId);
            final ReadGuestUserSessionResponse readGuestUserSessionResponse =
                    userAccountService.readGuestUserSession(readGuestUserSessionRequest);
            return okResponse(readGuestUserSessionResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @GET
    @Path(UNAUTHENTICATED_SESSION_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read unauthenticated session")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ReadUnauthenticatedSessionResponse.class)),
            description = "Read unauthenticated session response",
            responseCode = "200")
    @Timed
    public Response readUnauthenticatedSession(
            @NotNull
            @QueryParam("appbaseSessionId")
            final String appbaseSessionId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final ReadUnauthenticatedSessionRequest readUnauthenticatedSessionRequest =
                    new ReadUnauthenticatedSessionRequest(traceId, appbaseSessionId);
            final ReadUnauthenticatedSessionResponse readUnauthenticatedSessionResponse =
                    userAccountService.readUnauthenticatedSession(readUnauthenticatedSessionRequest);
            return okResponse(readUnauthenticatedSessionResponse);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @POST
    @Path(CHECK_TOKEN_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check user token from by an external IDP service (Firebase Auth, Okta, ...)")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = CheckTokenResponse.class)),
            description = "Check token response",
            responseCode = "200")
    @Timed
    public Response checkToken(
            @NotNull
            @CheckTokenRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = CheckTokenRequest.class)),
                    description = "Check token request",
                    required = true)
            final CheckTokenRequest checkTokenRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final CheckTokenResponse checkTokenResponse = userAccountService.checkToken(checkTokenRequest);
            return okResponse(checkTokenResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (AuthenticationServiceException e) {
            handleAuthenticationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @PUT
    @Path(IS_EXPIRED_SESSION_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check session expiration")
    @ApiResponse(
            responseCode = "204",
            description = "Returns 204 if not expired, otherwise 404")
    @Timed
    public Response isExpiredSession(
            @NotNull
            @IsExpiredSessionRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = IsExpiredSessionRequest.class)),
                    description = "Check session expiration request",
                    required = true)
            final IsExpiredSessionRequest isExpiredSessionRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final IsExpiredSessionResponse isExpiredSessionResponse =
                    userAccountService.isExpiredSession(isExpiredSessionRequest);
            if (isExpiredSessionResponse != null && !isExpiredSessionResponse.isExpired()) {
                return noContentResponse();
            }
            return notFoundResponse();
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @PUT
    @Path(RENEW_SESSION_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Renew session")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = RenewSessionResponse.class)),
            description = "Renew session response",
            responseCode = "200")
    @Timed
    public Response renewSession(
            @NotNull
            @RenewSessionRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = RenewSessionRequest.class)),
                    description = "Renew session request",
                    required = true)
            final RenewSessionRequest renewSessionRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final RenewSessionResponse renewSessionResponse = userAccountService.renewSession(renewSessionRequest);
            return okResponse(renewSessionResponse);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @PUT
    @Path(ACCOUNT_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update account")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = UpdateAccountResponse.class)),
            description = "Update account response",
            responseCode = "200")
    @Timed
    public Response updateAccount(
            @NotNull
            @UpdateAccountRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = UpdateAccountRequest.class)),
                    description = "Update account request",
                    required = true)
            final UpdateAccountRequest updateAccountRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final UpdateAccountResponse updateAccountResponse = userAccountService.updateAccount(updateAccountRequest);
            return okResponse(updateAccountResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @PUT
    @Path(SESSION_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update session")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = UpdateSessionResponse.class)),
            description = "Update session response",
            responseCode = "200")
    @Timed
    public Response updateSession(
            @NotNull
            @UpdateSessionRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = UpdateSessionRequest.class)),
                    description = "Update session request",
                    required = true)
            final UpdateSessionRequest updateSessionRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final UpdateSessionResponse updateSessionResponse = userAccountService.updateSession(updateSessionRequest);
            return okResponse(updateSessionResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (AuthenticationServiceException e) {
            handleAuthenticationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @PUT
    @Path(GUEST_USER_SESSION_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update guest session")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = UpdateGuestSessionResponse.class)),
            description = "Update guest session response",
            responseCode = "200")
    @Timed
    public Response updateGuestSession(
            @NotNull
            @UpdateGuestSessionRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = UpdateGuestSessionRequest.class)),
                    description = "Update guest session request",
                    required = true)
            final UpdateGuestSessionRequest updateGuestSessionRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final UpdateGuestSessionResponse updateGuestSessionResponse =
                    userAccountService.updateGuestSession(updateGuestSessionRequest);
            return okResponse(updateGuestSessionResponse);
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (AuthenticationServiceException e) {
            handleAuthenticationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @PUT
    @Path(UNAUTHENTICATED_SESSION_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update unauthenticated session")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = UpdateUnauthenticatedSessionResponse.class)),
            description = "Update unauthenticated session response",
            responseCode = "200")
    @Timed
    public Response updateUnauthenticatedSession(
            @NotNull
            @UpdateUnauthenticatedSessionRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = UpdateUnauthenticatedSessionRequest.class)),
                    description = "Update guest session request",
                    required = true)
            final UpdateUnauthenticatedSessionRequest updateUnauthenticatedSessionRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final UpdateUnauthenticatedSessionResponse updateUnauthenticatedSessionResponse =
                    userAccountService.updateUnauthenticatedSession(updateUnauthenticatedSessionRequest);
            return okResponse(updateUnauthenticatedSessionResponse);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @DELETE
    @Path(SESSION_PATH)
    @Operation(summary = "Delete session")
    @ApiResponse(
            responseCode = "204",
            description = "Returns 204 if deleted, otherwise 404")
    @Timed
    public Response deleteSession(
            @NotNull
            @QueryParam("appbaseUserId")
            final String appbaseUserId,
            @NotNull
            @QueryParam("appbaseSessionId")
            final String appbaseSessionId,
            @NotNull
            @QueryParam("appbaseRequesterId")
            final String appbaseRequesterId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final DeleteSessionRequest deleteSessionRequest =
                    new DeleteSessionRequest(traceId, appbaseRequesterId, appbaseUserId, appbaseSessionId);
            final DeleteSessionResponse deleteSessionResponse = userAccountService.deleteSession(deleteSessionRequest);
            if (deleteSessionResponse != null && deleteSessionResponse.isDeleted()) {
                return noContentResponse();
            }
            return notFoundResponse();
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @DELETE
    @Path(ACCOUNT_BY_APPBASE_USER_ID_PATH)
    @Operation(summary = "Delete account by appbaseUserId")
    @ApiResponse(
            responseCode = "204",
            description = "Returns 204 if deleted, otherwise 404")
    @Timed
    public Response deleteAccountByAppbaseUserId(
            @NotNull
            @QueryParam("appbaseUserId")
            final String appbaseUserId,
            @NotNull
            @QueryParam("appbaseRequesterId")
            final String appbaseRequesterId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final DeleteAccountByAppbaseUserIdRequest deleteAccountByAppbaseUserIdRequest =
                    new DeleteAccountByAppbaseUserIdRequest(traceId, appbaseRequesterId, appbaseUserId);
            final DeleteAccountByAppbaseUserIdResponse deleteAccountByAppbaseUserIdResponse =
                    userAccountService.deleteAccountByAppbaseUserId(deleteAccountByAppbaseUserIdRequest);
            if (deleteAccountByAppbaseUserIdResponse != null && deleteAccountByAppbaseUserIdResponse.isDeleted()) {
                return noContentResponse();
            }
            return notFoundResponse();
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @DELETE
    @Path(ACCOUNT_BY_IDP_USER_ID_PATH)
    @Operation(summary = "Delete account by idpUserId")
    @ApiResponse(
            responseCode = "204",
            description = "Returns 204 if deleted, otherwise 404")
    @Timed
    public Response deleteAccountByIdpUserId(
            @NotNull
            @QueryParam("idpUserId")
            final String idpUserId,
            @NotNull
            @QueryParam("appbaseRequesterId")
            final String appbaseRequesterId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final DeleteAccountByIdpUserIdRequest deleteAccountByIdpUserIdRequest =
                    new DeleteAccountByIdpUserIdRequest(traceId, appbaseRequesterId, idpUserId);
            final DeleteAccountByIdpUserIdResponse deleteAccountByIdpUserIdResponse =
                    userAccountService.deleteAccountByIdpUserId(deleteAccountByIdpUserIdRequest);
            if (deleteAccountByIdpUserIdResponse != null && deleteAccountByIdpUserIdResponse.isDeleted()) {
                return noContentResponse();
            }
            return notFoundResponse();
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @DELETE
    @Path(GUEST_USER_PATH)
    @Operation(summary = "Delete guest user")
    @ApiResponse(
            responseCode = "204",
            description = "Returns 204 if deleted, otherwise 404")
    @Timed
    public Response deleteGuestUser(
            @NotNull
            @QueryParam("appbaseUserId")
            final String appbaseUserId,
            @NotNull
            @QueryParam("appbaseRequesterId")
            final String appbaseRequesterId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final DeleteGuestUserRequest deleteGuestUserRequest =
                    new DeleteGuestUserRequest(traceId, appbaseRequesterId, appbaseUserId);
            final DeleteGuestUserResponse deleteGuestUserResponse =
                    userAccountService.deleteGuestUser(deleteGuestUserRequest);
            if (deleteGuestUserResponse != null && deleteGuestUserResponse.isDeleted()) {
                return noContentResponse();
            }
            return notFoundResponse();
        } catch (AuthorizationServiceException e) {
            handleAuthorizationServiceException(servletContext, e);
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @DELETE
    @Path(UNAUTHENTICATED_SESSION_PATH)
    @Operation(summary = "Delete unauthenticated session")
    @ApiResponse(
            responseCode = "204",
            description = "Returns 204 if deleted, otherwise 404")
    @Timed
    public Response deleteUnauthenticatedSession(
            @NotNull
            @QueryParam("appbaseSessionId")
            final String appbaseSessionId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final UserAccountService userAccountService = userAccountService(servletContext);
        try {
            final DeleteUnauthenticatedSessionRequest deleteUnauthenticatedSessionRequest =
                    new DeleteUnauthenticatedSessionRequest(traceId, appbaseSessionId);
            final DeleteUnauthenticatedSessionResponse deleteUnauthenticatedSessionResponse =
                    userAccountService.deleteUnauthenticatedSession(deleteUnauthenticatedSessionRequest);
            if (deleteUnauthenticatedSessionResponse != null && deleteUnauthenticatedSessionResponse.isDeleted()) {
                return noContentResponse();
            }
            return notFoundResponse();
        } catch (UserAccountServiceException e) {
            handleUserAccountServiceException(servletContext, e);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    protected UserAccountService userAccountService(@Nullable final ServletContext servletContext) {
        final AppbaseCore appbaseCore = appbaseCore(servletContext);
        if (appbaseCore == null) {
            throw new AppbaseServiceRuntimeException("Cannot find an instance of " + AppbaseCore.class.getName());
        }

        final AppbaseServiceEntry appbaseServiceEntry = appbaseCore
                .getServiceRegistry()
                .getAppbaseService(UserAccountService.class);
        if (appbaseServiceEntry == null) {
            throw new AppbaseServiceRuntimeException(
                    "Cannot find a service entry for " + UserAccountService.class.getName());
        }

        final UserAccountService userAccountService = (UserAccountService) appbaseServiceEntry.getService();
        if (userAccountService == null) {
            throw new AppbaseServiceRuntimeException(
                    "Cannot find an instance of " + UserAccountService.class.getName());
        }

        return userAccountService;
    }
}
