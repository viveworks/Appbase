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

package com.viveworks.appbase.service.web.oauth.okta;

/**
 * The bounded profile this module reads from an Okta userinfo response. Profile lookup only — the module
 * exposes no field or method that would revoke a token or mutate the account (spec §5, minimum scope).
 *
 * @param subject           the Okta user id ({@code sub})
 * @param email             the account email
 * @param emailVerified     whether Okta has verified the email
 * @param name              the display name
 * @param preferredUsername the Okta sign-in username
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public record OktaUserProfile(
        String subject,
        String email,
        boolean emailVerified,
        String name,
        String preferredUsername) {
}
