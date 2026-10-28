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

/**
 * Result of building and validating a configuration object via
 * {@code POST /api/configs/{fqcn}/build}.
 *
 * <p>On success, {@code yaml} contains the serialized YAML string and {@code errorMessage} is
 * {@code null}. On failure (validation errors or conversion failure), {@code yaml} is {@code null}
 * and {@code errorMessage} describes the problem.
 *
 * @param yaml         the YAML string representation of the built configuration, or {@code null} on error
 * @param errorMessage validation errors or conversion failure description, or {@code null} on success
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Schema(description = "Result of building and validating a configuration object via POST /{fqcn}/build. "
        + "Exactly one of 'yaml' or 'errorMessage' will be non-null.")
public record BuildResultDto(
        @Schema(description = "YAML string representation of the built and validated configuration object. "
                + "Non-null only on success.",
                nullable = true,
                example = "bootstrapServers: localhost:9092\ngroupId: my-group\n")
        @Nullable String yaml,

        @Schema(description = "Comma-separated list of Hibernate Validator constraint violations, or a "
                + "description of the conversion failure. Non-null only on failure.",
                nullable = true,
                example = "bootstrapServers: must not be blank")
        @Nullable String errorMessage
) {
}
