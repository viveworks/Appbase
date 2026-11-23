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

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Request body for writing an imported YAML configuration file.
 *
 * <p>Output path priority:
 * <ol>
 *   <li>{@code outputPath} is non-null → write to that path (creates parent dirs)</li>
 *   <li>{@code outputPath} is null → write to a system temp location</li>
 * </ol>
 *
 * @param sourceFile  absolute path of the original YAML file being imported; may be {@code null}
 * @param fieldValues map of Jackson property names to override values; empty map uses all defaults
 * @param outputPath  explicit target path for the written file; {@code null} writes to temp
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Schema(description = "Request body for writing an imported Appbase configuration YAML file to disk.")
public record ImportWriteRequestDto(
        @Schema(description = "Absolute path of the original YAML file being imported. "
                + "Informational only — used to identify the source when outputPath is null.",
                nullable = true,
                example = "/home/dev/configs/redis.yaml")
        @Nullable String sourceFile,

        @Schema(description = "Map of Jackson property names to override values. "
                + "Omitted keys fall back to class defaults. Pass an empty object {} for all defaults.",
                example = "{\"host\":\"prod-redis:6379\",\"ssl\":true}")
        Map<String, Object> fieldValues,

        @Schema(description = "Explicit output path for the written YAML file. "
                + "When null, the file is written to a system temp location.",
                nullable = true,
                example = "/home/dev/configs/out/redis.yaml")
        @Nullable String outputPath
) {
}
