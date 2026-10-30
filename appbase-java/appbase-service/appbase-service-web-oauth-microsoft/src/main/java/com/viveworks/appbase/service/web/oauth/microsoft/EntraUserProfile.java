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

package com.viveworks.appbase.service.web.oauth.microsoft;

/**
 * The bounded profile this module reads from a Microsoft Graph {@code /me} response. Profile lookup only —
 * the module exposes no field or method that would revoke a token or mutate the account (spec §5, minimum
 * scope).
 *
 * @param subject           the Entra object id ({@code id}) — the stable user identifier
 * @param userPrincipalName the Entra sign-in name ({@code userPrincipalName})
 * @param displayName       the display name
 * @param mail              the account's primary SMTP address ({@code mail}); may be {@code null} when the
 *                          account has no mailbox
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public record EntraUserProfile(
        String subject,
        String userPrincipalName,
        String displayName,
        String mail) {
}
