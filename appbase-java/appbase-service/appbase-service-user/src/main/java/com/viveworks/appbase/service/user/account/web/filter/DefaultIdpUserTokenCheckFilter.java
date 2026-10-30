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
package com.viveworks.appbase.service.user.account.web.filter;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.user.session.data.UserSession;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultIdpUserTokenCheckFilter implements IdpUserTokenCheckFilter {
    @Override
    public boolean idpUserTokenMatches(
            final String idpUserToken,
            final UserSession userSession)
    {
        AppbaseUtil.notBlank(idpUserToken, "idpUserToken");
        AppbaseUtil.notNull(userSession, "userSession");

        return Objects.equals(idpUserToken, userSession.getIdpUserToken());
    }
}
