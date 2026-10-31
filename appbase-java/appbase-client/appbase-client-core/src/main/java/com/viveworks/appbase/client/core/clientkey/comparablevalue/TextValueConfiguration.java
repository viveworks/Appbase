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

package com.viveworks.appbase.client.core.clientkey.comparablevalue;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents configuration for {@link TextValue}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = TextValueConfiguration.Builder.class)
public final class TextValueConfiguration {
    public static final TextValueConfiguration PRIMARY = TextValueConfiguration.builder().text("primary").build();

    private final String text;

    private TextValueConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.text, "builder.text");

        text = builder.text;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TextValueConfiguration that = (TextValueConfiguration) o;
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    @Override
    public String toString() {
        return "TextValueConfiguration{" +
                "text='" + text + '\'' +
                '}';
    }

    /**
     * Builder for {@link TextValueConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private String text;

        private Builder() {
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public TextValueConfiguration build() {
            return new TextValueConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
