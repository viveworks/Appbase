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

package com.viveworks.appbase.core.util.di;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * Configuration for an injected object.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 * @param <T> Type of the object being injected
 */
@Immutable
@JsonDeserialize(builder = InjectedTypeConfiguration.Builder.class)
public final class InjectedTypeConfiguration<T> {
    private final Class<T> objectType;

    @Nullable
    private final String qualifierName;

    @Nullable
    private final Class<? extends Annotation> qualifierAnnotationType;


    private InjectedTypeConfiguration(final Builder<T> builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.objectType, "builder.objectType");

        objectType = builder.objectType;
        qualifierName = builder.qualifierName;
        qualifierAnnotationType = builder.qualifierAnnotationType;
    }

    /**
     * Get the builder used to build this object.
     *
     * @param <T> Type of the object being injected
     * @return Builder for this object
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public Class<T> getObjectType() {
        return objectType;
    }

    @Nullable
    public String getQualifierName() {
        return qualifierName;
    }

    @Nullable
    public Class<? extends Annotation> getQualifierAnnotationType() {
        return qualifierAnnotationType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final InjectedTypeConfiguration<?> that = (InjectedTypeConfiguration<?>) o;
        return Objects.equals(objectType, that.objectType) &&
                Objects.equals(qualifierName, that.qualifierName) &&
                Objects.equals(qualifierAnnotationType, that.qualifierAnnotationType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectType, qualifierName, qualifierAnnotationType);
    }

    @Override
    public String toString() {
        return "InjectedTypeConfiguration{" +
                "objectType=" + objectType +
                ", qualifierName='" + qualifierName + '\'' +
                ", qualifierAnnotationType=" + qualifierAnnotationType +
                '}';
    }

    /**
     * Builder for {@link InjectedTypeConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder<T> {
        private Class<T> objectType;

        @Nullable
        private String qualifierName;

        @Nullable
        private Class<? extends Annotation> qualifierAnnotationType;


        private Builder() {
        }

        public Builder<T> objectType(final Class<T> objectType) {
            this.objectType = objectType;
            return this;
        }

        public Builder<T> qualifierName(@Nullable final String qualifierName) {
            this.qualifierName = qualifierName;
            return this;
        }

        public Builder<T> qualifierAnnotationType(@Nullable final Class<? extends Annotation> qualifierAnnotationType) {
            this.qualifierAnnotationType = qualifierAnnotationType;
            return this;
        }

        public InjectedTypeConfiguration<T> build() {
            return new InjectedTypeConfiguration<>(this);
        }
    }
    // CheckStyle: Builder end
}
