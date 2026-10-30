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

package com.viveworks.appbase.service.user.testhelper;

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.service.user.account.data.AfterIdpGuestSignInRequest;
import com.viveworks.appbase.service.user.account.data.GuestUser;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.data.UserSessionData;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import static com.viveworks.appbase.service.user.account.UserAccountService.APP_NAME_KEY;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AppbaseServiceTestHelper {
    public static void assertTimestampMinutes(final LocalDateTime expected, final LocalDateTime actual) {
        assertEqual(expected.getDayOfMonth(), actual.getDayOfMonth());
        assertEqual(expected.getMonth(), actual.getMonth());
        assertEqual(expected.getYear(), actual.getYear());
        if (expected.getMinute() < 58) {
            assertEqual(expected.getHour(), actual.getHour());
            if (expected.getSecond() < 58) {
                assertEqual(expected.getMinute(), actual.getMinute());
            }
        }
    }

    public static void assertUserAccount(final UserAccount expected, final UserAccount actual) {
        assertEqual(expected.getAppbaseUserId(), actual.getAppbaseUserId());
        assertEqual(expected.getIdpUserId(), actual.getIdpUserId());
        assertEqual(expected.getEmail(), actual.getEmail());
        assertEqual(expected.getEmailVerified(), actual.getEmailVerified());
        assertEqual(expected.getUserTopic(), actual.getUserTopic());
        assertEqual(expected.getUserHomeDataCenter(), actual.getUserHomeDataCenter());
        assertEqual(expected.getLanguageCode(), actual.getLanguageCode());
        assertEqual(expected.getAddressLine1(), actual.getAddressLine1());
        assertEqual(expected.getAddressLine2(), actual.getAddressLine2());
        assertEqual(expected.getCity(), actual.getCity());
        assertEqual(expected.getStateProvince(), actual.getStateProvince());
        assertEqual(expected.getPostalCode(), actual.getPostalCode());
        assertEqual(expected.getCountry(), actual.getCountry());
        assertEqual(expected.getLatitude(), actual.getLatitude());
        assertEqual(expected.getLongitude(), actual.getLongitude());
        assertEqual(expected.getDeleted(), actual.getDeleted());
        assertTimestampMinutes(expected.getCreationTimestamp(), actual.getCreationTimestamp());
        assertTimestampMinutes(expected.getLastAccessTimestamp(), actual.getLastAccessTimestamp());
    }

    public static void assertUnauthenticatedUserSession(final UserSession expected, final UserSession actual) {
        assertNotNull(expected);
        assertNotNull(actual);

        assertEqual(expected.getUserSessionData(), actual.getUserSessionData());
        assertTimestampMinutes(expected.getCreationTimestamp(), actual.getCreationTimestamp());
        assertTimestampMinutes(expected.getLastAccessTimestamp(), actual.getLastAccessTimestamp());
    }

    public static void assertUserSession(final UserSession expected, final UserSession actual) {
        assertNotNull(expected);
        assertNotNull(actual);

        assertEqual(expected.getAppbaseSessionId(), actual.getAppbaseSessionId());
        assertEqual(expected.getIdpUserToken(), actual.getIdpUserToken());
        assertEqual(expected.getUserSessionData(), actual.getUserSessionData());
        assertTimestampMinutes(expected.getCreationTimestamp(), actual.getCreationTimestamp());
        assertTimestampMinutes(expected.getLastAccessTimestamp(), actual.getLastAccessTimestamp());

        final GuestUser expectedGuestUser = expected.getGuestUser();
        final GuestUser actualGuestUser = actual.getGuestUser();
        if (expectedGuestUser != null) {
            assertNotNull(actualGuestUser);
            assertGuestUser(expectedGuestUser, actualGuestUser);
        } else {
            assertNull(actualGuestUser);
            assertUserAccount(expected.getUserAccount(), actual.getUserAccount());
        }
    }

    public static void assertGuestUser(final GuestUser expected, final GuestUser actual) {
        assertEqual(expected.getAppbaseUserId(), actual.getAppbaseUserId());
        assertEqual(expected.getIdpGuestId(), actual.getIdpGuestId());
        assertEqual(expected.getLanguageCode(), actual.getLanguageCode());
        assertEqual(expected.getName(), actual.getName());
        assertTimestampMinutes(expected.getCreationTimestamp(), actual.getCreationTimestamp());
        assertTimestampMinutes(expected.getLastAccessTimestamp(), actual.getLastAccessTimestamp());
    }

    public static void assertAppNameInSessionData(
            final AfterIdpGuestSignInRequest afterIdpGuestSignInRequest, final UserSession userSession)
    {
        assertNotNull(afterIdpGuestSignInRequest);
        assertNotNull(userSession);

        final String appName = afterIdpGuestSignInRequest.getAppName();
        final UserSessionData userSessionData1 = userSession.getUserSessionData();

        final Map<String, String> dataMap1 = userSessionData1.getDataMap();
        if (appName != null) {
            if (!dataMap1.containsKey(APP_NAME_KEY)) {
                throw new AssertionError("expected key <" + APP_NAME_KEY + "> in session data map");
            }
            assertEqual(appName, dataMap1.get(APP_NAME_KEY));
        }
    }

    public static AppbaseServiceEntry awaitServiceStartedStatus(
            final ServiceRegistry serviceRegistry,
            final Class<? extends AppbaseService> appbaseServiceType,
            final AppbaseServiceEntry appbaseServiceEntry)
            throws InterruptedException
    {
        AppbaseServiceEntry appbaseServiceEntry1 = appbaseServiceEntry;

        for (int i = 0; i < 5; ++i) {
            if (appbaseServiceEntry1.getServiceStatus() == ServiceStatus.STARTING) {
                serviceRegistry.pollAndUpdateStatuses();
                appbaseServiceEntry1 = serviceRegistry.getAppbaseService(appbaseServiceType);
                Thread.sleep(1000);
            } else {
                break;
            }
        }

        return appbaseServiceEntry1;
    }

    private static void assertEqual(final Object expected, final Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected: <" + expected + "> but was: <" + actual + ">");
        }
    }

    private static void assertNotNull(final Object value) {
        if (value == null) {
            throw new AssertionError("expected: not <null>");
        }
    }

    private static void assertNull(final Object value) {
        if (value != null) {
            throw new AssertionError("expected: <null> but was: <" + value + ">");
        }
    }
}
