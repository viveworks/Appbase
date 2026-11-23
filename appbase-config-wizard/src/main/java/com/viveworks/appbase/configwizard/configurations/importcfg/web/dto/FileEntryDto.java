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
 * Metadata for a single YAML file discovered during an import directory scan.
 *
 * @param path       display path of the YAML file — root-relative when the scan is confined to a
 *                   permitted root and the entry resolves cleanly; file name only for the unconfined
 *                   console or when an individual entry cannot be canonicalised (an unreadable or
 *                   cyclic symlink); never the absolute server layout (A10-01, internal work item 1513)
 * @param fileName   file name only (e.g. {@code redis.yaml})
 * @param candidates inferred type candidates sorted by confidence descending (up to 5)
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Schema(description = "Metadata for a single YAML file found during an import directory scan.")
public record FileEntryDto(
        @Schema(description = "Display path of the YAML file: root-relative when the scan is confined "
                + "to a permitted root and the entry resolves cleanly; file name only for the unconfined "
                + "console or when an entry cannot be canonicalised (an unreadable or cyclic symlink). "
                + "Never discloses the absolute server layout.",
                example = "env/redis.yaml")
        String path,

        @Schema(description = "File name only (no directory component).", example = "redis.yaml")
        String fileName,

        @Schema(description = "Inferred Appbase configuration type candidates, sorted by confidence descending. "
                + "At most 5 candidates are returned per file.")
        List<TypeCandidateDto> candidates
) {
}
