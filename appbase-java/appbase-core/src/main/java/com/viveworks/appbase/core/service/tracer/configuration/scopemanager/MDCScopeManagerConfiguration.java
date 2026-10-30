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

package com.viveworks.appbase.core.service.tracer.configuration.scopemanager;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jaegertracing.internal.MDCScopeManager;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link MDCScopeManager}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = MDCScopeManagerConfiguration.Builder.class)
public final class MDCScopeManagerConfiguration implements JaegerScopeManagerConfiguration {
    private final String mdcTraceIdKey;
    private final String mdcSpanIdKey;
    private final String mdcSampledKey;

    private MDCScopeManagerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.mdcTraceIdKey, "builder.mdcTraceIdKey");
        AppbaseUtil.notNull(builder.mdcSpanIdKey, "builder.mdcSpanIdKey");
        AppbaseUtil.notNull(builder.mdcSampledKey, "builder.mdcSampledKey");

        mdcTraceIdKey = builder.mdcTraceIdKey;
        mdcSpanIdKey = builder.mdcSpanIdKey;
        mdcSampledKey = builder.mdcSampledKey;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getMdcTraceIdKey() {
        return mdcTraceIdKey;
    }

    public String getMdcSpanIdKey() {
        return mdcSpanIdKey;
    }

    public String getMdcSampledKey() {
        return mdcSampledKey;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MDCScopeManagerConfiguration that = (MDCScopeManagerConfiguration) o;
        return Objects.equals(mdcTraceIdKey, that.mdcTraceIdKey) &&
                Objects.equals(mdcSpanIdKey, that.mdcSpanIdKey) &&
                Objects.equals(mdcSampledKey, that.mdcSampledKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mdcTraceIdKey, mdcSpanIdKey, mdcSampledKey);
    }

    @Override
    public String toString() {
        return "MDCScopeManagerConfiguration{" +
                "mdcTraceIdKey='" + mdcTraceIdKey + '\'' +
                ", mdcSpanIdKey='" + mdcSpanIdKey + '\'' +
                ", mdcSampledKey='" + mdcSampledKey + '\'' +
                '}';
    }

    /**
     * Builder for {@link MDCScopeManagerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_MDC_TRACE_ID_KEY = "trace.id";
        private static final String DEFAULT_MDC_SPAN_ID_KEY = "trace.spanId";
        private static final String DEFAULT_MDC_SAMPLED_KEY = "trace.sampled";

        private String mdcTraceIdKey = DEFAULT_MDC_TRACE_ID_KEY;
        private String mdcSpanIdKey = DEFAULT_MDC_SPAN_ID_KEY;
        private String mdcSampledKey = DEFAULT_MDC_SAMPLED_KEY;

        private Builder() {
        }

        public Builder mdcTraceIdKey(final String mdcTraceIdKey) {
            this.mdcTraceIdKey = mdcTraceIdKey;
            return this;
        }

        public Builder mdcSpanIdKey(final String mdcSpanIdKey) {
            this.mdcSpanIdKey = mdcSpanIdKey;
            return this;
        }

        public Builder mdcSampledKey(final String mdcSampledKey) {
            this.mdcSampledKey = mdcSampledKey;
            return this;
        }

        public MDCScopeManagerConfiguration build() {
            return new MDCScopeManagerConfiguration(this);
        }
    }
    // CheckStyle: Builder end

}
