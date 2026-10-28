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

import com.viveworks.appbase.service.AppbaseServiceException;
import com.viveworks.appbase.service.user.account.data.ExistsAccountByIdRequest;
import com.viveworks.appbase.service.user.account.data.ReadAccountByIdRequest;
import com.viveworks.appbase.service.user.account.data.UserAccount;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class UserAccountServiceException extends AppbaseServiceException {
    private static final long serialVersionUID = 2004237459735099923L;

    public UserAccountServiceException() {
    }

    public UserAccountServiceException(final String message) {
        super(message);
    }

    public UserAccountServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public UserAccountServiceException(final Throwable cause) {
        super(cause);
    }

    public UserAccountServiceException(
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public static class StoreAccountException extends UserAccountServiceException {
        private static final long serialVersionUID = 1873499283332468814L;

        public StoreAccountException(final UserAccount userAccount) {
            super(userAccount.toString());
        }
    }

    public static class CreateAccountException extends UserAccountServiceException {
        private static final long serialVersionUID = 2420073169354767381L;

        public CreateAccountException(final UserAccount userAccount) {
            super(userAccount.toString());
        }
    }

    public static class ExistsAccountByIdException extends UserAccountServiceException {
        private static final long serialVersionUID = 501117825495079171L;

        public ExistsAccountByIdException(
                final ExistsAccountByIdRequest existsAccountByIdRequest,
                final Throwable cause)
        {
            super(existsAccountByIdRequest.toString(), cause);
        }
    }

    public static class ReadAccountByIdException extends UserAccountServiceException {
        private static final long serialVersionUID = 3053258208065954474L;

        public ReadAccountByIdException(
                final ReadAccountByIdRequest readAccountByIdRequest,
                final Throwable cause)
        {
            super(readAccountByIdRequest.toString(), cause);
        }
    }

    public static class SessionStoreException extends UserAccountServiceException {
        private static final long serialVersionUID = -2139673213088554172L;

        public SessionStoreException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    public static class AccountStoreException extends UserAccountServiceException {
        private static final long serialVersionUID = 3069343794012292474L;

        public AccountStoreException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    public static class EmptyResultException extends UserAccountServiceException {
        private static final long serialVersionUID = 7045959998773382208L;

        public EmptyResultException(final String message) {
            super(message);
        }
    }
}
