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

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.service.testhelper.AppbaseServiceCoreTestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultPasswordServiceTest {
    private static AppbaseCore appbaseCore;
    private static DefaultPasswordService defaultPasswordService;

    @BeforeAll
    static void setUp() {
        System.setProperty(
                ClientCoreTestHelper.CORE_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-core.yaml");
        System.setProperty(
                ClientCoreTestHelper.CLIENT_CORE_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-client-core.yaml");

        appbaseCore = ClientCoreTestHelper.appbaseCore();
        defaultPasswordService = new DefaultPasswordService(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                new DefaultPasswordServiceConfiguration());
    }

    @AfterAll
    static void tearDown() {
        if (defaultPasswordService != null) {
            defaultPasswordService.shutdown();
        }

        if (appbaseCore != null) {
            appbaseCore.shutdown();
        }
    }

    @Test
    void testRegisterService() throws InterruptedException {
        final ServiceRegistry serviceRegistry = appbaseCore.getServiceRegistry();

        AppbaseServiceEntry appbaseServiceEntry1 = serviceRegistry.getAppbaseService(PasswordService.class);
        Assertions.assertNotNull(appbaseServiceEntry1);

        appbaseServiceEntry1 = AppbaseServiceCoreTestHelper.awaitServiceStartedStatus(
                serviceRegistry,
                PasswordService.class,
                appbaseServiceEntry1);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry1.getServiceStatus());
        Assertions.assertEquals(defaultPasswordService, appbaseServiceEntry1.getService());

        final AppbaseServiceEntry appbaseServiceEntry2 =
                serviceRegistry.getAppbaseService(PasswordService.class);
        Assertions.assertNotNull(appbaseServiceEntry2);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry2.getServiceStatus());
        Assertions.assertEquals(defaultPasswordService, appbaseServiceEntry2.getService());
    }

    @Test
    void testRandomSalt() {
        final String salt1 = defaultPasswordService.randomSalt();
        Assertions.assertNotNull(salt1);

        final String salt2 = defaultPasswordService.randomSalt();
        Assertions.assertNotNull(salt1);

        Assertions.assertNotEquals(salt1, salt2);
    }

    @Test
    void testRandomSaltUniquenessAndPerformance() {
        final int count = 1000;
        // Capacity sized to avoid rehashing: HashSet uses 0.75 load factor.
        final Set<String> salts = new HashSet<>((int) (count / 0.75) + 1);

        final long startNs = System.nanoTime();
        for (int i = 0; i < count; i++) {
            salts.add(defaultPasswordService.randomSalt());
        }
        final long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;

        Assertions.assertEquals(count, salts.size(), "All 1000 salts must be unique");
        // Shared SecureRandom instance should complete 1000 calls well under 500ms total.
        // A per-call new SecureRandom() with OS entropy seeding takes ~5–100ms each,
        // so 1000 calls would far exceed this budget on entropy-constrained systems.
        Assertions.assertTrue(
                elapsedMs < 500L,
                "Expected 1000 salts in < 500ms total, got " + elapsedMs + "ms");
    }

    @Test
    void testHashWithEmail() {
        final String password = "password123";
        final String email = "appbase@service.dev";

        final PasswordHash passwordHash = defaultPasswordService.hashWithEmail(password, email);
        Assertions.assertNotNull(passwordHash);
        Assertions.assertEquals(HashAlgorithm.SHA512, passwordHash.getHashAlgorithm());
        Assertions.assertEquals(DefaultPasswordService.SHA512_ROUNDS, passwordHash.getShaRounds());
        Assertions.assertEquals(email.toLowerCase(), passwordHash.getSalt());
        Assertions.assertNotEquals(password, passwordHash.getHash());
    }

    @Test
    void testHashWithSalt() {
        final String password = "password123";
        final String salt = "salt123";

        final PasswordHash passwordHash = defaultPasswordService.hashWithSalt(password, salt);
        Assertions.assertNotNull(passwordHash);
        Assertions.assertEquals(salt, passwordHash.getSalt());
        Assertions.assertEquals(HashAlgorithm.SHA512, passwordHash.getHashAlgorithm());
        Assertions.assertEquals(DefaultPasswordService.SHA512_ROUNDS, passwordHash.getShaRounds());
        Assertions.assertNotEquals(password, passwordHash.getHash());
    }

    @Test
    void testHashWithRandomSalt() {
        final String password = "password123";

        final PasswordHash passwordHash1 = defaultPasswordService.hashWithRandomSalt(password);
        Assertions.assertNotNull(passwordHash1);
        Assertions.assertEquals(HashAlgorithm.ARGON2ID, passwordHash1.getHashAlgorithm());
        Assertions.assertEquals(DefaultPasswordServiceConfiguration.DEFAULT_ITERATIONS, passwordHash1.getShaRounds());
        Assertions.assertTrue(passwordHash1.getHash().startsWith("$argon2id$"));
        Assertions.assertNotNull(passwordHash1.getSalt());
        Assertions.assertFalse(passwordHash1.getSalt().isBlank());

        final PasswordHash passwordHash2 = defaultPasswordService.hashWithRandomSalt(password);
        Assertions.assertNotNull(passwordHash2);
        Assertions.assertEquals(HashAlgorithm.ARGON2ID, passwordHash2.getHashAlgorithm());
        Assertions.assertNotEquals(passwordHash1.getSalt(), passwordHash2.getSalt());
        Assertions.assertNotEquals(passwordHash1.getHash(), passwordHash2.getHash());
    }

    @Test
    void testHashWithRandomSaltPhcFormat() {
        final PasswordHash hash = defaultPasswordService.hashWithRandomSalt("secret");

        // PHC string: $argon2id$v=19$m=19456,t=2,p=1$<salt>$<hash>
        final String phc = hash.getHash();
        Assertions.assertTrue(phc.startsWith("$argon2id$v=19$"));
        Assertions.assertTrue(phc.contains("$m=" + DefaultPasswordServiceConfiguration.DEFAULT_MEMORY_KIB + ","));
        Assertions.assertTrue(phc.contains(",t=" + DefaultPasswordServiceConfiguration.DEFAULT_ITERATIONS + ","));
        Assertions.assertTrue(phc.contains(",p=" + DefaultPasswordServiceConfiguration.DEFAULT_PARALLELISM + "$"));

        final String[] parts = phc.split("\\$", -1);
        // ["", "argon2id", "v=19", "m=19456,t=2,p=1", "<salt>", "<hash>"]
        Assertions.assertEquals(6, parts.length);
        Assertions.assertFalse(parts[4].isBlank()); // salt
        Assertions.assertFalse(parts[5].isBlank()); // hash
    }

    @Test
    void testSerializePasswordHashArgon2id() {
        final PasswordHash hash = defaultPasswordService.hashWithRandomSalt("secret");
        final String serialized = defaultPasswordService.serializePasswordHash(hash);

        // Argon2id serializes as the PHC string directly
        Assertions.assertEquals(hash.getHash(), serialized);
        Assertions.assertTrue(serialized.startsWith("$argon2id$"));
    }

    @Test
    void testDeserializePasswordHashArgon2id() {
        final PasswordHash original = defaultPasswordService.hashWithRandomSalt("secret");
        final String phc = original.getHash();

        final PasswordHash deserialized = defaultPasswordService.deserializePasswordHash(phc);

        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals(HashAlgorithm.ARGON2ID, deserialized.getHashAlgorithm());
        Assertions.assertEquals(phc, deserialized.getHash());
        Assertions.assertEquals(original.getSalt(), deserialized.getSalt());
        Assertions.assertEquals(DefaultPasswordServiceConfiguration.DEFAULT_ITERATIONS, deserialized.getShaRounds());
    }

    @Test
    void testVerifyArgon2idCorrectPassword() {
        final String password = "s3cr3tP@ssw0rd!";
        final PasswordHash storedHash = defaultPasswordService.hashWithRandomSalt(password);
        Assertions.assertTrue(defaultPasswordService.verify(password, storedHash));
    }

    @Test
    void testVerifyArgon2idWrongPassword() {
        final PasswordHash storedHash = defaultPasswordService.hashWithRandomSalt("correct-password");
        Assertions.assertFalse(defaultPasswordService.verify("wrong-password", storedHash));
    }

    @Test
    void testVerifyArgon2idCaseSensitive() {
        final PasswordHash storedHash = defaultPasswordService.hashWithRandomSalt("Password");
        Assertions.assertFalse(defaultPasswordService.verify("password", storedHash));
    }

    @Test
    void testVerifyArgon2idSha512StoredHashReturnsFalse() {
        // verify() must return false (not throw) for legacy SHA-512 hashes per interface contract
        final PasswordHash sha512Hash = PasswordHash.builder()
                .hashAlgorithm(HashAlgorithm.SHA512)
                .hash(defaultPasswordService.randomSalt())
                .salt(defaultPasswordService.randomSalt())
                .shaRounds(DefaultPasswordService.SHA512_ROUNDS)
                .build();
        Assertions.assertFalse(defaultPasswordService.verify("anyInput", sha512Hash));
    }

    /**
     * L2-10 (CWE-400/CWE-770): verification trusts the memory, iteration and parallelism
     * values -- plus the salt and hash segment lengths -- embedded in the persisted PHC
     * string. A malformed or attacker-planted credential record must be refused by an
     * explicit verification policy BEFORE Argon2 is initialised or anything is allocated
     * from the supplied segments; login-time verification must not become a
     * denial-of-service primitive for whoever controls the backing store.
     */
    private static PasswordHash argon2idPhcHash(final String paramSegment,
                                               final String saltBase64,
                                               final String hashBase64) {
        return PasswordHash.builder()
                .hashAlgorithm(HashAlgorithm.ARGON2ID)
                .hash("$argon2id$v=19$" + paramSegment + "$" + saltBase64 + "$" + hashBase64)
                .salt(saltBase64)
                .shaRounds(DefaultPasswordServiceConfiguration.DEFAULT_ITERATIONS)
                .build();
    }

    @Test
    @org.junit.jupiter.api.Timeout(10)
    void verifyRejectsOutOfPolicyPhcBeforeArgonAllocation() {
        // Pre-fix this arm does not merely fail an assertion: m=2147483647 KiB (~2 TiB) drives
        // BouncyCastle to allocate until the whole surefire fork is OOM-killed (two fork
        // dumpstreams recorded, 2026-10-10T00-15-46 and 00-17-53). Post-fix the refusal
        // precedes any Argon2 allocation, so the arm is safe to keep live.
        final PasswordHash planted = argon2idPhcHash("m=2147483647,t=2,p=1", "c2FsdHNlZ3M", "aGFzaGVnZw");
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> defaultPasswordService.verify("pw", planted),
                "a ~2 TiB memory parameter must be refused before Argon2 allocates for it");
    }

    @Test
    @org.junit.jupiter.api.Timeout(10)
    void verifyRejectsExtremeIterationsAndParallelism() {
        // Values sit just past the accepted policy envelope and are chosen so the pre-fix code
        // still executes quickly (and returns false or throws a parse error). The unbounded
        // extreme direction -- a memory parameter asking for ~2 TiB -- is covered separately
        // by verifyRejectsOutOfPolicyPhcBeforeArgonAllocation, whose pre-fix behavior is to
        // exhaust the surefire fork's heap outright.
        for (final String paramSegment : new String[] {
                "m=19456,t=101,p=1",
                "m=19456,t=2,p=17",
                "m=19456,t=0,p=1",
                "m=19456,t=2,p=0",
                "m=0,t=2,p=1"}) {
            final PasswordHash planted = argon2idPhcHash(paramSegment, "c2FsdHNlZ3M", "aGFzaGVnZw");
            Assertions.assertThrows(AppbaseRuntimeException.class,
                    () -> defaultPasswordService.verify("pw", planted),
                    "parameter segment '" + paramSegment + "' is outside the verification policy");
        }
    }

    @Test
    @org.junit.jupiter.api.Timeout(10)
    void verifyRejectsOversizedSaltAndHashSegments() {
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> defaultPasswordService.verify("pw",
                        argon2idPhcHash("m=19456,t=2,p=1", "A".repeat(512), "aGFzaGVnZw")),
                "an oversized salt segment must be refused before it is decoded");
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> defaultPasswordService.verify("pw",
                        argon2idPhcHash("m=19456,t=2,p=1", "c2FsdHNlZ3M", "A".repeat(4096))),
                "an oversized hash segment must be refused before it sizes any allocation");
    }

    @Test
    @org.junit.jupiter.api.Timeout(10)
    void verifyRejectsDuplicateAndUnknownPhcParameters() {
        for (final String paramSegment : new String[] {
                "m=19456,t=2,t=3,p=1",
                "m=19456,t=2,p=1,x=9",
                "m=19456,t=2",
                "m=19456,,t=2,p=1",
                "m=abc,t=2,p=1",
                "m=-1,t=2,p=1"}) {
            final PasswordHash planted = argon2idPhcHash(paramSegment, "c2FsdHNlZ3M", "aGFzaGVnZw");
            Assertions.assertThrows(AppbaseRuntimeException.class,
                    () -> defaultPasswordService.verify("pw", planted),
                    "parameter segment '" + paramSegment + "' must be refused by the strict PHC grammar");
        }
    }

    @Test
    @org.junit.jupiter.api.Timeout(30)
    void verifyAcceptsNonDefaultInPolicyParameters() throws Exception {
        // Guard against over-correction: params stronger than the defaults but within the
        // verification policy must still round-trip TRUE (computed here with the same
        // BouncyCastle primitives the service uses, config-independent as verify() is).
        final byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        final org.bouncycastle.crypto.generators.Argon2BytesGenerator generator =
                new org.bouncycastle.crypto.generators.Argon2BytesGenerator();
        generator.init(new org.bouncycastle.crypto.params.Argon2Parameters.Builder(
                        org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_id)
                .withVersion(org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(salt)
                .withMemoryAsKB(65536)
                .withIterations(3)
                .withParallelism(4)
                .build());
        final byte[] hash = new byte[64];
        generator.generateBytes("round-trip".getBytes(java.nio.charset.StandardCharsets.UTF_8), hash);
        final java.util.Base64.Encoder encoder = java.util.Base64.getEncoder().withoutPadding();

        final PasswordHash stored = argon2idPhcHash("m=65536,t=3,p=4",
                encoder.encodeToString(salt), encoder.encodeToString(hash));

        Assertions.assertTrue(defaultPasswordService.verify("round-trip", stored),
                "in-policy non-default parameters must verify normally");
        Assertions.assertFalse(defaultPasswordService.verify("wrong", stored));
    }

    @Test
    void testNeedsRehashArgon2id() {
        final PasswordHash argon2idHash = defaultPasswordService.hashWithRandomSalt("password");
        Assertions.assertFalse(defaultPasswordService.needsRehash(argon2idHash));
    }

    @Test
    void testNeedsRehashSha512() {
        final PasswordHash sha512Hash = PasswordHash.builder()
                .hashAlgorithm(HashAlgorithm.SHA512)
                .hash(defaultPasswordService.randomSalt())
                .salt(defaultPasswordService.randomSalt())
                .shaRounds(DefaultPasswordService.SHA512_ROUNDS)
                .build();
        Assertions.assertTrue(defaultPasswordService.needsRehash(sha512Hash));
    }

    @Test
    void testDeserializePasswordHashesMixed() {
        final String rounds = String.valueOf(DefaultPasswordService.SHA512_ROUNDS);
        final String cSalt = defaultPasswordService.randomSalt();
        final String cHash = defaultPasswordService.randomSalt();
        final String clientStr = "$" + HashAlgorithm.SHA512.getSerializedName() +
                "$rounds=" + rounds + "$" + cSalt + "$" + cHash;

        final PasswordHash serverHash = defaultPasswordService.hashWithRandomSalt("serverSecret");
        final String serverPhc = defaultPasswordService.serializePasswordHash(serverHash);

        final PasswordHashes passwordHashes =
                defaultPasswordService.deserializePasswordHashes(clientStr + serverPhc);

        Assertions.assertEquals(HashAlgorithm.SHA512, passwordHashes.getClientPasswordHash().getHashAlgorithm());
        Assertions.assertEquals(cHash, passwordHashes.getClientPasswordHash().getHash());
        Assertions.assertEquals(cSalt, passwordHashes.getClientPasswordHash().getSalt());
        Assertions.assertEquals(HashAlgorithm.ARGON2ID, passwordHashes.getServerPasswordHash().getHashAlgorithm());
        Assertions.assertEquals(serverPhc, passwordHashes.getServerPasswordHash().getHash());
    }

    @Test
    void testSerializePasswordHashesMixed() {
        final String cSalt = defaultPasswordService.randomSalt();
        final String cHash = defaultPasswordService.randomSalt();
        final PasswordHash clientHash = PasswordHash.builder()
                .hashAlgorithm(HashAlgorithm.SHA512)
                .hash(cHash)
                .salt(cSalt)
                .shaRounds(DefaultPasswordService.SHA512_ROUNDS)
                .build();
        final PasswordHash serverHash = defaultPasswordService.hashWithRandomSalt("serverSecret");

        final PasswordHashes passwordHashes = PasswordHashes.builder()
                .clientPasswordHash(clientHash)
                .serverPasswordHash(serverHash)
                .build();

        final String serialized = defaultPasswordService.serializePasswordHashes(passwordHashes);

        final String expectedClientPart = "$" + HashAlgorithm.SHA512.getSerializedName() +
                "$rounds=" + DefaultPasswordService.SHA512_ROUNDS + "$" + cSalt + "$" + cHash;
        Assertions.assertEquals(expectedClientPart + serverHash.getHash(), serialized);
    }

    @Test
    void testDeserializePasswordHash() {
        final String salt1 = defaultPasswordService.randomSalt();
        final String password = defaultPasswordService.randomSalt();
        final String rounds = String.valueOf(DefaultPasswordService.SHA512_ROUNDS);
        final String passwordHashStr =
                "$" + HashAlgorithm.SHA512.getSerializedName() +
                "$rounds=" + rounds +
                "$" + salt1 +
                "$" + password;

        final PasswordHash passwordHash = defaultPasswordService.deserializePasswordHash(passwordHashStr);
        Assertions.assertNotNull(passwordHash);
        Assertions.assertEquals(HashAlgorithm.SHA512, passwordHash.getHashAlgorithm());
        Assertions.assertEquals(DefaultPasswordService.SHA512_ROUNDS, passwordHash.getShaRounds());
        Assertions.assertEquals(password, passwordHash.getHash());
        Assertions.assertEquals(salt1, passwordHash.getSalt());
    }

    @Test
    void testDeserializePasswordHashImpl() {
        final String salt1 = defaultPasswordService.randomSalt();
        final String password = defaultPasswordService.randomSalt();
        final String rounds = String.valueOf(DefaultPasswordService.SHA512_ROUNDS);
        final String passwordHashStr =
                "$" + HashAlgorithm.SHA512.getSerializedName() +
                        "$rounds=" + rounds +
                        "$" + salt1 +
                        "$" + password;

        final PasswordHash passwordHash = PasswordService.deserializePasswordHashImpl(passwordHashStr);
        Assertions.assertNotNull(passwordHash);
        Assertions.assertEquals(HashAlgorithm.SHA512, passwordHash.getHashAlgorithm());
        Assertions.assertEquals(DefaultPasswordService.SHA512_ROUNDS, passwordHash.getShaRounds());
        Assertions.assertEquals(password, passwordHash.getHash());
        Assertions.assertEquals(salt1, passwordHash.getSalt());
    }

    @Test
    void testSerializePasswordHash() {
        final String salt1 = defaultPasswordService.randomSalt();
        final String password = defaultPasswordService.randomSalt();
        final PasswordHash passwordHash = PasswordHash
                .builder()
                .salt(salt1)
                .hash(password)
                .shaRounds(DefaultPasswordService.SHA512_ROUNDS)
                .hashAlgorithm(HashAlgorithm.SHA512)
                .build();

        final String serializePasswordHash = defaultPasswordService.serializePasswordHash(passwordHash);
        Assertions.assertNotNull(serializePasswordHash);

        final String expected = "$" + HashAlgorithm.SHA512.getSerializedName() +
                "$rounds=" + DefaultPasswordService.SHA512_ROUNDS +
                "$" + salt1 +
                "$" + password;
        Assertions.assertEquals(expected, serializePasswordHash);
    }

    @Test
    void testSerializePasswordHashImpl() {
        final String salt1 = defaultPasswordService.randomSalt();
        final String password = defaultPasswordService.randomSalt();
        final PasswordHash passwordHash = PasswordHash
                .builder()
                .salt(salt1)
                .hash(password)
                .shaRounds(DefaultPasswordService.SHA512_ROUNDS)
                .hashAlgorithm(HashAlgorithm.SHA512)
                .build();

        final String serializePasswordHash = PasswordService.serializePasswordHashImpl(passwordHash);
        Assertions.assertNotNull(serializePasswordHash);

        final String expected = "$" + HashAlgorithm.SHA512.getSerializedName() +
                "$rounds=" + DefaultPasswordService.SHA512_ROUNDS +
                "$" + salt1 +
                "$" + password;
        Assertions.assertEquals(expected, serializePasswordHash);
    }

    @Test
    void testDeserializePasswordHashes() {
        final String rounds = String.valueOf(DefaultPasswordService.SHA512_ROUNDS);

        final String salt1 = defaultPasswordService.randomSalt();
        final String password1 = defaultPasswordService.randomSalt();
        final String clientPasswordHashStr1 =
                "$" + HashAlgorithm.SHA512.getSerializedName() +
                        "$rounds=" + rounds +
                        "$" + salt1 +
                        "$" + password1;

        final String salt2 = defaultPasswordService.randomSalt();
        final String password2 = defaultPasswordService.randomSalt();
        final String severPasswordHashStr2 =
                "$" + HashAlgorithm.SHA512.getSerializedName() +
                        "$rounds=" + rounds +
                        "$" + salt2 +
                        "$" + password2;

        final String passwordHashesStr = clientPasswordHashStr1 + severPasswordHashStr2;
        final PasswordHashes passwordHashes = defaultPasswordService.deserializePasswordHashes(passwordHashesStr);

        final PasswordHash clientPasswordHash = passwordHashes.getClientPasswordHash();
        Assertions.assertNotNull(clientPasswordHash);
        Assertions.assertEquals(HashAlgorithm.SHA512, clientPasswordHash.getHashAlgorithm());
        Assertions.assertEquals(DefaultPasswordService.SHA512_ROUNDS, clientPasswordHash.getShaRounds());
        Assertions.assertEquals(password1, clientPasswordHash.getHash());
        Assertions.assertEquals(salt1, clientPasswordHash.getSalt());


        final PasswordHash serverPasswordHash = passwordHashes.getServerPasswordHash();
        Assertions.assertNotNull(serverPasswordHash);
        Assertions.assertEquals(HashAlgorithm.SHA512, serverPasswordHash.getHashAlgorithm());
        Assertions.assertEquals(DefaultPasswordService.SHA512_ROUNDS, serverPasswordHash.getShaRounds());
        Assertions.assertEquals(password2, serverPasswordHash.getHash());
        Assertions.assertEquals(salt2, serverPasswordHash.getSalt());
    }

    @Test
    void testDeserializePasswordHashesImpl() {
        final String rounds = String.valueOf(DefaultPasswordService.SHA512_ROUNDS);

        final String salt1 = defaultPasswordService.randomSalt();
        final String password1 = defaultPasswordService.randomSalt();
        final String clientPasswordHashStr1 =
                "$" + HashAlgorithm.SHA512.getSerializedName() +
                        "$rounds=" + rounds +
                        "$" + salt1 +
                        "$" + password1;

        final String salt2 = defaultPasswordService.randomSalt();
        final String password2 = defaultPasswordService.randomSalt();
        final String severPasswordHashStr2 =
                "$" + HashAlgorithm.SHA512.getSerializedName() +
                        "$rounds=" + rounds +
                        "$" + salt2 +
                        "$" + password2;

        final String passwordHashesStr = clientPasswordHashStr1 + severPasswordHashStr2;
        final PasswordHashes passwordHashes = PasswordService.deserializePasswordHashesImpl(passwordHashesStr);

        final PasswordHash clientPasswordHash = passwordHashes.getClientPasswordHash();
        Assertions.assertNotNull(clientPasswordHash);
        Assertions.assertEquals(HashAlgorithm.SHA512, clientPasswordHash.getHashAlgorithm());
        Assertions.assertEquals(DefaultPasswordService.SHA512_ROUNDS, clientPasswordHash.getShaRounds());
        Assertions.assertEquals(password1, clientPasswordHash.getHash());
        Assertions.assertEquals(salt1, clientPasswordHash.getSalt());


        final PasswordHash serverPasswordHash = passwordHashes.getServerPasswordHash();
        Assertions.assertNotNull(serverPasswordHash);
        Assertions.assertEquals(HashAlgorithm.SHA512, serverPasswordHash.getHashAlgorithm());
        Assertions.assertEquals(DefaultPasswordService.SHA512_ROUNDS, serverPasswordHash.getShaRounds());
        Assertions.assertEquals(password2, serverPasswordHash.getHash());
        Assertions.assertEquals(salt2, serverPasswordHash.getSalt());
    }

    @Test
    void testSerializePasswordHashes() {
        final String salt1 = defaultPasswordService.randomSalt();
        final String password1 = defaultPasswordService.randomSalt();
        final PasswordHash clientPasswordHash = PasswordHash
                .builder()
                .salt(salt1)
                .hash(password1)
                .shaRounds(DefaultPasswordService.SHA512_ROUNDS)
                .hashAlgorithm(HashAlgorithm.SHA512)
                .build();

        final String salt2 = defaultPasswordService.randomSalt();
        final String password2 = defaultPasswordService.randomSalt();
        final PasswordHash serverPasswordHash = PasswordHash
                .builder()
                .salt(salt2)
                .hash(password2)
                .shaRounds(DefaultPasswordService.SHA512_ROUNDS)
                .hashAlgorithm(HashAlgorithm.SHA512)
                .build();

        final PasswordHashes passwordHashes = PasswordHashes
                .builder()
                .clientPasswordHash(clientPasswordHash)
                .serverPasswordHash(serverPasswordHash)
                .build();
        final String serializePasswordHashes = defaultPasswordService.serializePasswordHashes(passwordHashes);

        final String rounds = String.valueOf(DefaultPasswordService.SHA512_ROUNDS);
        final String clientPasswordHashStr =
                "$" + HashAlgorithm.SHA512.getSerializedName() +
                        "$rounds=" + rounds +
                        "$" + salt1 +
                        "$" + password1;
        final String severPasswordHashStr =
                "$" + HashAlgorithm.SHA512.getSerializedName() +
                        "$rounds=" + rounds +
                        "$" + salt2 +
                        "$" + password2;
        final String expected = clientPasswordHashStr + severPasswordHashStr;

        Assertions.assertEquals(expected, serializePasswordHashes);
    }

    @Test
    void testSerializePasswordHashesImpl() {
        final String salt1 = defaultPasswordService.randomSalt();
        final String password1 = defaultPasswordService.randomSalt();
        final PasswordHash clientPasswordHash = PasswordHash
                .builder()
                .salt(salt1)
                .hash(password1)
                .shaRounds(DefaultPasswordService.SHA512_ROUNDS)
                .hashAlgorithm(HashAlgorithm.SHA512)
                .build();

        final String salt2 = defaultPasswordService.randomSalt();
        final String password2 = defaultPasswordService.randomSalt();
        final PasswordHash serverPasswordHash = PasswordHash
                .builder()
                .salt(salt2)
                .hash(password2)
                .shaRounds(DefaultPasswordService.SHA512_ROUNDS)
                .hashAlgorithm(HashAlgorithm.SHA512)
                .build();

        final PasswordHashes passwordHashes = PasswordHashes
                .builder()
                .clientPasswordHash(clientPasswordHash)
                .serverPasswordHash(serverPasswordHash)
                .build();
        final String serializePasswordHashes = PasswordService.serializePasswordHashesImpl(passwordHashes);

        final String rounds = String.valueOf(DefaultPasswordService.SHA512_ROUNDS);
        final String clientPasswordHashStr =
                "$" + HashAlgorithm.SHA512.getSerializedName() +
                        "$rounds=" + rounds +
                        "$" + salt1 +
                        "$" + password1;
        final String severPasswordHashStr =
                "$" + HashAlgorithm.SHA512.getSerializedName() +
                        "$rounds=" + rounds +
                        "$" + salt2 +
                        "$" + password2;
        final String expected = clientPasswordHashStr + severPasswordHashStr;

        Assertions.assertEquals(expected, serializePasswordHashes);
    }

    private void assertVerifyDoesNotEchoStoredHash(final String storedPhc, final String leakMarker) {
        final PasswordHash malformed = PasswordHash.builder()
                .hashAlgorithm(HashAlgorithm.ARGON2ID)
                .hash(storedPhc)
                .salt("salt")
                .shaRounds(1)
                .build();

        final RuntimeException thrown = Assertions.assertThrows(
                RuntimeException.class, () -> defaultPasswordService.verify("password", malformed));

        Throwable current = thrown;
        while (current != null) {
            Assertions.assertFalse(
                    String.valueOf(current.getMessage()).contains(leakMarker),
                    current.getClass().getSimpleName() + " echoed the stored hash: " + current.getMessage());
            current = current.getCause();
        }
    }

    @Test
    void verifyRefusalForATooShortStoredHashDoesNotEchoIt() {
        assertVerifyDoesNotEchoStoredHash("$argon2id$short-SECRETPW10", "SECRETPW10");
    }

    @Test
    void verifyRefusalForANonNumericParameterDoesNotEchoIt() {
        assertVerifyDoesNotEchoStoredHash("$argon2id$v=19$m=SECRETPW11,t=2,p=1$salt$hash", "SECRETPW11");
    }

    @Test
    void verifyRefusalForAMissingParameterDoesNotEchoTheParameterSegment() {
        assertVerifyDoesNotEchoStoredHash("$argon2id$v=19$SECRETPW12$salt$hash$x", "SECRETPW12");
    }
}
