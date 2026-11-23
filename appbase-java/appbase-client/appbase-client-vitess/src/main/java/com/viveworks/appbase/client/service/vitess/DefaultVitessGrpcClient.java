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

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vitess.client.Context;
import io.vitess.client.VTGateBlockingConnection;
import io.vitess.client.VTGateConnection;
import io.vitess.client.VTSession;

/**
 * Default implementation of {@link VitessGrpcClient}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class DefaultVitessGrpcClient implements VitessGrpcClient {
    private final VTGateConnection vtGateConnection;
    private final VTGateBlockingConnection vtGateBlockingConnection;
    private final Context defaultContext;
    private final VTSession vtSession;

    private DefaultVitessGrpcClient(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.vtGateConnection, "builder.vtGateConnection");
        AppbaseUtil.notNull(builder.vtGateBlockingConnection, "builder.vtGateBlockingConnection");
        AppbaseUtil.notNull(builder.defaultContext, "builder.defaultContext");
        AppbaseUtil.notNull(builder.vtSession, "builder.vtSession");

        vtGateConnection = builder.vtGateConnection;
        vtGateBlockingConnection = builder.vtGateBlockingConnection;
        defaultContext = builder.defaultContext;
        vtSession = builder.vtSession;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public VTGateConnection getVtGateConnection() {
        return vtGateConnection;
    }

    public VTGateBlockingConnection getVtGateBlockingConnection() {
        return vtGateBlockingConnection;
    }

    @Override
    public Context getDefaultContext() {
        return defaultContext;
    }

    @Override
    public VTSession getVtSession() {
        return vtSession;
    }

    @Override
    public String toString() {
        return "DefaultVitessGrpcClient{" +
                "vtGateConnection=" + vtGateConnection +
                ", vtGateBlockingConnection=" + vtGateBlockingConnection +
                ", defaultContext=" + defaultContext +
                ", vtSession=" + vtSession +
                '}';
    }

    /**
     * Builder for {@link DefaultVitessGrpcClient}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private VTGateConnection vtGateConnection;
        private VTGateBlockingConnection vtGateBlockingConnection;
        private Context defaultContext;
        private VTSession vtSession;

        private Builder() {
        }

        public Builder vtGateConnection(final VTGateConnection vtGateConnection) {
            this.vtGateConnection = vtGateConnection;
            return this;
        }

        public Builder vtGateBlockingConnection(final VTGateBlockingConnection vtGateBlockingConnection) {
            this.vtGateBlockingConnection = vtGateBlockingConnection;
            return this;
        }

        public Builder defaultContext(final Context defaultContext) {
            this.defaultContext = defaultContext;
            return this;
        }

        public Builder vtSession(final VTSession vtSession) {
            this.vtSession = vtSession;
            return this;
        }

        public DefaultVitessGrpcClient build() {
            return new DefaultVitessGrpcClient(this);
        }
    }
    // CheckStyle: Builder end
}
