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

package com.viveworks.appbase.core.service.tracer.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.service.tracer.DefaultTracerService;
import com.viveworks.appbase.core.service.tracer.TracerServiceConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.propagation.JaegerPropagationConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.JaegerReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.LoggingReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.ConstSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.JaegerSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.scopemanager.JaegerScopeManagerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.scopemanager.MDCScopeManagerConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for {@link DefaultTracerService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@DefaultTracerServiceConfiguration.Valid
@JsonDeserialize(builder = DefaultTracerServiceConfiguration.Builder.class)
public final class DefaultTracerServiceConfiguration implements TracerServiceConfiguration {
    public static final String DEFAULT_YAML_NODE_NAME = "defaultTracerServiceConfiguration";

    @NotBlank
    private final String serviceName;

    @Nullable
    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> tags;

    private final boolean use128BitTraceId;

    private final boolean expandExceptionLogs;

    @NotNull
    private final JaegerPropagationConfiguration jaegerPropagationConfiguration;

    @NotNull
    private final JaegerSamplerConfiguration jaegerSamplerConfiguration;

    @NotNull
    private final JaegerScopeManagerConfiguration jaegerScopeManagerConfiguration;

    @NotNull
    @JsonSerialize(contentAs = JaegerReporterConfiguration.class)
    @JsonDeserialize(contentAs = JaegerReporterConfiguration.class)
    private final List<JaegerReporterConfiguration> jaegerReporterConfigurations;

    private DefaultTracerServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultTracerServiceConfigurationValidator.validate(
                builder.serviceName,
                builder.jaegerPropagationConfiguration,
                builder.jaegerSamplerConfiguration,
                builder.jaegerScopeManagerConfiguration,
                builder.jaegerReporterConfigurations);

        serviceName = builder.serviceName;
        if (builder.tags != null) {
            tags = new HashMap<>(builder.tags);
        } else {
            tags = Map.of();
        }
        use128BitTraceId = builder.use128BitTraceId;
        expandExceptionLogs = builder.expandExceptionLogs;
        jaegerPropagationConfiguration = builder.jaegerPropagationConfiguration;
        jaegerSamplerConfiguration = builder.jaegerSamplerConfiguration;
        jaegerReporterConfigurations = builder.jaegerReporterConfigurations;
        jaegerScopeManagerConfiguration = builder.jaegerScopeManagerConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getServiceName() {
        return serviceName;
    }

    @Nullable
    public Map<String, String> getTags() {
        return tags;
    }

    public boolean isUse128BitTraceId() {
        return use128BitTraceId;
    }

    public boolean isExpandExceptionLogs() {
        return expandExceptionLogs;
    }

    public JaegerPropagationConfiguration getJaegerPropagationConfiguration() {
        return jaegerPropagationConfiguration;
    }

    public JaegerSamplerConfiguration getJaegerSamplerConfiguration() {
        return jaegerSamplerConfiguration;
    }

    public List<JaegerReporterConfiguration> getJaegerReporterConfigurations() {
        return jaegerReporterConfigurations;
    }

    public JaegerScopeManagerConfiguration getJaegerScopeManagerConfiguration() {
        return jaegerScopeManagerConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefaultTracerServiceConfiguration that = (DefaultTracerServiceConfiguration) o;
        return use128BitTraceId == that.use128BitTraceId &&
                expandExceptionLogs == that.expandExceptionLogs &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(jaegerPropagationConfiguration, that.jaegerPropagationConfiguration) &&
                Objects.equals(jaegerSamplerConfiguration, that.jaegerSamplerConfiguration) &&
                Objects.equals(jaegerReporterConfigurations, that.jaegerReporterConfigurations) &&
                Objects.equals(jaegerScopeManagerConfiguration, that.jaegerScopeManagerConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                serviceName,
                tags,
                use128BitTraceId,
                expandExceptionLogs,
                jaegerPropagationConfiguration,
                jaegerSamplerConfiguration,
                jaegerReporterConfigurations,
                jaegerScopeManagerConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultTracerServiceConfiguration{" +
                "serviceName='" + serviceName + '\'' +
                ", tags=" + tags +
                ", use128BitTraceId=" + use128BitTraceId +
                ", expandExceptionLogs=" + expandExceptionLogs +
                ", jaegerPropagationConfiguration=" + jaegerPropagationConfiguration +
                ", jaegerSamplerConfiguration=" + jaegerSamplerConfiguration +
                ", jaegerReporterConfigurations=" + jaegerReporterConfigurations +
                ", jaegerScopeManagerConfiguration=" + jaegerScopeManagerConfiguration +
                '}';
    }

    /**
     * {@link DefaultTracerServiceConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_SERVICE_NAME = "AppbaseService";
        private static final boolean DEFAULT_USE_128_BIT_TRACE_ID = true;
        private static final boolean DEFAULT_EXPAND_EXCEPTION_LOGS = true;
        private static final JaegerPropagationConfiguration DEFAULT_JAEGER_PROPAGATION_CONFIGURATION =
                JaegerPropagationConfiguration.builder().build();
        private static final JaegerSamplerConfiguration DEFAULT_JAEGER_SAMPLER_CONFIGURATION =
                ConstSamplerConfiguration.builder().decision(true).build();
        private static final List<JaegerReporterConfiguration> DEFAULT_JAEGER_REPORTER_CONFIGURATIONS =
                List.of(LoggingReporterConfiguration.builder().enabled(true).build());
        private static final JaegerScopeManagerConfiguration DEFAULT_JAEGER_SCOPE_MANAGER_CONFIGURATION =
                MDCScopeManagerConfiguration.builder().build();


        private String serviceName = DEFAULT_SERVICE_NAME;

        @JsonSerialize(keyAs = String.class, contentAs = String.class)
        @JsonDeserialize(keyAs = String.class, contentAs = String.class)
        private Map<String, String> tags;

        private boolean use128BitTraceId = DEFAULT_USE_128_BIT_TRACE_ID;

        private boolean expandExceptionLogs = DEFAULT_EXPAND_EXCEPTION_LOGS;

        private JaegerPropagationConfiguration jaegerPropagationConfiguration =
                DEFAULT_JAEGER_PROPAGATION_CONFIGURATION;

        private JaegerSamplerConfiguration jaegerSamplerConfiguration = DEFAULT_JAEGER_SAMPLER_CONFIGURATION;

        @JsonSerialize(contentAs = JaegerReporterConfiguration.class)
        @JsonDeserialize(contentAs = JaegerReporterConfiguration.class)
        private List<JaegerReporterConfiguration> jaegerReporterConfigurations = DEFAULT_JAEGER_REPORTER_CONFIGURATIONS;

        private JaegerScopeManagerConfiguration jaegerScopeManagerConfiguration = DEFAULT_JAEGER_SCOPE_MANAGER_CONFIGURATION;

        private Builder() {
        }

        public Builder serviceName(final String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder tags(
                @JsonSerialize(keyAs = String.class, contentAs = String.class)
                @JsonDeserialize(keyAs = String.class, contentAs = String.class)
                final Map<String, String> tags)
        {
            this.tags = tags;
            return this;
        }

        public Builder use128BitTraceId(final boolean use128BitTraceId) {
            this.use128BitTraceId = use128BitTraceId;
            return this;
        }

        public Builder expandExceptionLogs(final boolean expandExceptionLogs) {
            this.expandExceptionLogs = expandExceptionLogs;
            return this;
        }

        public Builder jaegerPropagationConfiguration(
                final JaegerPropagationConfiguration jaegerPropagationConfiguration)
        {
            this.jaegerPropagationConfiguration = jaegerPropagationConfiguration;
            return this;
        }

        public Builder jaegerSamplerConfiguration(final JaegerSamplerConfiguration jaegerSamplerConfiguration) {
            this.jaegerSamplerConfiguration = jaegerSamplerConfiguration;
            return this;
        }

        public Builder jaegerReporterConfigurations(
                @JsonSerialize(contentAs = JaegerReporterConfiguration.class)
                @JsonDeserialize(contentAs = JaegerReporterConfiguration.class)
                final List<JaegerReporterConfiguration> jaegerReporterConfigurations)
        {
            this.jaegerReporterConfigurations = jaegerReporterConfigurations;
            return this;
        }

        public Builder jaegerScopeManagerConfiguration(
                final JaegerScopeManagerConfiguration jaegerScopeManagerConfiguration)
        {
            this.jaegerScopeManagerConfiguration = jaegerScopeManagerConfiguration;
            return this;
        }

        public DefaultTracerServiceConfiguration build() {
            return new DefaultTracerServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultTracerServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultTracerServiceConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
