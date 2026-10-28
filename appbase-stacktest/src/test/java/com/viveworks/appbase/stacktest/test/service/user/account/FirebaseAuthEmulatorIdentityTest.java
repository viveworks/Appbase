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

package com.viveworks.appbase.stacktest.test.service.user.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.util.Base64;

/**
 * Checks the profile claims required by the account stacktest against a live Auth emulator.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfEnvironmentVariable(named = "FIREBASE_AUTH_EMULATOR_HOST", matches = "[^:]+:[0-9]+")
class FirebaseAuthEmulatorIdentityTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void createdEmailIdentityHasNameAndEmailClaims() throws IOException {
        final FirebaseAuthEmulatorIdentity.Identity identity = FirebaseAuthEmulatorIdentity.createEmailUser();
        final String[] tokenParts = identity.idToken().split("\\.");
        // Auth emulator ID tokens are unsigned, so their trailing signature segment is empty.
        Assertions.assertTrue(tokenParts.length >= 2);
        final JsonNode claims = OBJECT_MAPPER.readTree(Base64.getUrlDecoder().decode(tokenParts[1]));

        Assertions.assertEquals(identity.uid(), claims.path("user_id").asText());
        Assertions.assertEquals(identity.email(), claims.path("email").asText());
        Assertions.assertEquals("name-" + identity.uid(), claims.path("name").asText());
    }
}
