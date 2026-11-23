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

package com.viveworks.appbase.core.service.log;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.log.accessor.DefaultLogAccessorConfiguration;
import com.viveworks.appbase.core.service.log.appender.disruptor.DisruptorMediatorEventDelegatingAppenderConfiguration;
import com.viveworks.appbase.core.service.log.appender.lucene.LuceneAppenderConfiguration;
import com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration;
import com.viveworks.appbase.core.service.log.common.lucene.LuceneLogIndexConfiguration;
import com.viveworks.appbase.core.service.log.filter.burst.BurstTurboLogEventFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.duplicate.DuplicateLogTurboFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.event.DisruptorLogAndTurboLogEventFilterDelegatorConfiguration;
import com.viveworks.appbase.core.service.log.filter.event.listener.lucene.LuceneLogAndTurboLogEventFilterListenerConfiguration;
import com.viveworks.appbase.core.service.log.filter.metrics.MetricsTurboFilterHandlerConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/**
 * Default implementation of {@link LogServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultLogServiceConfiguration.Builder.class)
@DefaultLogServiceConfiguration.Valid
public final class DefaultLogServiceConfiguration implements LogServiceConfiguration {
    public static final String DEFAULT_YAML_NODE_NAME = "defaultLogServiceConfiguration";

    @Nullable
    private final String logbackXml;

    @NotNull
    private final DefaultLogAccessorConfiguration defaultLogAccessorConfiguration;

    @NotNull
    private final MetricsTurboFilterHandlerConfiguration metricsTurboFilterHandlerConfiguration;

    @NotNull
    private final BurstTurboLogEventFilterHandlerConfiguration burstTurboLogEventFilterHandlerConfiguration;

    @NotNull
    private final DisruptorMediatorEventDelegatingAppenderConfiguration
            disruptorMediatorEventDelegatingAppenderConfiguration;

    @NotNull
    private final LuceneLogIndexConfiguration luceneLogIndexConfiguration;

    @NotNull
    private final LuceneAppenderConfiguration luceneAppenderConfiguration;

    @NotNull
    private final DisruptorLogAndTurboLogEventFilterDelegatorConfiguration
            disruptorLogAndTurboLogEventFilterDelegatorConfiguration;

    @NotNull
    private final DuplicateLogTurboFilterHandlerConfiguration
            duplicateLogTurboFilterHandlerConfiguration;

    @NotNull
    private final LuceneLogAndTurboLogEventFilterListenerConfiguration
            luceneLogAndTurboLogEventFilterListenerConfiguration;

    private DefaultLogServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultLogServiceConfigurationValidator.validate(
                builder.defaultLogAccessorConfiguration,
                builder.metricsTurboFilterHandlerConfiguration,
                builder.burstTurboLogEventFilterHandlerConfiguration,
                builder.disruptorMediatorEventDelegatingAppenderConfiguration,
                builder.luceneLogIndexConfiguration,
                builder.luceneAppenderConfiguration,
                builder.disruptorLogAndTurboLogEventFilterDelegatorConfiguration,
                builder.duplicateLogTurboFilterHandlerConfiguration,
                builder.luceneLogAndTurboLogEventFilterListenerConfiguration,
                null,
                null);

        logbackXml = builder.logbackXml;
        defaultLogAccessorConfiguration = builder.defaultLogAccessorConfiguration;
        metricsTurboFilterHandlerConfiguration = builder.metricsTurboFilterHandlerConfiguration;
        burstTurboLogEventFilterHandlerConfiguration = builder.burstTurboLogEventFilterHandlerConfiguration;
        disruptorMediatorEventDelegatingAppenderConfiguration =
                builder.disruptorMediatorEventDelegatingAppenderConfiguration;
        luceneLogIndexConfiguration = builder.luceneLogIndexConfiguration;
        luceneAppenderConfiguration = builder.luceneAppenderConfiguration;
        disruptorLogAndTurboLogEventFilterDelegatorConfiguration =
                builder.disruptorLogAndTurboLogEventFilterDelegatorConfiguration;
        duplicateLogTurboFilterHandlerConfiguration =
                builder.duplicateLogTurboFilterHandlerConfiguration;
        luceneLogAndTurboLogEventFilterListenerConfiguration =
                builder.luceneLogAndTurboLogEventFilterListenerConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getLogbackXml() {
        return logbackXml;
    }

    public DefaultLogAccessorConfiguration getDefaultLogAccessorConfiguration() {
        return defaultLogAccessorConfiguration;
    }

    public MetricsTurboFilterHandlerConfiguration getMetricsTurboFilterHandlerConfiguration() {
        return metricsTurboFilterHandlerConfiguration;
    }

    public BurstTurboLogEventFilterHandlerConfiguration getBurstTurboLogEventFilterHandlerConfiguration() {
        return burstTurboLogEventFilterHandlerConfiguration;
    }

    public DisruptorMediatorEventDelegatingAppenderConfiguration
    getDisruptorMediatorEventDelegatingAppenderConfiguration()
    {
        return disruptorMediatorEventDelegatingAppenderConfiguration;
    }

    public LuceneLogIndexConfiguration getLuceneLogIndexConfiguration() {
        return luceneLogIndexConfiguration;
    }

    public LuceneAppenderConfiguration getLuceneAppenderConfiguration() {
        return luceneAppenderConfiguration;
    }

    public DisruptorLogAndTurboLogEventFilterDelegatorConfiguration
    getDisruptorLogAndTurboLogEventFilterDelegatorConfiguration()
    {
        return disruptorLogAndTurboLogEventFilterDelegatorConfiguration;
    }

    public DuplicateLogTurboFilterHandlerConfiguration getDuplicateLogTurboFilterHandlerConfiguration() {
        return duplicateLogTurboFilterHandlerConfiguration;
    }

    public LuceneLogAndTurboLogEventFilterListenerConfiguration
    getLuceneLogAndTurboLogEventFilterListenerConfiguration()
    {
        return luceneLogAndTurboLogEventFilterListenerConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof DefaultLogServiceConfiguration)) {
            return false;
        }

        final DefaultLogServiceConfiguration that = (DefaultLogServiceConfiguration) o;
        return Objects.equals(logbackXml, that.logbackXml) &&
                Objects.equals(
                        defaultLogAccessorConfiguration,
                        that.defaultLogAccessorConfiguration) &&
                Objects.equals(
                        metricsTurboFilterHandlerConfiguration,
                        that.metricsTurboFilterHandlerConfiguration) &&
                Objects.equals(
                        burstTurboLogEventFilterHandlerConfiguration,
                        that.burstTurboLogEventFilterHandlerConfiguration) &&
                Objects.equals(
                        disruptorMediatorEventDelegatingAppenderConfiguration,
                        that.disruptorMediatorEventDelegatingAppenderConfiguration) &&
                Objects.equals(
                        luceneLogIndexConfiguration,
                        that.luceneLogIndexConfiguration) &&
                Objects.equals(
                        luceneAppenderConfiguration,
                        that.luceneAppenderConfiguration) &&
                Objects.equals(
                        disruptorLogAndTurboLogEventFilterDelegatorConfiguration,
                        that.disruptorLogAndTurboLogEventFilterDelegatorConfiguration) &&
                Objects.equals(
                        duplicateLogTurboFilterHandlerConfiguration,
                        that.duplicateLogTurboFilterHandlerConfiguration) &&
                Objects.equals(
                        luceneLogAndTurboLogEventFilterListenerConfiguration,
                        that.luceneLogAndTurboLogEventFilterListenerConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                logbackXml,
                defaultLogAccessorConfiguration,
                metricsTurboFilterHandlerConfiguration,
                burstTurboLogEventFilterHandlerConfiguration,
                disruptorMediatorEventDelegatingAppenderConfiguration,
                luceneLogIndexConfiguration,
                luceneAppenderConfiguration,
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration,
                duplicateLogTurboFilterHandlerConfiguration,
                luceneLogAndTurboLogEventFilterListenerConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultLogServiceConfiguration{" +
                "logbackXml='" + logbackXml + '\'' +
                ", defaultLogAccessorConfiguration=" + defaultLogAccessorConfiguration +
                ", metricsTurboFilterHandlerConfiguration=" + metricsTurboFilterHandlerConfiguration +
                ", burstTurboLogEventFilterHandlerConfiguration=" + burstTurboLogEventFilterHandlerConfiguration +
                ", disruptorMediatorEventDelegatingAppenderConfiguration=" +
                disruptorMediatorEventDelegatingAppenderConfiguration +
                ", luceneLogIndexConfiguration=" + luceneLogIndexConfiguration +
                ", luceneAppenderConfiguration=" + luceneAppenderConfiguration +
                ", disruptorLogAndTurboLogEventFilterDelegatorConfiguration=" +
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration +
                ", duplicateLogTurboFilterHandlerConfiguration=" + duplicateLogTurboFilterHandlerConfiguration +
                ", luceneLogAndTurboLogEventFilterListenerConfiguration=" +
                luceneLogAndTurboLogEventFilterListenerConfiguration +
                '}';
    }

    /**
     * Builder for {@link DefaultLogServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final DefaultLogAccessorConfiguration DEFAULT_DEFAULT_LOG_ACCESSOR_CONFIGURATION =
                DefaultLogAccessorConfiguration.builder().build();
        private static final MetricsTurboFilterHandlerConfiguration
                DEFAULT_METRICS_TURBO_FILTER_HANDLER_CONFIGURATION =
                MetricsTurboFilterHandlerConfiguration.builder().build();
        private static final BurstTurboLogEventFilterHandlerConfiguration
                DEFAULT_BURST_TURBO_LOG_EVENT_FILTER_HANDLER_CONFIGURATION =
                BurstTurboLogEventFilterHandlerConfiguration.builder().build();
        private static final DisruptorMediatorEventDelegatingAppenderConfiguration
                DEFAULT_DISRUPTOR_MEDIATOR_EVENT_DELEGATING_APPENDER_CONFIGURATION =
                DisruptorMediatorEventDelegatingAppenderConfiguration.builder().build();
        // R3 (internal work item 1073, spec §10): index defaults disabled, in step with the Lucene appender and store, which
        // also default off now — so a zero-config log service does no hot-path indexing. Opt in explicitly.
        private static final LuceneLogIndexConfiguration
                DEFAULT_LUCENE_LOG_INDEX_CONFIGURATION =
                DefaultLuceneLogIndexConfiguration.builder().build();
        private static final LuceneAppenderConfiguration
                DEFAULT_LUCENE_APPENDER_CONFIGURATION =
                LuceneAppenderConfiguration.builder().build();
        private static final DisruptorLogAndTurboLogEventFilterDelegatorConfiguration
                DEFAULT_DISRUPTOR_LOG_AND_TURBO_LOG_EVENT_FILTER_DELEGATOR_CONFIGURATION =
                DisruptorLogAndTurboLogEventFilterDelegatorConfiguration.builder().build();
        private static final DuplicateLogTurboFilterHandlerConfiguration
                DEFAULT_DUPLICATE_LOG_TURBO_FILTER_HANDLER_CONFIGURATION =
                DuplicateLogTurboFilterHandlerConfiguration.builder().build();
        private static final LuceneLogAndTurboLogEventFilterListenerConfiguration
                DEFAULT_LUCENE_LOG_AND_TURBO_LOG_EVENT_FILTER_LISTENER_CONFIGURATION =
                LuceneLogAndTurboLogEventFilterListenerConfiguration.builder().build();

        private String logbackXml;
        private DefaultLogAccessorConfiguration defaultLogAccessorConfiguration =
                DEFAULT_DEFAULT_LOG_ACCESSOR_CONFIGURATION;
        private MetricsTurboFilterHandlerConfiguration metricsTurboFilterHandlerConfiguration =
                DEFAULT_METRICS_TURBO_FILTER_HANDLER_CONFIGURATION;
        private BurstTurboLogEventFilterHandlerConfiguration burstTurboLogEventFilterHandlerConfiguration =
                DEFAULT_BURST_TURBO_LOG_EVENT_FILTER_HANDLER_CONFIGURATION;
        private DisruptorMediatorEventDelegatingAppenderConfiguration
                disruptorMediatorEventDelegatingAppenderConfiguration =
                DEFAULT_DISRUPTOR_MEDIATOR_EVENT_DELEGATING_APPENDER_CONFIGURATION;
        private LuceneLogIndexConfiguration luceneLogIndexConfiguration =
                DEFAULT_LUCENE_LOG_INDEX_CONFIGURATION;
        private LuceneAppenderConfiguration luceneAppenderConfiguration =
                DEFAULT_LUCENE_APPENDER_CONFIGURATION;
        private DisruptorLogAndTurboLogEventFilterDelegatorConfiguration
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration =
                DEFAULT_DISRUPTOR_LOG_AND_TURBO_LOG_EVENT_FILTER_DELEGATOR_CONFIGURATION;
        private DuplicateLogTurboFilterHandlerConfiguration duplicateLogTurboFilterHandlerConfiguration =
                DEFAULT_DUPLICATE_LOG_TURBO_FILTER_HANDLER_CONFIGURATION;
        private LuceneLogAndTurboLogEventFilterListenerConfiguration
                luceneLogAndTurboLogEventFilterListenerConfiguration =
                DEFAULT_LUCENE_LOG_AND_TURBO_LOG_EVENT_FILTER_LISTENER_CONFIGURATION;

        private Builder() {
        }

        public Builder logbackXml(final String logbackXml) {
            this.logbackXml = logbackXml;
            return this;
        }

        public Builder defaultLogAccessorConfiguration(
                final DefaultLogAccessorConfiguration defaultLogAccessorConfiguration)
        {
            this.defaultLogAccessorConfiguration = defaultLogAccessorConfiguration;
            return this;
        }

        public Builder disruptorMediatorEventDelegatingAppenderConfiguration(
                final DisruptorMediatorEventDelegatingAppenderConfiguration
                        disruptorMediatorEventDelegatingAppenderConfiguration)
        {
            this.disruptorMediatorEventDelegatingAppenderConfiguration =
                    disruptorMediatorEventDelegatingAppenderConfiguration;
            return this;
        }

        public Builder luceneLogIndexConfiguration(final LuceneLogIndexConfiguration luceneLogIndexConfiguration) {
            this.luceneLogIndexConfiguration = luceneLogIndexConfiguration;
            return this;
        }

        public Builder luceneAppenderConfiguration(final LuceneAppenderConfiguration luceneAppenderConfiguration) {
            this.luceneAppenderConfiguration = luceneAppenderConfiguration;
            return this;
        }

        public Builder burstLogTurboFilterHandlerConfiguration(
                final BurstTurboLogEventFilterHandlerConfiguration burstTurboLogEventFilterHandlerConfiguration)
        {
            this.burstTurboLogEventFilterHandlerConfiguration = burstTurboLogEventFilterHandlerConfiguration;
            return this;
        }

        public Builder duplicateLogTurboFilterHandlerConfiguration(
                final DuplicateLogTurboFilterHandlerConfiguration duplicateLogTurboFilterHandlerConfiguration)
        {
            this.duplicateLogTurboFilterHandlerConfiguration = duplicateLogTurboFilterHandlerConfiguration;
            return this;
        }

        public Builder disruptorLogAndTurboLogEventFilterDelegatorConfiguration(
                final DisruptorLogAndTurboLogEventFilterDelegatorConfiguration
                        disruptorLogAndTurboLogEventFilterDelegatorConfiguration)
        {
            this.disruptorLogAndTurboLogEventFilterDelegatorConfiguration =
                    disruptorLogAndTurboLogEventFilterDelegatorConfiguration;
            return this;
        }

        public Builder luceneLogAndTurboLogEventFilterListenerConfiguration(
                final LuceneLogAndTurboLogEventFilterListenerConfiguration
                        luceneLogAndTurboLogEventFilterListenerConfiguration)
        {
            this.luceneLogAndTurboLogEventFilterListenerConfiguration =
                    luceneLogAndTurboLogEventFilterListenerConfiguration;
            return this;
        }

        public Builder metricsTurboFilterHandlerConfiguration(
                final MetricsTurboFilterHandlerConfiguration metricsTurboFilterHandlerConfiguration)
        {
            this.metricsTurboFilterHandlerConfiguration = metricsTurboFilterHandlerConfiguration;
            return this;
        }

        public DefaultLogServiceConfiguration build() {
            return new DefaultLogServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultLogServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultLogServiceConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
