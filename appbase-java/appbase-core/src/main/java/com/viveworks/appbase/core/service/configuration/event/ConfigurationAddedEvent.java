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

package com.viveworks.appbase.core.service.configuration.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/**
 * An event sent to {@link ConfigurationListener} when a configuration was added.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@ConfigurationAddedEvent.Valid
public final class ConfigurationAddedEvent {
    @NotNull
    private final ConfigurationKey configurationKey;

    @NotNull
    private final ConfigurationValue<?> configurationValue;

    @Positive
    private final long timestamp;

    @JsonCreator
    public ConfigurationAddedEvent(@JsonProperty("configurationKey")
                                   final ConfigurationKey configurationKey,
                                   @JsonProperty("configurationValue")
                                   final ConfigurationValue<?> configurationValue,
                                   @JsonProperty("timestamp")
                                   final long timestamp)
    {
        ConfigurationAddedEventValidator.validate(configurationKey, configurationValue, timestamp);

        this.configurationKey = configurationKey;
        this.configurationValue = configurationValue;
        this.timestamp = timestamp;
    }

    public ConfigurationKey getConfigurationKey() {
        return configurationKey;
    }

    public ConfigurationValue<?> getConfigurationValue() {
        return configurationValue;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ConfigurationAddedEvent that = (ConfigurationAddedEvent) o;
        return timestamp == that.timestamp &&
                Objects.equals(configurationKey, that.configurationKey) &&
                Objects.equals(configurationValue, that.configurationValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configurationKey, configurationValue, timestamp);
    }

    @Override
    public String toString() {
        return "ConfigurationAddedEvent{" +
                "configurationKey=" + configurationKey +
                ", configurationValue=" + configurationValue +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * JSR 303 validation constraint for {@link ConfigurationAddedEvent}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ConfigurationAddedEventValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
