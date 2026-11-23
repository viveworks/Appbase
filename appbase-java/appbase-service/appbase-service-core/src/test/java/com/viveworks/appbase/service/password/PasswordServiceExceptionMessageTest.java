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

package com.viveworks.appbase.service.password;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Parsing a malformed password hash must refuse with a message that describes the expected form without
 * echoing the rejected input, which may be a plaintext password or real hash material.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class PasswordServiceExceptionMessageTest {
    private static void assertRefusalDoesNotEchoInput(
            final String input, final String leakMarker, final Runnable parse, final String expectedPhrase)
    {
        final AppbaseRuntimeException thrown = Assertions.assertThrows(AppbaseRuntimeException.class, parse::run);
        final String message = String.valueOf(thrown.getMessage());

        Assertions.assertFalse(message.contains(leakMarker), "message echoed the input: " + message);
        Assertions.assertFalse(message.contains(input), "message echoed the whole input: " + message);
        Assertions.assertTrue(message.contains(expectedPhrase), "message lost its description: " + message);
    }

    @Test
    void malformedSingleHashDoesNotEchoTheInput() {
        final String input = "plaintext-SECRETPW1";

        assertRefusalDoesNotEchoInput(
                input,
                "SECRETPW1",
                () -> PasswordService.deserializePasswordHashImpl(input),
                "Password hash must be of the form");
    }

    @Test
    void malformedHashPairDoesNotEchoTheInput() {
        final String input = "$sha512$rounds=80$cSalt$cHash-SECRETPW2";

        assertRefusalDoesNotEchoInput(
                input,
                "SECRETPW2",
                () -> PasswordService.deserializePasswordHashesImpl(input),
                "Password hashes must be of the form");
    }

    @Test
    void invalidArgon2idStringDoesNotEchoTheInput() {
        final String input = "$argon2id$short-SECRETPW3";

        assertRefusalDoesNotEchoInput(
                input,
                "SECRETPW3",
                () -> PasswordService.deserializePasswordHashImpl(input),
                "Invalid Argon2id PHC string");
    }

    @Test
    void argon2idStringMissingTParameterDoesNotEchoTheInput() {
        final String input = "$argon2id$v=19$m=19456,p=1$SALT-SECRETPW4$HASH-SECRETPW5";

        final AppbaseRuntimeException thrown = Assertions.assertThrows(
                AppbaseRuntimeException.class, () -> PasswordService.deserializePasswordHashImpl(input));
        final String message = String.valueOf(thrown.getMessage());

        Assertions.assertFalse(message.contains("SECRETPW4"), "message echoed the salt: " + message);
        Assertions.assertFalse(message.contains("SECRETPW5"), "message echoed the hash: " + message);
        Assertions.assertTrue(message.contains("Missing t parameter"), "message lost its description: " + message);
    }

    private static void assertNoEchoAnywhere(final Runnable parse, final String leakMarker) {
        final RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, parse::run);

        Throwable current = thrown;
        while (current != null) {
            Assertions.assertFalse(
                    String.valueOf(current.getMessage()).contains(leakMarker),
                    current.getClass().getSimpleName() + " echoed the input: " + current.getMessage());
            current = current.getCause();
        }
    }

    @Test
    void nonNumericRoundsDoesNotEchoTheRoundsText() {
        assertNoEchoAnywhere(
                () -> PasswordService.deserializePasswordHashImpl("$sha512$rounds=abc-SECRETPW6$salt$hash"),
                "SECRETPW6");
    }

    @Test
    void nonNumericRoundsInEitherHashOfAPairDoesNotEchoTheRoundsText() {
        assertNoEchoAnywhere(
                () -> PasswordService.deserializePasswordHashesImpl(
                        "$sha512$rounds=abc-SECRETPW7$cs$ch$sha512$rounds=80$ss$sh"),
                "SECRETPW7");
        assertNoEchoAnywhere(
                () -> PasswordService.deserializePasswordHashesImpl(
                        "$sha512$rounds=80$cs$ch$sha512$rounds=abc-SECRETPW8$ss$sh"),
                "SECRETPW8");
    }

    @Test
    void unknownHashAlgorithmDoesNotEchoTheAlgorithmField() {
        assertNoEchoAnywhere(
                () -> PasswordService.deserializePasswordHashImpl("$bogus-SECRETPW9$rounds=80$salt$hash"),
                "SECRETPW9");
    }

    /**
     * The Argon2id {@code t=} parameter is matched by {@code "t=(\d+)"}, which guarantees digit
     * characters but not a value that fits in {@code int}. An unguarded {@code Integer.parseInt}
     * on an oversized value throws {@link NumberFormatException} with the rejected digits in its
     * own message -- exactly the leak class this file's other {@code rounds} sites are already
     * guarded against via {@code parseRounds}. Flagged against this exact call by GitHub Copilot
     * on internal work item 2571 and never fixed there; reproduced independently by two code-review panel agents
     * on this gate's final re-run.
     */
    @Test
    void argon2idOversizedTParameterDoesNotEchoTheRoundsText() {
        assertNoEchoAnywhere(
                () -> PasswordService.deserializePasswordHashImpl(
                        "$argon2id$v=19$m=19456,t=99999999999999999999,p=1$SALT-SECRETPW10$HASH-SECRETPW11"),
                "99999999999999999999");
        assertNoEchoAnywhere(
                () -> PasswordService.deserializePasswordHashImpl(
                        "$argon2id$v=19$m=19456,t=99999999999999999999,p=1$SALT-SECRETPW10$HASH-SECRETPW11"),
                "SECRETPW10");
    }

    @Test
    void wellFormedHashStillParses() {
        final PasswordHash parsed =
                PasswordService.deserializePasswordHashImpl("$sha512$rounds=80$cSalt$cHash");

        Assertions.assertEquals("cSalt", parsed.getSalt());
        Assertions.assertEquals("cHash", parsed.getHash());
        Assertions.assertEquals(80, parsed.getShaRounds());
    }
}
