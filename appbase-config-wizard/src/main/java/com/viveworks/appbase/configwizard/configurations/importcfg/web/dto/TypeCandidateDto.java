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

package com.viveworks.appbase.configwizard.configurations.importcfg.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * A scored candidate Appbase configuration type for an imported YAML file.
 *
 * <p>Confidence is in the range {@code 0.0–1.0}; a value of {@code >= 0.7} is
 * considered "Suggested" by the type-inference algorithm.
 *
 * @param fqcn       fully qualified class name of the candidate config class
 * @param simpleName simple name of the candidate config class
 * @param confidence confidence score in range {@code 0.0–1.0}
 * @param signals    human-readable list of scoring signals (e.g. "filename-match", "field-overlap")
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Schema(description = "A scored candidate Appbase configuration type inferred for an imported YAML file.")
public record TypeCandidateDto(
        @Schema(description = "Fully qualified class name of the candidate config class.",
                example = "com.viveworks.appbase.client.service.redis.configuration.DefaultRedisClientIntegrationServiceConfiguration")
        String fqcn,

        @Schema(description = "Simple class name of the candidate config class.",
                example = "DefaultRedisClientIntegrationServiceConfiguration")
        String simpleName,

        @Schema(description = "Confidence score in range 0.0–1.0. Values >= 0.7 are considered 'Suggested'.",
                minimum = "0.0", maximum = "1.0", example = "0.85")
        double confidence,

        @Schema(description = "Human-readable scoring signals that contributed to the confidence.",
                example = "[\"filename-match\", \"field-overlap\"]")
        List<String> signals
) {
}
