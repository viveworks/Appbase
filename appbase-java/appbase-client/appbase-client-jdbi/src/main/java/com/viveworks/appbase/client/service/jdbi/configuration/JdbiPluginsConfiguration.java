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

/**
 * This package contains client services for {@code JDBC} (Java Database Connectivity).
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */

package com.viveworks.appbase.client.service.jdbi.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.service.jdbi.configuration.factory.JdbiPluginsFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jdbi.v3.core.spi.JdbiPlugin;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents configuration for {@link JdbiPlugin}s that must be installed.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = JdbiPluginsConfiguration.Builder.class)
public final class JdbiPluginsConfiguration {
    private final boolean installSqlObjectPlugin;

    private final InjectedTypeConfiguration<JdbiPluginsFactory> jdbiPluginsFactoryInjectedTypeConfiguration;

    private JdbiPluginsConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        installSqlObjectPlugin = builder.installSqlObjectPlugin;
        jdbiPluginsFactoryInjectedTypeConfiguration = builder.jdbiPluginsFactoryInjectedTypeConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isInstallSqlObjectPlugin() {
        return installSqlObjectPlugin;
    }

    public InjectedTypeConfiguration<JdbiPluginsFactory> getJdbiPluginsFactoryInjectedTypeConfiguration() {
        return jdbiPluginsFactoryInjectedTypeConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JdbiPluginsConfiguration that = (JdbiPluginsConfiguration) o;
        return installSqlObjectPlugin == that.installSqlObjectPlugin &&
                Objects.equals(
                        jdbiPluginsFactoryInjectedTypeConfiguration, that.jdbiPluginsFactoryInjectedTypeConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(installSqlObjectPlugin, jdbiPluginsFactoryInjectedTypeConfiguration);
    }

    @Override
    public String toString() {
        return "JdbiPluginsConfiguration{" +
                "installSqlObjectPlugin=" + installSqlObjectPlugin +
                ", jdbiPluginsFactoryInjectedTypeConfiguration=" + jdbiPluginsFactoryInjectedTypeConfiguration +
                '}';
    }


    /**
     * Builder for {@link JdbiPluginsConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_INSTALL_SQL_OBJECT_PLUGIN = true;

        private boolean installSqlObjectPlugin = DEFAULT_INSTALL_SQL_OBJECT_PLUGIN;
        private InjectedTypeConfiguration<JdbiPluginsFactory> jdbiPluginsFactoryInjectedTypeConfiguration;

        private Builder() {
        }

        public Builder installSqlObjectPlugin(final boolean installSqlObjectPlugin) {
            this.installSqlObjectPlugin = installSqlObjectPlugin;
            return this;
        }

        public Builder jdbiPluginsFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<JdbiPluginsFactory> jdbiPluginsFactoryInjectedTypeConfiguration)
        {
            this.jdbiPluginsFactoryInjectedTypeConfiguration = jdbiPluginsFactoryInjectedTypeConfiguration;
            return this;
        }

        public JdbiPluginsConfiguration build() {
            return new JdbiPluginsConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
