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

package com.viveworks.appbase.core.service.configuration.accessor.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import javax.annotation.Nullable;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;
import java.util.Objects;

/**
 * Represents configuration stored in a data store.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Entity(name = "Configuration")
@Table(
        name = "Configuration",
        indexes = {
                @Index(name = "Index_Configuration_discriminator", columnList = "discriminator"),
                @Index(name = "Index_Configuration_key", columnList = "key"),
                @Index(name = "Index_Configuration_keyJavaTypeName", columnList = "keyJavaTypeName"),
        }
)
public class ConfigurationEntity {
    public static final String ID_RAW_CONFIGURATION_CONTEXT_KEY = "ID";
    public static final String DISCRIMINATOR_RAW_CONFIGURATION_CONTEXT_KEY = "DISCRIMINATOR";
    public static final String KEY_JAVA_TYPE_NAME_RAW_CONFIGURATION_CONTEXT_KEY = "KEY_JAVA_TYPE_NAME";
    public static final String LAST_UPDATED_TIMESTAMP_RAW_CONFIGURATION_CONTEXT_KEY = "LAST_UPDATED_TIMESTAMP";
    public static final String LAST_UPDATED_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private static final int MAX_LENGTH = 1024;

    @NotNull(groups = ReadChecks.class)
    @Positive(groups = ReadChecks.class)
    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @NotBlank(groups = {ReadChecks.class, WriteChecks.class})
    @Basic(optional = false)
    @Column(nullable = false, length = MAX_LENGTH)
    private String discriminator;

    @NotBlank(groups = {ReadChecks.class, WriteChecks.class})
    @Basic(optional = false)
    @Column(nullable = false, length = MAX_LENGTH)
    private String key;

    @Nullable
    @Basic
    @Column(length = MAX_LENGTH)
    private String keyJavaTypeName;

    @NotBlank(groups = {ReadChecks.class, WriteChecks.class})
    @Lob
    @Basic(optional = false, fetch = FetchType.EAGER)
    @Column(nullable = false)
    private String value;

    @NotBlank(groups = {ReadChecks.class, WriteChecks.class})
    @Basic(optional = false)
    @Column(nullable = false, length = MAX_LENGTH)
    private String valueMimeType;

    @NotNull(groups = {ReadChecks.class, WriteChecks.class})
    @Basic(optional = false)
    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdatedTimestamp;

    public ConfigurationEntity() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public void setDiscriminator(final String discriminator) {
        this.discriminator = discriminator;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    @Nullable
    public String getKeyJavaTypeName() {
        return keyJavaTypeName;
    }

    public void setKeyJavaTypeName(@Nullable final String keyJavaTypeName) {
        this.keyJavaTypeName = keyJavaTypeName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public String getValueMimeType() {
        return valueMimeType;
    }

    public void setValueMimeType(final String valueMimeType) {
        this.valueMimeType = valueMimeType;
    }

    public Date getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public void setLastUpdatedTimestamp(final Date lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ConfigurationEntity that = (ConfigurationEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(discriminator, that.discriminator) &&
                Objects.equals(key, that.key) &&
                Objects.equals(keyJavaTypeName, that.keyJavaTypeName) &&
                Objects.equals(value, that.value) &&
                Objects.equals(valueMimeType, that.valueMimeType) &&
                Objects.equals(lastUpdatedTimestamp, that.lastUpdatedTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, discriminator, key, keyJavaTypeName, value, valueMimeType, lastUpdatedTimestamp);
    }

    @Override
    public String toString() {
        return "ConfigurationEntity{" +
                "id=" + id +
                ", discriminator='" + discriminator + '\'' +
                ", key='" + key + '\'' +
                ", keyJavaTypeName='" + keyJavaTypeName + '\'' +
                ", value='" + value + '\'' +
                ", valueMimeType='" + valueMimeType + '\'' +
                ", lastUpdatedTimestamp=" + lastUpdatedTimestamp +
                '}';
    }


    /**
     * Used for validation checks during read operations.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    public interface ReadChecks {
    }


    /**
     * Used for validation checks during write operations.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    public interface WriteChecks {
    }
}
