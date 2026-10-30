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
 * An event sent to {@link ConfigurationListener} when a configuration changed.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@ConfigurationChangedEvent.Valid
public final class ConfigurationChangedEvent {
    @NotNull
    private final ConfigurationKey configurationKey;

    @NotNull
    private final ConfigurationValue<?> oldConfigurationValue;

    @NotNull
    private final ConfigurationValue<?> newConfigurationValue;

    @Positive
    private final long timestamp;

    @JsonCreator
    public ConfigurationChangedEvent(@JsonProperty("configurationKey")
                                     final ConfigurationKey configurationKey,
                                     @JsonProperty("oldConfigurationValue")
                                     final ConfigurationValue<?> oldConfigurationValue,
                                     @JsonProperty("newConfigurationValue")
                                     final ConfigurationValue<?> newConfigurationValue,
                                     @JsonProperty("timestamp")
                                     final long timestamp)
    {
        ConfigurationChangedEventValidator.validate(
                configurationKey, oldConfigurationValue, newConfigurationValue, timestamp);

        this.configurationKey = configurationKey;
        this.oldConfigurationValue = oldConfigurationValue;
        this.newConfigurationValue = newConfigurationValue;
        this.timestamp = timestamp;
    }

    public ConfigurationKey getConfigurationKey() {
        return configurationKey;
    }

    public ConfigurationValue<?> getOldConfigurationValue() {
        return oldConfigurationValue;
    }

    public ConfigurationValue<?> getNewConfigurationValue() {
        return newConfigurationValue;
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

        final ConfigurationChangedEvent that = (ConfigurationChangedEvent) o;
        return timestamp == that.timestamp &&
                Objects.equals(configurationKey, that.configurationKey) &&
                Objects.equals(oldConfigurationValue, that.oldConfigurationValue) &&
                Objects.equals(newConfigurationValue, that.newConfigurationValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                configurationKey,
                oldConfigurationValue,
                newConfigurationValue,
                timestamp);
    }

    @Override
    public String toString() {
        return "ConfigurationChangedEvent{" +
                "configurationKey=" + configurationKey +
                ", oldConfigurationValue=" + oldConfigurationValue +
                ", newConfigurationValue=" + newConfigurationValue +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * JSR 303 validation constraint annotation for {@link ConfigurationChangedEvent}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ConfigurationChangedEventValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
