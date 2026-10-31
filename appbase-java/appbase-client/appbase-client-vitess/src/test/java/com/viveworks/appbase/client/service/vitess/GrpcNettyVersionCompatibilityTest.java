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

package com.viveworks.appbase.client.service.vitess;

import io.grpc.netty.NettyChannelBuilder;
import io.netty.util.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Guards the runtime gRPC/Netty combination used by the Vitess transport.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class GrpcNettyVersionCompatibilityTest {

    private static final int[] FIRST_AFFECTED_NETTY_VERSION = {4, 1, 111};

    @Test
    void runtimeDependenciesExcludeKnownHttp2FrameCorruptionCombination() {
        final String grpcVersion = implementationVersion(NettyChannelBuilder.class);
        final String nettyVersion = implementationVersion(Version.class);

        final boolean affectedNetty = isAtLeast(nettyVersion, FIRST_AFFECTED_NETTY_VERSION);
        final boolean fixedGrpc = grpcHasFrameCorruptionFix(grpcVersion);

        Assertions.assertFalse(
                affectedNetty && !fixedGrpc,
                () -> "grpc-netty " + grpcVersion + " is incompatible with Netty " + nettyVersion +
                        "; use a gRPC release containing the HTTP/2 frame corruption fix");
    }

    @ParameterizedTest
    @CsvSource({
            "1.62.99, false",
            "1.63.1, false",
            "1.63.2, true",
            "1.64.0, false",
            "1.64.1, false",
            "1.64.2, true",
            "1.65.0, false",
            "1.65.1, true",
            "1.66.0, true",
            "2.0.0, true"
    })
    void recognizesFixedReleaseOnEachGrpcBranch(final String grpcVersion, final boolean expected) {
        Assertions.assertEquals(expected, grpcHasFrameCorruptionFix(grpcVersion));
    }

    private static String implementationVersion(final Class<?> type) {
        final String version = type.getPackage().getImplementationVersion();
        Assertions.assertNotNull(version, () -> "Missing Implementation-Version for " + type.getName());
        return version;
    }

    private static boolean isAtLeast(final String actualVersion, final int[] minimumVersion) {
        final int[] components = versionComponents(actualVersion);
        Assertions.assertTrue(
                components.length >= minimumVersion.length,
                () -> "Cannot compare dependency version " + actualVersion);
        for (int index = 0; index < minimumVersion.length; index++) {
            final int actualComponent = components[index];
            if (actualComponent != minimumVersion[index]) {
                return actualComponent > minimumVersion[index];
            }
        }
        return true;
    }

    private static boolean grpcHasFrameCorruptionFix(final String grpcVersion) {
        final int[] components = versionComponents(grpcVersion);
        Assertions.assertTrue(components.length >= 3, () -> "Cannot compare dependency version " + grpcVersion);
        final int major = components[0];
        final int minor = components[1];
        final int patch = components[2];

        if (major != 1) {
            return major > 1;
        }
        if (minor < 63) {
            return false;
        }
        if (minor == 63 || minor == 64) {
            return patch >= 2;
        }
        if (minor == 65) {
            return patch >= 1;
        }
        return true;
    }

    private static int[] versionComponents(final String version) {
        final String[] components = version.split("[.-]");
        final int[] numericComponents = new int[components.length];
        for (int index = 0; index < components.length; index++) {
            if (!components[index].chars().allMatch(Character::isDigit)) {
                break;
            }
            numericComponents[index] = Integer.parseInt(components[index]);
        }
        return numericComponents;
    }
}
