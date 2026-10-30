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

package com.viveworks.appbase.client.service.jpa.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.service.jpa.DefaultJpaClientIntegrationService;
import com.viveworks.appbase.core.service.exception.store.EntityManagerConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Default implementation of {@link DefaultJpaClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultJpaClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultJpaClientIntegrationServiceConfiguration implements JpaClientIntegrationServiceConfiguration {
    private final TextValueConfiguration textValueKeyConfiguration;
    private final EntityManagerConfiguration entityManagerConfiguration;

    private DefaultJpaClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.textValueKeyConfiguration, "builder.textValueKeyConfiguration");
        AppbaseUtil.notNull(builder.entityManagerConfiguration, "builder.entityManagerConfiguration");

        textValueKeyConfiguration = builder.textValueKeyConfiguration;
        entityManagerConfiguration = builder.entityManagerConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public TextValueConfiguration getTextValueKeyConfiguration() {
        return textValueKeyConfiguration;
    }

    public EntityManagerConfiguration getEntityManagerConfiguration() {
        return entityManagerConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultJpaClientIntegrationServiceConfiguration that = (DefaultJpaClientIntegrationServiceConfiguration) o;
        return Objects.equals(textValueKeyConfiguration, that.textValueKeyConfiguration) &&
                Objects.equals(entityManagerConfiguration, that.entityManagerConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(textValueKeyConfiguration, entityManagerConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultJpaClientIntegrationServiceConfiguration{" +
                "textValueKeyConfiguration=" + textValueKeyConfiguration +
                ", entityManagerConfiguration=" + entityManagerConfiguration +
                '}';
    }


    /**
     * Builder for {@link DefaultJpaClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final TextValueConfiguration DEFAULT_TEXT_VALUE_KEY_CONFIGURATION = TextValueConfiguration.PRIMARY;
        private static final EntityManagerConfiguration DEFAULT_ENTITY_MANAGER_CONFIGURATION =
                new EntityManagerConfiguration("jpaClientServicePersistenceUnit", null, null);


        private TextValueConfiguration textValueKeyConfiguration = DEFAULT_TEXT_VALUE_KEY_CONFIGURATION;
        private EntityManagerConfiguration entityManagerConfiguration = DEFAULT_ENTITY_MANAGER_CONFIGURATION;


        private Builder() {
        }

        public Builder textValueKeyConfiguration(final TextValueConfiguration textValueKeyConfiguration) {
            this.textValueKeyConfiguration = textValueKeyConfiguration;
            return this;
        }

        public Builder entityManagerConfiguration(final EntityManagerConfiguration entityManagerConfiguration) {
            this.entityManagerConfiguration = entityManagerConfiguration;
            return this;
        }

        public DefaultJpaClientIntegrationServiceConfiguration build() {
            return new DefaultJpaClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
