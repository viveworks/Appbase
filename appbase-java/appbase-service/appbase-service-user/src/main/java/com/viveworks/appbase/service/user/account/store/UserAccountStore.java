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

package com.viveworks.appbase.service.user.account.store;

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.service.user.account.data.UserAccount;

import java.util.Optional;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface UserAccountStore extends AppbaseService {
    int createUserAccount(UserAccount userAccount) throws UserAccountStoreException;

    Optional<UserAccount> readByAppbaseUserId(String appbaseUserId) throws UserAccountStoreException;

    Optional<UserAccount> readByIdpUserId(String idpUserId) throws UserAccountStoreException;

    Optional<UserAccount> readByEmail(String email) throws UserAccountStoreException;

    boolean existsByAppbaseUserId(String appbaseUserId) throws UserAccountStoreException;

    boolean existsByIdpUserId(String idpUserId) throws UserAccountStoreException;

    boolean existsByEmail(String email) throws UserAccountStoreException;

    int updateUserAccount(UserAccount userAccount) throws UserAccountStoreException;

    boolean deleteByIdpUserId(String idpUserId) throws UserAccountStoreException;

    boolean deleteByAppbaseUserId(String appbaseUserId) throws UserAccountStoreException;

    boolean deleteByEmail(String email) throws UserAccountStoreException;
}
