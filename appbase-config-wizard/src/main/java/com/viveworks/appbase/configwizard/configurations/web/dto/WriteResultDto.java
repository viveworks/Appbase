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
 * Result of building and writing a configuration file via
 * {@code POST /api/configs/{fqcn}/write}.
 *
 * <p>On success, {@code path} contains the path of the written file relative to the wizard's root
 * (never the absolute filesystem path — A10-01, internal work item 1449) and {@code errorMessage} is {@code null}. On
 * failure, {@code path} is {@code null} and {@code errorMessage} describes the problem.
 *
 * @param path         path of the written YAML file relative to the wizard's root, or {@code null} on error
 * @param errorMessage description of the failure, or {@code null} on success
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Schema(description = "Result of building and writing a configuration file to disk via POST /{fqcn}/write. "
        + "Exactly one of 'path' or 'errorMessage' will be non-null.")
public record WriteResultDto(
        @Schema(description = "Path of the YAML file that was written, relative to the wizard's root "
                + "(not the absolute filesystem path). Non-null only on success.",
                nullable = true,
                example = "configs/DefaultKafkaClientConfiguration.yaml")
        @Nullable String path,

        @Schema(description = "Description of the I/O or build failure. Non-null only on failure.",
                nullable = true,
                example = "Permission denied: /etc/appbase/kafka.yaml")
        @Nullable String errorMessage
) {
}
