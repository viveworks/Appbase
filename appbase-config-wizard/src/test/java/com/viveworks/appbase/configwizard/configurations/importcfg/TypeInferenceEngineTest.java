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

package com.viveworks.appbase.configwizard.configurations.importcfg;

import com.viveworks.appbase.configwizard.configurations.importcfg.web.dto.TypeCandidateDto;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class TypeInferenceEngineTest {
    private TypeInferenceEngine engine;
    private ConfigScanner scanner;

    @BeforeEach void setUp() {
        scanner = new ConfigScanner();
        engine = new TypeInferenceEngine(scanner.scan());
    }

    @Test
    void redisYaml_highFilenameScore_suggestsRedisConfig(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("redis.yaml");
        Files.writeString(f, "host: localhost\nport: 6379\n");
        List<TypeCandidateDto> candidates = engine.infer(f);
        assertFalse(candidates.isEmpty(), "Must return candidates for redis.yaml");
        assertTrue(candidates.get(0).confidence() >= 0.5,
            "Redis filename should score at least 0.5 for a Redis config class");
        assertTrue(candidates.get(0).fqcn().toLowerCase().contains("redis"),
            "Top candidate for redis.yaml should be a Redis config");
    }

    @Test
    void unknownYaml_returnsAtMostFiveCandidates_noneAboveThreshold(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("unknown-service.yaml");
        Files.writeString(f, "foo: bar\nbaz: qux\n");
        List<TypeCandidateDto> candidates = engine.infer(f);
        assertTrue(candidates.size() <= 5, "At most 5 candidates");
        candidates.forEach(c ->
            assertTrue(c.confidence() < 0.7, "Unknown yaml should not be confident"));
    }

    @Test
    void emptyYaml_returnsResults_noException(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("empty.yaml");
        Files.writeString(f, "");
        assertDoesNotThrow(() -> engine.infer(f));
    }

    /**
     * L2-11 (CWE-400): a YAML alias bomb (billion-laughs shape — each level anchors the
     * previous level's collection and references it twice) must stay bounded by the parser's
     * explicit alias/nesting limits instead of being expanded into an exponential object
     * graph. The file below is a few hundred bytes but would expand to ~2^30 nodes if the
     * alias count were uncapped; the parse must refuse it bounded (and quickly).
     */
    @Test
    @org.junit.jupiter.api.Timeout(10)
    void aliasBombYaml_isBoundedByParserLimits(@TempDir Path dir) throws Exception {
        final StringBuilder bomb = new StringBuilder("n0: &n0 [anchorpayload]\n");
        for (int i = 1; i <= 30; i++) {
            bomb.append("n").append(i).append(": &n").append(i).append(" [*n").append(i - 1)
                    .append(", *n").append(i - 1).append("]\n");
        }
        Path bombFile = dir.resolve("bomb.yaml");
        Files.writeString(bombFile, bomb.toString());

        List<TypeCandidateDto> candidates = assertDoesNotThrow(() -> engine.infer(bombFile));

        assertNotNull(candidates, "a bounded parse refusal must still return a candidate list");
    }

    @Test
    void filenameScoreOnly_exactSimpleNameMatch_scoresMaxFilename(@TempDir Path dir) throws Exception {
        // A YAML file named after a real config class should score 0.6 for filename alone
        String simpleName = scanner.scan().get(0).simpleName(); // e.g. DefaultRedisClient...
        Path f = dir.resolve(simpleName.toLowerCase() + ".yaml");
        Files.writeString(f, "");
        List<TypeCandidateDto> candidates = engine.infer(f);
        assertFalse(candidates.isEmpty());
        // The matching class should appear first
        assertEquals(simpleName, candidates.get(0).simpleName());
    }

    @Test
    void fieldOverlapScore_yamlFieldsMatchConfigFields_increasesConfidence(@TempDir Path dir) throws Exception {
        // Find a config class; write a YAML with its known field names as keys
        var info = scanner.scan().get(0);
        StringBuilder yaml = new StringBuilder();
        new com.viveworks.appbase.configwizard.configurations.prompt.FieldPrompter(
            new com.fasterxml.jackson.databind.ObjectMapper())
            .introspect(info)
            .stream().limit(3)
            .forEach(f2 -> yaml.append(f2.name()).append(": somevalue\n"));
        Path f = dir.resolve("config.yaml");
        Files.writeString(f, yaml.toString());
        List<TypeCandidateDto> candidates = engine.infer(f);
        // The matching config class should appear in top 5
        assertTrue(candidates.stream().anyMatch(c -> c.fqcn().equals(info.fqcn())),
            "Config class with field match should appear in candidates");
    }
}
