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
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AbstractStatelessAppbaseService;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultPasswordService
        extends AbstractStatelessAppbaseService<DefaultPasswordServiceConfiguration>
        implements PasswordService
{
    public static final String SHA512 = "SHA-512";

    /**
     * Recorded in {@link PasswordHash#getShaRounds()} and serialised into the persisted credential
     * string as {@code $sha512$rounds=80$<salt>$<hash>}.
     *
     * <p>⚠️ <strong>This is a wire-format constant, not a tuning knob.</strong> Changing it changes
     * the serialisation of every newly written credential and breaks round-tripping against every
     * stored one. It must not be altered without a migration.
     *
     * <p>⚠️ <strong>It does not describe the computation.</strong> {@link #hashWithEmail} and
     * {@link #hashWithSalt} perform exactly <em>one</em> SHA-512 digest pass, not 80 iterations.
     * The value is carried for format compatibility only. Raised as F-04 of the 2026-07-26 Copilot
     * scan (internal work item 751); documented rather than changed, because the value is persisted.
     *
     * <p>⚠️ <strong>The Java and legacy Dart client-side hashes are not interoperable.</strong> The
     * legacy Flutter client uses the crypt(3) {@code $6$} scheme via {@code Crypt.sha512} with
     * <em>5000</em> rounds, while this
     * class emits a bare Base64 digest and records 80. A hash produced by one side can never equal
     * one produced by the other. This is latent rather than live: {@code hashWithEmail} and
     * {@code hashWithSalt} have <em>no</em> non-test caller in the Java tree, and the server never
     * recomputes the client hash — {@code DefaultUserAccountService} takes it as supplied and runs
     * Argon2id over it. Raised as F-07 (internal work item 751); converging the two is a protocol decision with a
     * stored-data migration, and is tracked separately.
     */
    public static final int SHA512_ROUNDS = 80;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Verification-side bound on the whole PHC string (L2-10). Emission-side strings are a few
     * hundred characters at most; anything longer is not a credential this service wrote.
     */
    private static final int MAX_VERIFY_PHC_LENGTH = 2048;

    /**
     * Verification policy envelope for the Argon2id parameters an attacker-controllable stored
     * string may demand. The defaults this service emits (m=19456 KiB = 19 MiB, t=2, p=1) sit
     * far inside; the caps bound BouncyCastle's allocation and loop counts (max 1 GiB of
     * memory, 100 iterations, 16 lanes); the floors exclude degenerate zero-parameter requests.
     */
    private static final int MIN_VERIFY_MEMORY_KIB = 8;
    private static final int MAX_VERIFY_MEMORY_KIB = 1 << 20;
    private static final int MIN_VERIFY_ITERATIONS = 1;
    private static final int MAX_VERIFY_ITERATIONS = 100;
    private static final int MIN_VERIFY_PARALLELISM = 1;
    private static final int MAX_VERIFY_PARALLELISM = 16;

    /**
     * Verification-side bounds on the base64 salt and hash segments, checked before decoding.
     * 172 base64 characters ≈ 129 salt bytes (this service emits 16); 1024 ≈ 768 hash bytes
     * (this service emits 32) — both far above any real credential, both small enough that the
     * {@code byte[storedBytes.length]} verification buffer can never be used as an allocation
     * amplifier.
     */
    private static final int MAX_VERIFY_SALT_BASE64_CHARS = 172;
    private static final int MAX_VERIFY_HASH_BASE64_CHARS = 1024;

    /** The only parameter keys a verified PHC string may carry. */
    private static final Set<String> PHC_PARAMETER_KEYS = Set.of("m", "t", "p");

    private final DefaultPasswordServiceConfiguration configuration;

    /**
     * Creates a new {@link DefaultPasswordService} with the given dependencies.
     *
     * @param logService the log service
     * @param metricService the metric service
     * @param mediatorService the mediator service
     * @param serviceRegistry the service registry
     * @param serviceConfiguration the Argon2id configuration
     */
    public DefaultPasswordService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final DefaultPasswordServiceConfiguration serviceConfiguration)
    {
        super(logService, metricService, mediatorService, serviceRegistry, serviceConfiguration);
        this.configuration = serviceConfiguration;
    }

    @Override
    protected void registerService() {
        serviceRegistry.registerAppbaseService(PasswordService.class, this);
        serviceRegistry.registerAppbaseService(DefaultPasswordService.class, this);
    }

    @Override
    public String randomSalt() {
        final byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);

        return Base64.getEncoder().encodeToString(salt);
    }

    @Override
    public PasswordHash hashWithEmail(final String password, final String email) {
        AppbaseUtil.notBlank(password, "password");
        AppbaseUtil.notBlank(email, "email");

        final String lowerCaseEmail = email.toLowerCase();
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance(SHA512);
        } catch (NoSuchAlgorithmException e) {
            throw new AppbaseRuntimeException("SHA-512 not supported by the runtime");
        }
        md.update(lowerCaseEmail.getBytes(StandardCharsets.UTF_8));

        final byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
        final String hashedPasswordStr = Base64.getEncoder().encodeToString(hashedPassword);

        return PasswordHash
                .builder()
                .hash(hashedPasswordStr)
                .salt(lowerCaseEmail)
                .hashAlgorithm(HashAlgorithm.SHA512)
                .shaRounds(SHA512_ROUNDS)
                .build();
    }

    @Override
    public PasswordHash hashWithSalt(final String password, final String salt) {
        AppbaseUtil.notBlank(password, "password");
        AppbaseUtil.notBlank(salt, "salt");

        final MessageDigest md;
        try {
            md = MessageDigest.getInstance(SHA512);
        } catch (NoSuchAlgorithmException e) {
            throw new AppbaseRuntimeException("SHA-512 not supported by the runtime");
        }
        md.update(salt.getBytes(StandardCharsets.UTF_8));

        final byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
        final String hashedPasswordStr = Base64.getEncoder().encodeToString(hashedPassword);

        return PasswordHash
                .builder()
                .hash(hashedPasswordStr)
                .salt(salt)
                .hashAlgorithm(HashAlgorithm.SHA512)
                .shaRounds(SHA512_ROUNDS)
                .build();
    }

    @Override
    public PasswordHash hashWithRandomSalt(final String password) {
        AppbaseUtil.notBlank(password, "password");

        final byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);

        final byte[] hash = argon2id(password.getBytes(StandardCharsets.UTF_8), salt);
        final String saltBase64 = Base64.getEncoder().withoutPadding().encodeToString(salt);
        final String hashBase64 = Base64.getEncoder().withoutPadding().encodeToString(hash);
        final String phcString = buildPhcString(saltBase64, hashBase64);

        return PasswordHash.builder()
                .hashAlgorithm(HashAlgorithm.ARGON2ID)
                .hash(phcString)
                .salt(saltBase64)
                .shaRounds(configuration.getIterations())
                .build();
    }

    /**
     * Runs the Argon2id key derivation function using the current configuration.
     *
     * @param input the password bytes
     * @param salt  the random salt bytes
     * @return the raw Argon2id hash bytes
     */
    private byte[] argon2id(final byte[] input, final byte[] salt) {
        final Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(salt)
                .withMemoryAsKB(configuration.getMemoryKiB())
                .withIterations(configuration.getIterations())
                .withParallelism(configuration.getParallelism())
                .build();

        final Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        final byte[] hash = new byte[configuration.getHashLength()];
        generator.generateBytes(input, hash);
        return hash;
    }

    /**
     * Builds the PHC-format string for an Argon2id hash.
     *
     * @param saltBase64 the Base64 (no-padding) encoded salt
     * @param hashBase64 the Base64 (no-padding) encoded hash
     * @return PHC string of the form {@code $argon2id$v=19$m=...,t=...,p=...$salt$hash}
     */
    private String buildPhcString(final String saltBase64, final String hashBase64) {
        return "$argon2id$v=19" +
                "$m=" + configuration.getMemoryKiB() +
                ",t=" + configuration.getIterations() +
                ",p=" + configuration.getParallelism() +
                "$" + saltBase64 +
                "$" + hashBase64;
    }

    /**
     * Verifies a plaintext password against an Argon2id stored hash.
     *
     * <p>Parameters (memory, iterations, parallelism) are read from the PHC string embedded in
     * {@code storedHash.hash}, making verification config-independent and forward-compatible.
     * The comparison is constant-time via {@link MessageDigest#isEqual}.
     *
     * <p>L2-10 (CWE-400): the stored PHC string is attacker-controllable (it arrives from the
     * credential store, which a compromise or a malicious import can shape), and BouncyCastle
     * honours its parameters literally — {@code m=2147483647} makes the process allocate toward
     * 2 TiB before any comparison happens. Every structural and policy check below therefore
     * runs <em>before</em> {@link Argon2BytesGenerator} is initialised or any buffer is sized
     * from the supplied hash, and every refusal omits the rejected text.
     *
     * @param input      the plaintext password to verify
     * @param storedHash the Argon2id {@link PasswordHash} produced by {@link #hashWithRandomSalt}
     * @return {@code true} if {@code input} matches the stored hash, {@code false} otherwise
     */
    @Override
    public boolean verify(final String input, final PasswordHash storedHash) {
        AppbaseUtil.notBlank(input, "input");
        AppbaseUtil.notNull(storedHash, "storedHash");

        if (storedHash.getHashAlgorithm() != HashAlgorithm.ARGON2ID) {
            return false;
        }

        final String phcString = storedHash.getHash();
        if (phcString == null || phcString.length() > MAX_VERIFY_PHC_LENGTH) {
            throw new AppbaseRuntimeException(
                    "Invalid Argon2id PHC string for verify (the rejected value is not included)");
        }

        final String[] parts = phcString.split("\\$", -1);
        if (parts.length != 6
                || !"argon2id".equals(parts[1])
                || !"v=19".equals(parts[2])) {
            throw new AppbaseRuntimeException(
                    "Invalid Argon2id PHC string for verify (the rejected value is not included)");
        }

        final Map<String, Integer> parameters = parsePhcParameters(parts[3]);
        final int memKib = requirePhcParameter(parameters, "m",
                MIN_VERIFY_MEMORY_KIB, MAX_VERIFY_MEMORY_KIB);
        final int iterations = requirePhcParameter(parameters, "t",
                MIN_VERIFY_ITERATIONS, MAX_VERIFY_ITERATIONS);
        final int parallelism = requirePhcParameter(parameters, "p",
                MIN_VERIFY_PARALLELISM, MAX_VERIFY_PARALLELISM);

        if (parts[4].isEmpty() || parts[4].length() > MAX_VERIFY_SALT_BASE64_CHARS
                || parts[5].isEmpty() || parts[5].length() > MAX_VERIFY_HASH_BASE64_CHARS) {
            throw new AppbaseRuntimeException(
                    "Invalid Argon2id PHC string for verify (the rejected value is not included)");
        }

        final byte[] salt;
        final byte[] storedBytes;
        try {
            salt = Base64.getDecoder().decode(padBase64(parts[4]));
            storedBytes = Base64.getDecoder().decode(padBase64(parts[5]));
        } catch (IllegalArgumentException e) {
            // Deliberately no cause: its message echoes the rejected base64 text.
            throw new AppbaseRuntimeException(
                    "Invalid Argon2id PHC string for verify (the rejected value is not included)");
        }
        if (storedBytes.length == 0) {
            throw new AppbaseRuntimeException("Invalid Argon2id PHC string: empty hash segment");
        }

        final Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(salt)
                .withMemoryAsKB(memKib)
                .withIterations(iterations)
                .withParallelism(parallelism)
                .build();

        final Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        final byte[] derived = new byte[storedBytes.length];
        generator.generateBytes(input.getBytes(StandardCharsets.UTF_8), derived);

        return MessageDigest.isEqual(derived, storedBytes);
    }

    /**
     * Parses the PHC parameter segment ({@code m=…,t=…,p=…}) under a strict grammar: each
     * comma-separated token must be {@code <known-key>=<ascii-digits>}, keys must not repeat,
     * and no empty tokens are permitted. A segment that a lenient {@code indexOf} scan would
     * silently accept — a duplicated {@code t}, an unknown {@code x=9}, an embedded empty
     * token, a signed or non-numeric value — is rejected here, before any of it reaches Argon2.
     */
    private static Map<String, Integer> parsePhcParameters(final String paramSegment) {
        final Map<String, Integer> parsed = new HashMap<>();
        for (final String token : paramSegment.split(",", -1)) {
            final int eq = token.indexOf('=');
            if (eq <= 0 || eq == token.length() - 1) {
                throw new AppbaseRuntimeException(
                        "Invalid Argon2id PHC parameter segment (the rejected value is not included)");
            }
            final String key = token.substring(0, eq);
            final String value = token.substring(eq + 1);
            if (!PHC_PARAMETER_KEYS.contains(key)
                    || !isAsciiDigits(value)
                    || parsed.containsKey(key)) {
                throw new AppbaseRuntimeException(
                        "Invalid Argon2id PHC parameter segment (the rejected value is not included)");
            }
            try {
                parsed.put(key, Integer.parseInt(value));
            } catch (NumberFormatException e) {
                // Digits only, but longer than an int — still out of policy.
                throw new AppbaseRuntimeException(
                        "Invalid Argon2id PHC parameter segment (the rejected value is not included)");
            }
        }
        return parsed;
    }

    private static boolean isAsciiDigits(final String value) {
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return !value.isEmpty();
    }

    /**
     * Returns a parsed PHC parameter, refusing it unless it lies inside the verification policy
     * envelope. The caps bound what BouncyCastle can be made to allocate or loop over — 1 GiB
     * of memory, 100 iterations, 16 lanes — far above anything this service emits
     * (m=19456, t=2, p=1) and above any sane imported credential, far below what an
     * attacker-controlled string could otherwise demand.
     */
    private static int requirePhcParameter(final Map<String, Integer> parameters,
                                           final String key,
                                           final int min,
                                           final int max) {
        final Integer value = parameters.get(key);
        if (value == null) {
            throw new AppbaseRuntimeException(
                    "Missing " + key + " parameter in PHC string (the rejected value is not included)");
        }
        if (value < min || value > max) {
            throw new AppbaseRuntimeException(
                    "Out-of-policy " + key + " parameter in PHC string (the rejected value is not included)");
        }
        return value;
    }

    private static String padBase64(final String s) {
        final int pad = (4 - s.length() % 4) % 4;
        return pad == 0 ? s : s + "=".repeat(pad);
    }
}
