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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;

import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum HashAlgorithm {
    SHA512("6", "sha512"),
    ARGON2ID("argon2id", "argon2id");

    private static final Map<String, HashAlgorithm> HASH_ALGORITHM_BY_SHORT_SYMBOL_MAP = Map.of(
            SHA512.getShortSymbol(), SHA512,
            ARGON2ID.getShortSymbol(), ARGON2ID
    );

    private final String shortSymbol;
    private final String serializedName;

    HashAlgorithm(final String shortSymbol, final String serializedName) {
        this.shortSymbol = shortSymbol;
        this.serializedName = serializedName;
    }

    public static HashAlgorithm fromShortSymbol(final String shortSymbol) {
        return HASH_ALGORITHM_BY_SHORT_SYMBOL_MAP.get(shortSymbol);
    }

    public String getShortSymbol() {
        return shortSymbol;
    }

    @JsonValue
    public String getSerializedName() {
        return serializedName;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static HashAlgorithm from(final String hashAlgorithm) {
        if (hashAlgorithm == null) {
            return HashAlgorithm.SHA512;
        }

        final String trimmedHashAlgorithm = hashAlgorithm.trim();
        if ("HashAlgorithm.sha512".equals(trimmedHashAlgorithm)) {
            return HashAlgorithm.SHA512;
        }

        if (trimmedHashAlgorithm.contains(HashAlgorithm.SHA512.serializedName)) {
            return HashAlgorithm.SHA512;
        }

        if (trimmedHashAlgorithm.toUpperCase().equals(HashAlgorithm.SHA512.name())) {
            return HashAlgorithm.SHA512;
        }

        if ("6".equals(trimmedHashAlgorithm)) {
            return HashAlgorithm.SHA512;
        }

        if (trimmedHashAlgorithm.equals(HashAlgorithm.ARGON2ID.serializedName) ||
                trimmedHashAlgorithm.toUpperCase().equals(HashAlgorithm.ARGON2ID.name())) {
            return HashAlgorithm.ARGON2ID;
        }

        throw new AppbaseRuntimeException("Unsupported hash algorithm (the rejected value is not included)");
    }
}
