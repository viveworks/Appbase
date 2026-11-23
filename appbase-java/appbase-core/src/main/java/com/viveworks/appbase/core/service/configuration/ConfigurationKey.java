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

package com.viveworks.appbase.core.service.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents a configuration key.
 *
 * <p>
 *     Keys are of 2 types:
 * </p>
 * <ul>
 *     <li>Property key ({@link #propertyKey}): These refer to a property.</li>
 *     <li>Type key ({@link #typeKey}): These refer to a <code>Java type</code></li>
 * </ul>
 *
 * <p>
 *     Additionally, this keeps track of the {@link ConfigurationAccessor} that actually loaded this key (and its
 *     value).
 * </p>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@SuppressWarnings("checkstyle:WeakerAccess")
public final class ConfigurationKey implements Comparable<ConfigurationKey> {
    private final String propertyKey;
    private final Class<? extends AppbaseConfiguration> typeKey;
    private final Class<? extends ConfigurationAccessor> source;

    /**
     * Creates an unscoped property key.
     *
     * @param propertyKey nonblank property name
     * @throws AppbaseRuntimeException if {@code propertyKey} is null or blank
     */
    public ConfigurationKey(final String propertyKey) {
        AppbaseUtil.notBlank(propertyKey, "propertyKey");

        this.propertyKey = propertyKey;
        this.typeKey = null;
        this.source = null;
    }

    /**
     * Creates an unscoped typed-configuration key.
     *
     * @param typeKey configuration interface or implementation class
     * @throws AppbaseRuntimeException if {@code typeKey} is {@code null}
     */
    public ConfigurationKey(final Class<? extends AppbaseConfiguration> typeKey) {
        AppbaseUtil.notNull(typeKey, "typeKey");

        this.typeKey = typeKey;
        this.propertyKey = null;
        this.source = null;
    }

    /**
     * Creates a property key scoped to the accessor that loaded it.
     *
     * @param propertyKey nonblank property name
     * @param source accessor implementation that supplied the property
     * @throws AppbaseRuntimeException if either argument is null, or the name is blank
     */
    public ConfigurationKey(final String propertyKey, final Class<? extends ConfigurationAccessor> source) {
        AppbaseUtil.notBlank(propertyKey, "propertyKey");
        AppbaseUtil.notNull(source, "source");

        this.propertyKey = propertyKey;
        this.source = source;
        this.typeKey = null;
    }

    /**
     * Creates a typed key scoped to the accessor that loaded it.
     *
     * @param typeKey configuration interface or implementation class
     * @param source accessor implementation that supplied the object
     * @throws AppbaseRuntimeException if either argument is {@code null}
     */
    public ConfigurationKey(final Class<? extends AppbaseConfiguration> typeKey,
                            final Class<? extends ConfigurationAccessor> source)
    {
        AppbaseUtil.notNull(typeKey, "typeKey");
        AppbaseUtil.notNull(source, "source");

        this.typeKey = typeKey;
        this.source = source;
        this.propertyKey = null;
    }

    /**
     * Reconstructs a key from JSON fields; a type takes precedence in {@link #getKey()} when both forms are present.
     *
     * @param propertyKey property name, possibly {@code null} when {@code typeKey} is present
     * @param typeKey configuration class, possibly {@code null} when a nonblank property name is present
     * @param source accessor implementation that supplied the value, possibly {@code null}
     * @throws AppbaseRuntimeException if no type and no nonblank property name is supplied
     */
    @JsonCreator
    @SuppressFBWarnings("WEM_WEAK_EXCEPTION_MESSAGING")
    public ConfigurationKey(@JsonProperty("propertyKey")
                            final String propertyKey,
                            @JsonProperty("typeKey")
                            final Class<? extends AppbaseConfiguration> typeKey,
                            @Nullable
                            @JsonProperty("source")
                            final Class<? extends ConfigurationAccessor> source)
    {
        if (typeKey == null && AppbaseUtil.isBlank(propertyKey)) {
            throw new AppbaseRuntimeException("Either propertyKey or typeKey must be set");
        }

        this.propertyKey = propertyKey;
        this.typeKey = typeKey;
        this.source = source;
    }

    /**
     * Builds this object using a property key.
     *
     * @param propertyKey Property key
     * @return an instance of this class
     */
    public static ConfigurationKey fromPropertyKey(final String propertyKey) {
        return new ConfigurationKey(propertyKey);
    }

    /**
     * Builds this object using a type key.
     *
     * @param typeKey Type key
     * @return an instance of this class
     */
    public static ConfigurationKey fromTypeKey(final Class<? extends AppbaseConfiguration> typeKey) {
        return new ConfigurationKey(typeKey);
    }

    /**
     * Builds this object using a property key and source.
     *
     * @param propertyKey Property key
     * @param source Source configuration loader
     * @return an instance of this class
     */
    public static ConfigurationKey from(final String propertyKey, final Class<? extends ConfigurationAccessor> source) {
        return new ConfigurationKey(propertyKey, source);
    }

    /**
     * Builds this object using type key and source.
     *
     * @param typeKey Type key
     * @param source Source configuration loader
     * @return an instance of this class
     */
    public static ConfigurationKey from(final Class<? extends AppbaseConfiguration> typeKey,
                                        final Class<? extends ConfigurationAccessor> source)
    {
        return new ConfigurationKey(typeKey, source);
    }

    /**
     * Builds this object using property key, type key and source.
     *
     * @param propertyKey Property key
     * @param typeKey Type key
     * @param source configuration loader
     * @return an instance of this class
     */
    public static ConfigurationKey from(final String propertyKey,
                                        final Class<? extends AppbaseConfiguration> typeKey,
                                        final Class<? extends ConfigurationAccessor> source)
    {
        return new ConfigurationKey(propertyKey, typeKey, source);
    }

    /**
     * Returns {@code true} when this key contains a configuration class.
     *
     * @return {@code true} when this key contains a configuration class
     */
    public boolean isTyped() {
        return typeKey != null;
    }

    /**
     * Returns the property name, or {@code null} for a type-only key.
     *
     * @return the property name, or {@code null} for a type-only key
     */
    public String getPropertyKey() {
        return propertyKey;
    }

    /**
     * Returns the configuration class, or {@code null} for a property-only key.
     *
     * @return the configuration class, or {@code null} for a property-only key
     */
    public Class<? extends AppbaseConfiguration> getTypeKey() {
        return typeKey;
    }

    /**
     * Returns the originating accessor type, or {@code null} for an unscoped key.
     *
     * @return the originating accessor type, or {@code null} for an unscoped key
     */
    public Class<? extends ConfigurationAccessor> getSource() {
        return source;
    }

    /**
     * Selects the class name for a typed key and the property name otherwise.
     * @return the canonical string used to index this key
     */
    public String getKey() {
        if (isTyped()) {
            return typeKey.getName();
        }

        return propertyKey;
    }

    @Override
    public int compareTo(@Nonnull final ConfigurationKey other) {
        return Integer.compare(this.hashCode(), other.hashCode());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ConfigurationKey that = (ConfigurationKey) o;
        return Objects.equals(propertyKey, that.propertyKey) &&
                Objects.equals(typeKey, that.typeKey) &&
                Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyKey, typeKey, source);
    }

    @Override
    public String toString() {
        return "ConfigurationKey{" +
                "propertyKey='" + propertyKey + '\'' +
                ", typeKey=" + typeKey +
                ", source=" + source +
                '}';
    }
}
