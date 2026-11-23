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
 * An event sent to {@link ConfigurationListener} when a configuration was removed.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@ConfigurationRemovedEvent.Valid
public final class ConfigurationRemovedEvent {
    @NotNull
    private final ConfigurationKey configurationKey;

    @Positive
    private final long timestamp;

    @JsonCreator
    public ConfigurationRemovedEvent(@JsonProperty("configurationKey")
                                     final ConfigurationKey configurationKey,
                                     @JsonProperty("timestamp")
                                     final long timestamp)
    {
        ConfigurationRemovedEventValidator.validate(configurationKey, timestamp);

        this.configurationKey = configurationKey;
        this.timestamp = timestamp;
    }

    public ConfigurationKey getConfigurationKey() {
        return configurationKey;
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

        final ConfigurationRemovedEvent that = (ConfigurationRemovedEvent) o;
        return timestamp == that.timestamp &&
                Objects.equals(configurationKey, that.configurationKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configurationKey, timestamp);
    }

    @Override
    public String toString() {
        return "ConfigurationRemovedEvent{" +
                "configurationKey=" + configurationKey +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * JSR 303 validation constraint annotation for {@link ConfigurationRemovedEvent}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ConfigurationRemovedEventValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
