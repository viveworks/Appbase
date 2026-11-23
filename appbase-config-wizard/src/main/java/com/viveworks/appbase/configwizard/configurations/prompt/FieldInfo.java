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

package com.viveworks.appbase.configwizard.configurations.prompt;

import javax.annotation.Nullable;

/**
 * Metadata for one Jackson-visible property on an Appbase config class.
 *
 * <p>{@code required} is {@code true} for Java primitive types ({@code boolean}, {@code int},
 * {@code long}, etc.) which cannot hold {@code null}, and {@code false} for reference types
 * that are optional in JSON/YAML.
 *
 * <p>{@code nestedClass} is non-null when the property type is itself a complex buildable
 * type (annotated with {@code @JsonDeserialize(builder=…)}) or implements
 * {@link com.viveworks.appbase.core.service.AppbaseConfiguration} — the prompter recurses
 * into it during interactive prompting.
 *
 * @param name             Jackson property name as it will appear in YAML (e.g. "commandTimeoutMs")
 * @param type             raw Java class for the property (e.g. {@code String.class}, {@code int.class})
 * @param typeName         human-readable type string shown in prompts (e.g. "String", "int", "boolean")
 * @param defaultValue     Java object derived from the defaultInstance (String/Integer/Boolean/etc.),
 *                         or {@code null} when no default is available
 * @param required         {@code true} when the property type is a Java primitive (non-nullable)
 * @param nestedClass      non-null when this property's type is itself a buildable configuration
 * @param collectionHint   human-readable generic type for Map/List/Set fields
 *                         (e.g. {@code "Map<String, List<String>>"}, {@code "List<String>"}),
 *                         {@code null} for non-collection fields
 * @param elementClass     non-null when the collection element type (or map value type) is itself a
 *                         buildable nested configuration; prompts open a sub-form per element
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public record FieldInfo(
        String name,
        Class<?> type,
        String typeName,
        @Nullable Object defaultValue,
        boolean required,
        @Nullable Class<?> nestedClass,
        @Nullable String collectionHint,
        /**
         * Non-null when this field is a Map/List/Set whose element type (or map value type)
         * is itself a buildable nested configuration object. When non-null, collection
         * handlers should open a sub-form for each added element/value rather than prompting
         * for raw text.
         */
        @Nullable Class<?> elementClass
) {
}
