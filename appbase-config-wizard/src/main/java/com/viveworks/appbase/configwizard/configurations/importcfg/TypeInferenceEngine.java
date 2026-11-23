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

package com.viveworks.appbase.configwizard.configurations.importcfg;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.configwizard.configurations.importcfg.web.dto.TypeCandidateDto;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigClassInfo;
import com.viveworks.appbase.configwizard.security.ImportScanLimits;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Infers the Appbase configuration type for a YAML file using filename tokenization
 * and field overlap scoring.
 *
 * <h2>Scoring algorithm</h2>
 * <p>Each candidate's confidence is the sum of two sub-scores, capped at {@code 1.0}:
 * <ul>
 *   <li><b>Filename score (0–0.5)</b> — tokenizes the file stem by camelCase / delimiter
 *       boundaries and matches against each config class's simple-name tokens. An exact
 *       (case-insensitive, non-alphanumeric chars removed) match of stem to simpleName
 *       immediately returns the maximum filename score of {@code 0.5}.</li>
 *   <li><b>Field overlap score (0–0.5)</b> — parses the YAML file's top-level keys and
 *       computes the fraction of those keys that appear in the config class's Jackson
 *       deserializable property names.</li>
 * </ul>
 *
 * <p>Results are sorted by confidence descending, limited to {@value #MAX_CANDIDATES}
 * and filtered to only classes with a confidence {@code > 0}.
 * A confidence of {@code >= 0.7} is considered "Suggested".
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class TypeInferenceEngine {

    /** Maximum number of candidates to return per file. */
    private static final int MAX_CANDIDATES = 5;

    /** Threshold above which a candidate is considered "Suggested". */
    @SuppressWarnings("unused")
    private static final double CONFIDENT_THRESHOLD = 0.7;

    /**
     * Maximum filename sub-score (0.0–0.6).
     *
     * <p>Weighted higher than the field overlap score so that an exact filename token match
     * always outranks a class that matches only via generic field names (e.g. "host", "port").
     */
    private static final double MAX_FILENAME_SCORE = 0.6;

    /**
     * Maximum field overlap sub-score (0.0–0.4).
     *
     * <p>Weighted lower than the filename score so that pure field-name coincidences (common
     * keys like "host" or "port") cannot outrank a class whose name matches the YAML filename.
     */
    private static final double MAX_FIELD_SCORE = 0.4;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<ConfigClassInfo> allConfigs;

    /**
     * Field names keyed by config FQCN; pre-computed at construction time for efficiency.
     */
    private final Map<String, Set<String>> configFieldNames;

    /**
     * Constructs a {@code TypeInferenceEngine} from the given list of known config classes.
     *
     * <p>Jackson introspection is performed eagerly at construction time so that
     * {@link #infer(Path)} can be called repeatedly without repeated introspection overhead.
     *
     * @param allConfigs all known Appbase configuration classes (typically from {@code ConfigScanner.scan()})
     */
    public TypeInferenceEngine(final List<ConfigClassInfo> allConfigs) {
        this.allConfigs = List.copyOf(allConfigs);
        this.configFieldNames = new HashMap<>();
        for (ConfigClassInfo config : allConfigs) {
            configFieldNames.put(config.fqcn(), introspectFieldNames(config));
        }
    }

    /**
     * Infers the top-{@value #MAX_CANDIDATES} Appbase configuration type candidates for the
     * given YAML file, sorted by confidence descending.
     *
     * <p>Only candidates with {@code confidence > 0} are included. An empty list is returned
     * when no config class matches either by filename tokens or field overlap.
     *
     * @param yamlFile the YAML file to analyse; must be readable
     * @return up to {@value #MAX_CANDIDATES} type candidates sorted by confidence descending
     * @throws IOException when the file cannot be read
     */
    public List<TypeCandidateDto> infer(final Path yamlFile) throws IOException {
        String filename = yamlFile.getFileName().toString();
        String stem = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf('.'))
                : filename;

        Map<String, Object> yamlTopKeys = parseTopLevelKeys(yamlFile);

        return allConfigs.stream()
                .map(config -> computeCandidate(stem, yamlTopKeys, config))
                .filter(c -> c.confidence() > 0)
                .sorted(Comparator.comparingDouble(TypeCandidateDto::confidence).reversed())
                .limit(MAX_CANDIDATES)
                .toList();
    }

    /**
     * Computes a single type candidate for a config class given the YAML file's stem and
     * top-level keys.
     *
     * @param stem       the file name without extension (e.g. {@code "redis"})
     * @param yamlTopKeys top-level key-value map parsed from the YAML file; may be empty
     * @param config     the config class to score
     * @return a populated {@link TypeCandidateDto} (confidence may be {@code 0.0})
     */
    private TypeCandidateDto computeCandidate(final String stem,
                                              final Map<String, Object> yamlTopKeys,
                                              final ConfigClassInfo config) {
        List<String> signals = new ArrayList<>();
        double fnScore = filenameScore(stem, config);
        if (fnScore > 0) {
            signals.add("filename-match");
        }
        double foScore = fieldOverlapScore(yamlTopKeys, config);
        if (foScore > 0) {
            signals.add("field-overlap");
        }
        double confidence = Math.min(1.0, fnScore + foScore);
        return new TypeCandidateDto(config.fqcn(), config.simpleName(), confidence,
                Collections.unmodifiableList(signals));
    }

    /**
     * Computes the filename sub-score (0.0–0.5) for a config class.
     *
     * <p>Two strategies are applied in order, and the first non-zero result is returned:
     * <ol>
     *   <li>Exact match: if the stem (lowercased, non-alphanumeric chars stripped) equals the
     *       config's simple name lowercased → returns {@value #MAX_FILENAME_SCORE}.</li>
     *   <li>Token overlap: tokenizes both the stem and the config simple name; returns
     *       {@code (stem tokens found in class tokens) / (stem tokens count) * 0.5}.</li>
     * </ol>
     *
     * @param stem   the YAML file stem (no extension)
     * @param config the config class to score against
     * @return filename sub-score in {@code [0.0, 0.5]}
     */
    private double filenameScore(final String stem, final ConfigClassInfo config) {
        // Exact match check (normalized — remove non-alphanumeric, lowercase)
        String stemNorm = stem.toLowerCase().replaceAll("[^a-z0-9]", "");
        String classNorm = config.simpleName().toLowerCase();
        if (stemNorm.equals(classNorm)) {
            return MAX_FILENAME_SCORE;
        }

        List<String> stemTokens = tokenize(stem);
        if (stemTokens.isEmpty()) {
            return 0.0;
        }

        Set<String> classTokenSet = new HashSet<>(tokenize(config.simpleName()));
        long matches = stemTokens.stream().filter(classTokenSet::contains).count();
        if (matches == 0) {
            return 0.0;
        }
        return Math.min(MAX_FILENAME_SCORE, (double) matches / stemTokens.size() * MAX_FILENAME_SCORE);
    }

    /**
     * Computes the field overlap sub-score (0.0–0.5) for a config class.
     *
     * <p>Calculates the fraction of YAML top-level keys that appear in the config class's
     * Jackson deserializable property names, scaled to {@code [0.0, 0.5]}.
     *
     * @param yamlTopKeys top-level key-value pairs parsed from the YAML file
     * @param config      the config class to score against
     * @return field overlap sub-score in {@code [0.0, 0.5]}
     */
    private double fieldOverlapScore(final Map<String, Object> yamlTopKeys,
                                     final ConfigClassInfo config) {
        if (yamlTopKeys.isEmpty()) {
            return 0.0;
        }
        Set<String> fieldNames = configFieldNames.getOrDefault(config.fqcn(), Collections.emptySet());
        if (fieldNames.isEmpty()) {
            return 0.0;
        }
        long matches = yamlTopKeys.keySet().stream().filter(fieldNames::contains).count();
        if (matches == 0) {
            return 0.0;
        }
        return Math.min(MAX_FIELD_SCORE, (double) matches / yamlTopKeys.size() * MAX_FIELD_SCORE);
    }

    /**
     * Tokenizes a string by delimiter characters ({@code -, _, ., space}) and camelCase
     * boundaries, returning lowercase tokens.
     *
     * <p>For example, {@code "DefaultRedisClient"} → {@code ["default", "redis", "client"]};
     * {@code "unknown-service"} → {@code ["unknown", "service"]}.
     *
     * @param s the string to tokenize; must not be {@code null}
     * @return ordered list of lowercase tokens; empty when {@code s} is blank
     */
    private static List<String> tokenize(final String s) {
        List<String> tokens = new ArrayList<>();
        // Split by delimiter characters first
        String[] delimParts = s.split("[-_.\\s]+");
        for (String part : delimParts) {
            if (part.isEmpty()) {
                continue;
            }
            // Split on camelCase boundaries:
            // insert space before an uppercase letter that follows a lowercase letter
            // also handle consecutive uppercase (e.g. "XMLParser" → "XML Parser")
            String spaced = part.replaceAll("([a-z])([A-Z])", "$1 $2")
                                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
            for (String token : spaced.split("\\s+")) {
                if (!token.isEmpty()) {
                    tokens.add(token.toLowerCase());
                }
            }
        }
        return tokens;
    }

    /**
     * Parses the top-level key-value pairs from a YAML file.
     *
     * <p>Returns an empty map when the file is empty, unparseable, or contains a non-mapping
     * YAML document (e.g. a scalar or sequence at the root).
     *
     * <p>L2-11 (CWE-400): the read and the parse are both bounded. The content is streamed
     * through a reader capped at {@link ImportScanLimits#MAX_YAML_FILE_BYTES}, so an oversized
     * file is refused before it is materialized; the parse applies explicit
     * {@link LoaderOptions} limits — nesting depth, alias count, code points — so a tiny
     * alias-bomb (billion-laughs) file cannot expand into an unbounded object graph.
     *
     * @param yamlFile the YAML file to parse
     * @return top-level YAML key-value map; never {@code null}; may be empty
     * @throws IOException when the file cannot be read from disk or exceeds the per-file bound
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseTopLevelKeys(final Path yamlFile) throws IOException {
        final String content = readBounded(yamlFile);
        if (content.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            final Object parsed = boundedYaml().load(content);
            if (parsed instanceof Map<?, ?>) {
                return (Map<String, Object>) parsed;
            }
        } catch (Exception e) {
            // Unparseable YAML — treat as empty. Limit violations inside the parser (nesting
            // depth, alias count, code points) land here too: the parse aborts bounded
            // instead of expanding, which is the guard this method owes (L2-11).
        }
        return Collections.emptyMap();
    }

    /**
     * Builds a single-use SnakeYAML parser with the import expansion bounds applied
     * explicitly rather than inherited from library defaults, which vary across versions.
     * SnakeYAML instances are not thread-safe; one is created per parse.
     *
     * @return a bounded {@link Yaml} parser
     */
    private static Yaml boundedYaml() {
        final LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit((int) ImportScanLimits.MAX_YAML_FILE_BYTES + 1);
        loaderOptions.setNestingDepthLimit(ImportScanLimits.MAX_YAML_NESTING_DEPTH);
        loaderOptions.setMaxAliasesForCollections(ImportScanLimits.MAX_YAML_ALIASES);
        loaderOptions.setAllowRecursiveKeys(false);
        final DumperOptions dumperOptions = new DumperOptions();
        return new Yaml(new SafeConstructor(loaderOptions),
                new Representer(dumperOptions), dumperOptions, loaderOptions);
    }

    /**
     * Reads the file fully but bounded: never more than
     * {@link ImportScanLimits#MAX_YAML_FILE_BYTES} bytes are buffered, so the string this
     * returns can never be larger than the configured per-file bound regardless of the
     * file's real size on disk.
     *
     * @param yamlFile the file to read
     * @return the file's UTF-8 content, at most {@code MAX_YAML_FILE_BYTES} bytes
     * @throws IOException when the file cannot be read or exceeds the per-file bound
     */
    private static String readBounded(final Path yamlFile) throws IOException {
        final int cap = (int) ImportScanLimits.MAX_YAML_FILE_BYTES;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream in = Files.newInputStream(yamlFile)) {
            final byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) >= 0) {
                buffer.write(chunk, 0, read);
                if (buffer.size() > cap) {
                    throw new IOException(
                            "YAML file exceeds the import per-file size limit (content is not included)");
                }
            }
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    /**
     * Uses Jackson's deserialisation config to introspect the property names of a config class.
     *
     * @param config the config class to introspect
     * @return set of Jackson property names; empty when introspection fails
     */
    private static Set<String> introspectFieldNames(final ConfigClassInfo config) {
        try {
            JavaType javaType = OBJECT_MAPPER.constructType(config.configClass());
            BeanDescription beanDesc = OBJECT_MAPPER.getDeserializationConfig().introspect(javaType);
            Set<String> names = new HashSet<>();
            beanDesc.findProperties().forEach(prop -> names.add(prop.getName()));
            return Collections.unmodifiableSet(names);
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }
}
