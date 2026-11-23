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

package com.viveworks.appbase.client.service.vitess;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.vitess.configuration.CustomGrpcClientFactoryConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.Http2NegotiationType;
import com.viveworks.appbase.client.service.vitess.configuration.NettyChannelConfiguration;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

/**
 * Verifies that the default Vitess transport delegates both channel and event-loop selection to gRPC.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
final class CustomGrpcClientFactoryTransportTest {

    @Test
    void defaultTransportBuildsWithoutOneSidedEventLoopConfiguration() {
        final AppbaseClientCore clientCore = Mockito.mock(AppbaseClientCore.class, Answers.RETURNS_DEEP_STUBS);
        final NettyChannelConfiguration nettyConfiguration = NettyChannelConfiguration.builder()
                .http2NegotiationType(Http2NegotiationType.PLAINTEXT)
                .sslClientContextConfiguration(null)
                .build();
        final CustomGrpcClientFactoryConfiguration configuration =
                CustomGrpcClientFactoryConfiguration.builder()
                        .nettyChannelConfiguration(nettyConfiguration)
                        .build();
        final CustomGrpcClientFactory factory = new CustomGrpcClientFactory(clientCore, configuration);

        final ManagedChannel channel = Assertions.assertDoesNotThrow(
                () -> factory.channelBuilder("unused").build());
        channel.shutdownNow();
    }
}
