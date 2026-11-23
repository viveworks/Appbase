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

package com.viveworks.appbase.core.service.exception.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.util.AppbaseUtil;
import jakarta.validation.constraints.NotBlank;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration to create {@link jakarta.persistence.EntityManager}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
public class EntityManagerConfiguration {
    private final String persistenceUnitName;

    @Nullable
    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> entityManagerFactoryProperties;

    @Nullable
    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> entityManagerProperties;

    @JsonCreator
    public EntityManagerConfiguration(
            @NotBlank
            @JsonProperty("persistenceUnitName")
            final String persistenceUnitName,
            @Nullable
            @JsonProperty("entityManagerFactoryProperties")
            @JsonSerialize(keyAs = String.class, contentAs = String.class)
            @JsonDeserialize(keyAs = String.class, contentAs = String.class)
            final Map<String, String> entityManagerFactoryProperties,
            @Nullable
            @JsonProperty("entityManagerProperties")
            @JsonSerialize(keyAs = String.class, contentAs = String.class)
            @JsonDeserialize(keyAs = String.class, contentAs = String.class)
            final Map<String, String> entityManagerProperties)
    {
        AppbaseUtil.notBlank(persistenceUnitName, "persistenceUnitName");

        this.persistenceUnitName = persistenceUnitName;
        if (entityManagerFactoryProperties != null) {
            this.entityManagerFactoryProperties = new HashMap<>(entityManagerFactoryProperties);
        } else {
            this.entityManagerFactoryProperties = null;
        }

        if (entityManagerProperties != null) {
            this.entityManagerProperties = new HashMap<>(entityManagerProperties);
        } else {
            this.entityManagerProperties = null;
        }
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    @Nullable
    public Map<String, String> getEntityManagerFactoryProperties() {
        return entityManagerFactoryProperties;
    }

    @Nullable
    public Map<String, String> getEntityManagerProperties() {
        return entityManagerProperties;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final EntityManagerConfiguration that = (EntityManagerConfiguration) o;
        return Objects.equals(persistenceUnitName, that.persistenceUnitName) &&
                Objects.equals(entityManagerFactoryProperties, that.entityManagerFactoryProperties) &&
                Objects.equals(entityManagerProperties, that.entityManagerProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(persistenceUnitName, entityManagerFactoryProperties, entityManagerProperties);
    }

    @Override
    public String toString() {
        return "EntityManagerConfiguration{" +
                "persistenceUnitName='" + persistenceUnitName + '\'' +
                ", entityManagerFactoryProperties=" + AppbaseUtil.OBFUSCATE +
                ", entityManagerProperties=" + AppbaseUtil.OBFUSCATE +
                '}';
    }
}
