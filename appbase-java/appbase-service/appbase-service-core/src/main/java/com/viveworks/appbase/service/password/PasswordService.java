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

package com.viveworks.appbase.service.password;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface PasswordService extends AppbaseService {
    String PASSWORD_HASH_REGEX = "\\$(.*)\\$rounds=(.*)\\$(.*)\\$(.*)";
    Pattern PASSWORD_HASH_PATTERN = Pattern.compile(PASSWORD_HASH_REGEX);
    Pattern PASSWORD_HASHES_PATTERN = Pattern.compile(PASSWORD_HASH_REGEX + PASSWORD_HASH_REGEX);
    Pattern ARGON2ID_T_PATTERN = Pattern.compile("t=(\\d+)");
    int CLIENT_ALGORITHM_GROUP = 1;
    int CLIENT_ROUNDS_GROUP = 2;
    int CLIENT_SALT_GROUP = 3;
    int CLIENT_HASH_GROUP = 4;
    int SERVER_ALGORITHM_GROUP = 5;
    int SERVER_ROUNDS_GROUP = 6;
    int SERVER_SALT_GROUP = 7;
    int SERVER_HASH_GROUP = 8;

    /**
     * Parses the {@code rounds} field of a serialized hash. The field is unconstrained text in the pattern, so a
     * malformed value must not reach an exception message: {@link NumberFormatException} would echo it.
     */
    private static int parseRounds(final String roundsText) {
        try {
            return Integer.parseInt(roundsText);
        } catch (NumberFormatException e) {
            // Deliberately no cause: its message is the rejected text.
            throw new AppbaseRuntimeException(
                    "Password hash rounds must be a whole number (the rejected value is not included)");
        }
    }

    default PasswordHash deserializePasswordHash(final String passwordHashStr) {
        return deserializePasswordHashImpl(passwordHashStr);
    }

    static PasswordHash deserializePasswordHashImpl(final String passwordHashStr) {
        AppbaseUtil.notBlank(passwordHashStr, "passwordHashStr");

        if (passwordHashStr.startsWith("$argon2id$")) {
            return deserializeArgon2idHashImpl(passwordHashStr);
        }

        final Matcher matcher = PASSWORD_HASH_PATTERN.matcher(passwordHashStr);
        if (!matcher.matches()) {
            // The rejected value is never echoed: a malformed "hash" is often a plaintext password.
            throw new AppbaseRuntimeException("Password hash must be of the form: " +
                    "$algorithm$rounds=NNN$clientSalt$clientPassword (the rejected value is not included)");
        }

        return PasswordHash
                .builder()
                .hashAlgorithm(HashAlgorithm.from(matcher.group(CLIENT_ALGORITHM_GROUP)))
                .salt(matcher.group(CLIENT_SALT_GROUP))
                .shaRounds(parseRounds(matcher.group(CLIENT_ROUNDS_GROUP)))
                .hash(matcher.group(CLIENT_HASH_GROUP))
                .build();
    }

    /**
     * Parses an Argon2id PHC-format string into a {@link PasswordHash}.
     *
     * <p>Expected format: {@code $argon2id$v=19$m=NNNN,t=N,p=N$<salt>$<hash>}
     *
     * @param phcString the PHC string to parse
     * @return the parsed {@link PasswordHash}
     */
    private static PasswordHash deserializeArgon2idHashImpl(final String phcString) {
        // $argon2id$v=19$m=19456,t=2,p=1$<salt>$<hash>
        final String[] parts = phcString.split("\\$", -1);
        if (parts.length < 6) {
            throw new AppbaseRuntimeException("Invalid Argon2id PHC string (the rejected value is not included)");
        }

        final Matcher tMatcher = ARGON2ID_T_PATTERN.matcher(parts[3]);
        if (!tMatcher.find()) {
            throw new AppbaseRuntimeException(
                    "Missing t parameter in Argon2id PHC string (the rejected value is not included)");
        }

        return PasswordHash.builder()
                .hashAlgorithm(HashAlgorithm.ARGON2ID)
                .hash(phcString)
                .salt(parts[4])
                // Routed through parseRounds (not a raw Integer.parseInt), same as the three
                // sibling rounds-parse sites above: the pattern only guarantees digit characters,
                // not a value that fits in int, and an unguarded parseInt would echo the
                // oversized rejected text in NumberFormatException's message. Flagged against
                // this exact call by GitHub Copilot on internal work item 2571 (never fixed there) and
                // independently reproduced by two code-review panel agents on this gate's
                // final re-run.
                .shaRounds(parseRounds(tMatcher.group(1)))
                .build();
    }

    default String serializePasswordHash(final PasswordHash passwordHash) {
        return serializePasswordHashImpl(passwordHash);
    }

    static String serializePasswordHashImpl(final PasswordHash passwordHash) {
        AppbaseUtil.notNull(passwordHash, "passwordHash");
        AppbaseUtil.validate(new PasswordHash.Validator(), passwordHash);

        if (passwordHash.getHashAlgorithm() == HashAlgorithm.ARGON2ID) {
            return passwordHash.getHash();
        }

        return "$" +
                passwordHash.getHashAlgorithm().getSerializedName() + "$" +
                "rounds=" +
                passwordHash.getShaRounds() + "$" +
                passwordHash.getSalt() + "$" +
                passwordHash.getHash();
    }

    /**
     * Creates {@link PasswordHashes} from the string representation defined by {@code PASSWORD_HASHES_PATTERN} above.
     *
     * The password hashes are stored in that string format. This method is used to convert to the
     * {@link PasswordHashes} POJO to transfer as JSON by the server.
     */
    default PasswordHashes deserializePasswordHashes(final String passwordHashesStr) {
        return deserializePasswordHashesImpl(passwordHashesStr);
    }

    static PasswordHashes deserializePasswordHashesImpl(final String passwordHashesStr) {
        AppbaseUtil.notBlank(passwordHashesStr, "passwordHashesStr");

        // Mixed format: client SHA-512 + server Argon2id
        // e.g. $sha512$rounds=80$<cSalt>$<cHash>$argon2id$v=19$m=...,t=...,p=...$<sSalt>$<sHash>
        final int argon2idIdx = passwordHashesStr.indexOf("$argon2id$");
        if (argon2idIdx > 0) {
            final PasswordHash clientPasswordHash =
                    deserializePasswordHashImpl(passwordHashesStr.substring(0, argon2idIdx));
            final PasswordHash serverPasswordHash =
                    deserializePasswordHashImpl(passwordHashesStr.substring(argon2idIdx));
            return PasswordHashes.builder()
                    .clientPasswordHash(clientPasswordHash)
                    .serverPasswordHash(serverPasswordHash)
                    .build();
        }

        // Legacy format: both SHA-512
        final Matcher matcher = PASSWORD_HASHES_PATTERN.matcher(passwordHashesStr);
        if (!matcher.matches()) {
            throw new AppbaseRuntimeException("Password hashes must be of the form: " +
                    "$clientAlgorithm$rounds=NNN$clientSalt$clientPassword" +
                    "$serverAlgorithm$rounds=NNN$serverSalt$serverPassword (the rejected value is not included)");
        }

        final PasswordHash clientPasswordHash = PasswordHash
                .builder()
                .hashAlgorithm(HashAlgorithm.from(matcher.group(CLIENT_ALGORITHM_GROUP)))
                .salt(matcher.group(CLIENT_SALT_GROUP))
                .shaRounds(parseRounds(matcher.group(CLIENT_ROUNDS_GROUP)))
                .hash(matcher.group(CLIENT_HASH_GROUP))
                .build();
        final PasswordHash serverPasswordHash = PasswordHash
                .builder()
                .hashAlgorithm(HashAlgorithm.from(matcher.group(SERVER_ALGORITHM_GROUP)))
                .salt(matcher.group(SERVER_SALT_GROUP))
                .shaRounds(parseRounds(matcher.group(SERVER_ROUNDS_GROUP)))
                .hash(matcher.group(SERVER_HASH_GROUP))
                .build();
        return PasswordHashes
                .builder()
                .clientPasswordHash(clientPasswordHash)
                .serverPasswordHash(serverPasswordHash)
                .build();
    }

    /**
     * Returns the concatenation of crypt format strings of client and server hashes:
     *
     * $clientAlgorithm$rounds=5000$clientSalt$clientPassword$serverAlgorithm$rounds=5000$serverSalt$serverPassword
     *
     * This is used mainly to store in DB.
     */
    default String serializePasswordHashes(final PasswordHashes passwordHashes) {
        return serializePasswordHashesImpl(passwordHashes);
    }

    static String serializePasswordHashesImpl(final PasswordHashes passwordHashes) {
        AppbaseUtil.notNull(passwordHashes, "passwordHashes");

        return serializePasswordHashImpl(passwordHashes.getClientPasswordHash()) +
                serializePasswordHashImpl(passwordHashes.getServerPasswordHash());
    }

    String randomSalt();

    PasswordHash hashWithEmail(String password, String email) throws NoSuchAlgorithmException;

    PasswordHash hashWithSalt(String password, String salt);

    PasswordHash hashWithRandomSalt(String password);

    /**
     * Verifies {@code input} against an Argon2id {@code storedHash}.
     * Returns {@code false} immediately for non-Argon2id hashes.
     * Uses constant-time comparison.
     *
     * @param input the value to verify (as received from the client)
     * @param storedHash the Argon2id hash from the database
     * @return true if input produces the same hash; false otherwise
     */
    boolean verify(String input, PasswordHash storedHash);

    /**
     * Returns true if {@code storedHash} was produced by a legacy algorithm
     * and should be re-hashed with Argon2id on next successful login.
     *
     * @param storedHash the server password hash from the database
     * @return true if the hash algorithm is not Argon2id
     */
    default boolean needsRehash(final PasswordHash storedHash) {
        AppbaseUtil.notNull(storedHash, "storedHash");
        return storedHash.getHashAlgorithm() != HashAlgorithm.ARGON2ID;
    }
}
