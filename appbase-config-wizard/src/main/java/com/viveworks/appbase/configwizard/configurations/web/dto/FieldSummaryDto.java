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

package com.viveworks.appbase.configwizard.configurations.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Metadata for a single field on an Appbase configuration class.
 *
 * <p>Used inside {@link ConfigDetailDto#fields()} to describe each property visible
 * in YAML output.
 *
 * @param name             Jackson property name as it will appear in the YAML file (e.g. "commandTimeoutMs")
 * @param typeName         human-readable simple type string (e.g. "String", "int", "boolean")
 * @param required         {@code true} when the property type is a Java primitive and cannot be null
 * @param defaultValue     string representation of the default value from the class's default instance,
 *                         or {@code null} when no default is available, the field is optional, or the
 *                         field is a nested config object. Always a String — never a raw boolean or number —
 *                         so that the Flutter client can parse it without type gymnastics.
 * @param nestedTypeFqcn   fully-qualified class name of the nested config type when this field holds an
 *                         Appbase configuration object (e.g. "com.viveworks.appbase…HostPortConfiguration"),
 *                         or {@code null} for primitive, String, and non-config fields. When non-null, the
 *                         Flutter client should render a sub-form rather than a plain text input.
 * @param enumValues             ordered list of enum constant names when the field type is a Java enum (e.g.
 *                               {@code ["SEQUENCE","PARALLEL","RANDOM"]}); {@code null} for non-enum fields.
 *                               Clients can use this to render a dropdown instead of free-text input.
 * @param elementNestedTypeFqcn  FQCN of the collection element's nested config type, or {@code null}.
 *                               When non-null the UI should render a sub-form button for each added
 *                               element instead of a plain text field.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Schema(description = "Metadata for a single configurable field on an Appbase configuration class. "
        + "Used by the UI to render the appropriate input widget and pre-populate defaults.")
public record FieldSummaryDto(
        @Schema(description = "Jackson property name as it appears in the generated YAML file",
                example = "bootstrapServers")
        String name,

        @Schema(description = "Human-readable simple type name of the field",
                example = "String",
                allowableValues = {"String", "int", "long", "boolean", "double", "float",
                        "Integer", "Long", "Boolean", "Double", "Float"})
        String typeName,

        @Schema(description = "True when the field is a Java primitive and therefore cannot be null; "
                + "the UI should enforce a non-empty value",
                example = "false")
        boolean required,

        @Schema(description = "String representation of the default value taken from the class default instance. "
                + "Always a String (never a raw boolean or number) so the Flutter client can parse without type "
                + "gymnastics. Null when no default is available, the field is optional, or the field holds a "
                + "nested configuration object.",
                nullable = true,
                example = "localhost:9092")
        @Nullable String defaultValue,

        @Schema(description = "Fully qualified class name of the nested Appbase configuration type when this "
                + "field holds another configuration object. When non-null the UI should render a sub-form "
                + "instead of a plain text input. Null for primitive, String, and non-configuration fields.",
                nullable = true,
                example = "com.viveworks.appbase.core.configuration.HostPortConfiguration")
        @Nullable String nestedTypeFqcn,

        @Schema(description = "Ordered list of valid enum constant names when the field type is a Java enum. "
                + "Null for non-enum fields. Clients should render a dropdown when this is non-null.",
                nullable = true,
                example = "[\"SEQUENCE\",\"PARALLEL\",\"RANDOM\"]")
        @Nullable List<String> enumValues,

        @Schema(description = "FQCN of the nested config type for collection element values. "
                + "When non-null the UI renders a sub-form for each Map value / List element "
                + "instead of a plain text field.",
                nullable = true)
        @Nullable String elementNestedTypeFqcn
) {
}
