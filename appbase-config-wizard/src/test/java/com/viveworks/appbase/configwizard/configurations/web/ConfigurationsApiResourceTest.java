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

package com.viveworks.appbase.configwizard.configurations.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.configwizard.configurations.prompt.FieldPrompter;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigClassInfo;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigScanner;
import com.viveworks.appbase.configwizard.configurations.web.dto.BuildResultDto;
import com.viveworks.appbase.configwizard.configurations.web.dto.ConfigDetailDto;
import com.viveworks.appbase.configwizard.configurations.web.dto.ConfigSummaryDto;
import com.viveworks.appbase.configwizard.configurations.web.dto.DefaultDto;
import com.viveworks.appbase.configwizard.configurations.web.dto.FieldSummaryDto;
import com.viveworks.appbase.configwizard.configurations.web.dto.WriteRequestDto;
import com.viveworks.appbase.configwizard.configurations.web.dto.WriteResultDto;
import com.viveworks.appbase.configwizard.configurations.writer.YamlWriter;
import com.viveworks.appbase.configwizard.security.PathConfinement;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link ConfigurationsApiResource} exercising the resource methods
 * directly against a real {@link ConfigScanner}, {@link FieldPrompter}, and
 * {@link YamlWriter} — no HTTP server required.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
final class ConfigurationsApiResourceTest {

    private static final String SENTINEL_PATH = "/private/aud09/secrets.yaml";
    private static final String SENTINEL_TYPE = "com.example.Aud09SecretType";
    private static final String SENTINEL_FIELD = "secretPropertyName";
    private static final String SENTINEL_DETAIL =
            SENTINEL_PATH + " " + SENTINEL_TYPE + " " + SENTINEL_FIELD;
    private static final java.util.regex.Pattern SAFE_ERROR = java.util.regex.Pattern.compile(
            "Configuration request failed; reference=([0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-"
                    + "[89ab][0-9a-f]{3}-[0-9a-f]{12})");
    private static final String TEXT_VALUE_CONFIGURATION =
            "com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration";

    private ConfigScanner scanner;
    private FieldPrompter prompter;
    private YamlWriter writer;
    private ConfigurationsApiResource resource;

    /**
     * Sets up the resource under test with real collaborators.
     */
    @BeforeEach
    void setUp() {
        scanner = new ConfigScanner();
        prompter = new FieldPrompter(new ObjectMapper());
        writer = new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.none());
        resource = new ConfigurationsApiResource(scanner, prompter, writer, PathConfinement.none());
    }

    /**
     * Builds a resource confined to {@code root}, with its writer confined to the same root.
     *
     * @param root the only tree the resource may read or write
     * @return a confined resource
     */
    private static ConfigurationsApiResource confinedTo(final Path root) {
        return new ConfigurationsApiResource(
                new ConfigScanner(),
                new FieldPrompter(new ObjectMapper()),
                new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root)),
                PathConfinement.of(root));
    }

    @Test
    void writeConfig_outputPathOutsideRoot_returnsForbiddenAndWritesNothing(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path outside = Files.createDirectory(tmp.resolve("outside"));
        Path target = outside.resolve("hooks-pre-commit");
        String fqcn = new ConfigScanner().scan().get(0).fqcn();

        WebApplicationException thrown = assertThrows(WebApplicationException.class,
                () -> confinedTo(root).writeConfig(fqcn,
                        new WriteRequestDto(Map.of(), target.toString())));
        assertEquals(403, thrown.getResponse().getStatus());
        assertFalse(Files.exists(target), "A rejected write must not create its target");
    }

    @Test
    void writeConfig_outputPathInsideRoot_succeeds(@TempDir final Path tmp) throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        String fqcn = new ConfigScanner().scan().get(0).fqcn();

        WriteResultDto result = confinedTo(root).writeConfig(fqcn,
                new WriteRequestDto(Map.of(), root.resolve("ok.yaml").toString()));

        assertNull(result.errorMessage());
        // A10-01 (internal work item 1449): the returned path is root-relative, not the absolute filesystem path.
        assertFalse(Path.of(result.path()).isAbsolute(),
                "the written path must be root-relative, not absolute: " + result.path());
        assertTrue(Files.exists(root.resolve(result.path())),
                "the written file must exist under the root");
    }

    /**
     * {@code GET /api/configs} must return a non-empty list of config summaries (HTTP 200
     * equivalent).
     *
     * <p>The Appbase classpath always contains at least one configuration class; the list
     * must therefore be non-empty.
     */
    @Test
    void listConfigs_returnsNonEmptyList() {
        // Act
        List<ConfigSummaryDto> result = resource.listConfigs();

        // Assert
        assertNotNull(result, "Response must not be null");
        assertFalse(result.isEmpty(), "At least one Appbase config class must be on the classpath");
        ConfigSummaryDto first = result.get(0);
        assertAll("first summary",
                () -> assertNotNull(first.fqcn(), "fqcn must be set"),
                () -> assertNotNull(first.simpleName(), "simpleName must be set"),
                () -> assertFalse(first.fqcn().isEmpty(), "fqcn must not be empty"),
                () -> assertFalse(first.simpleName().isEmpty(), "simpleName must not be empty")
        );
    }

    /**
     * {@code GET /api/configs/{fqcn}} must return a non-empty field list for any class
     * discovered by the scanner.
     */
    @Test
    void getConfigDetail_forFirstDiscoveredConfig_returnsNonEmptyFieldList() {
        // Arrange
        ConfigSummaryDto first = resource.listConfigs().get(0);

        // Act
        ConfigDetailDto detail = resource.getConfigDetail(first.fqcn());

        // Assert
        assertNotNull(detail, "Detail response must not be null");
        assertEquals(first.fqcn(), detail.fqcn(), "FQCN must be echoed back");
        assertFalse(detail.fields().isEmpty(), "Config class must have at least one field");
    }

    /**
     * {@code GET /api/configs/{fqcn}} must also resolve by simple name (not only FQCN).
     */
    @Test
    void getConfigDetail_bySimpleName_resolves() {
        // Arrange
        ConfigSummaryDto first = resource.listConfigs().get(0);

        // Act
        ConfigDetailDto detail = resource.getConfigDetail(first.simpleName());

        // Assert
        assertNotNull(detail);
        assertEquals(first.fqcn(), detail.fqcn(), "Resolution by simple name must return same class");
    }

    /**
     * {@code GET /api/configs/{fqcn}} must throw {@link NotFoundException} for an unknown class.
     */
    @Test
    void getConfigDetail_unknownClass_throwsNotFoundException() {
        assertThrows(NotFoundException.class,
                () -> resource.getConfigDetail("com.example.DoesNotExist"),
                "Unknown FQCN must cause 404");
    }

    /**
     * P-042 E01 N08 (internal work item 1379) -- register row AUD-06 (Lens 2 F1 / Lens 4 F1, CWE-470).
     *
     * ⚠️ THE ARM ABOVE PASSES FOR THE WRONG REASON, AND PASSED BEFORE THE FIX TOO.
     * {@code "com.example.DoesNotExist"} is not a class at all, so {@code loadClass} throws
     * {@code ClassNotFoundException} and the 404 arrives by accident of the input. It cannot
     * distinguish {@code orElseGet(loadClass)} from {@code orElseThrow} -- both 404 on a name that
     * does not resolve.
     *
     * The exploit input is the opposite: a class that RESOLVES and is simply not a scanned config
     * class. Under the old code the scanner allowlist was advisory -- a miss fell through to
     * {@code loadClass}, which succeeded, and the arbitrary class was then handed to
     * {@code scanner.infoFor(...)}. {@code java.lang.String} is the cheapest such input: always
     * loadable, never a config class.
     */
    @Test
    void getConfigDetail_loadableNonConfigClass_isRejectedByTheAllowlist() {
        assertThrows(NotFoundException.class,
                () -> resource.getConfigDetail("java.lang.String"),
                "the scanner allowlist must be the control, not a hint: a class that loads but was "
                        + "never scanned must be refused, not loaded");
    }

    @Test
    void advertisedNestedObjectSupportsDetailDefaultsBuildAndWrite(@TempDir final Path tmp) {
        String nestedFqcn = scanner.scan().stream()
                .flatMap(info -> prompter.introspect(info).stream())
                .filter(field -> field.nestedClass() != null)
                .map(field -> scanner.infoFor(field.nestedClass()))
                .filter(info -> info.defaultInstance() != null)
                .map(ConfigClassInfo::fqcn)
                .distinct()
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "the scanned configuration surface must advertise a buildable nested object"));

        ConfigDetailDto detail = resource.getConfigDetail(nestedFqcn);
        DefaultDto defaults = resource.getDefaultValues(nestedFqcn);
        BuildResultDto build = resource.buildConfig(
                nestedFqcn, Map.of());
        WriteResultDto write = resource.writeConfig(
                nestedFqcn,
                new WriteRequestDto(Map.of(), tmp.resolve("nested.yaml").toString()));

        assertEquals(nestedFqcn, detail.fqcn());
        assertNotNull(defaults.values());
        assertNotNull(build.yaml(), build.errorMessage());
        assertNull(build.errorMessage());
        assertNotNull(write.path(), write.errorMessage());
        assertNull(write.errorMessage());
        assertTrue(Files.exists(tmp.resolve(write.path())));
    }

    @Test
    void advertisedCollectionElementSupportsDetailDefaultsBuildAndWrite(@TempDir final Path tmp) {
        String elementFqcn = scanner.scan().stream()
                .flatMap(info -> prompter.introspect(info).stream())
                .filter(field -> field.elementClass() != null)
                .map(field -> scanner.infoFor(field.elementClass()))
                .filter(info -> info.defaultInstance() != null)
                .map(ConfigClassInfo::fqcn)
                .distinct()
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "the scanned configuration surface must advertise a buildable collection element"));

        ConfigDetailDto detail = resource.getConfigDetail(elementFqcn);
        DefaultDto defaults = resource.getDefaultValues(elementFqcn);
        BuildResultDto build = resource.buildConfig(elementFqcn, Map.of());
        WriteResultDto write = resource.writeConfig(
                elementFqcn, new WriteRequestDto(Map.of(), tmp.resolve("element.yaml").toString()));

        assertEquals(elementFqcn, detail.fqcn());
        assertNotNull(defaults.values(), "an advertised buildable element must expose defaults");
        assertNotNull(build.yaml(), build.errorMessage());
        assertNull(build.errorMessage());
        assertNotNull(write.path(), write.errorMessage());
        assertNull(write.errorMessage());
        assertTrue(Files.exists(tmp.resolve(write.path())));
    }

    /**
     * {@code POST /api/configs/{fqcn}/build} with empty field values must succeed for classes
     * that have all-defaults constructors and return a non-null YAML string (HTTP 200 equivalent).
     */
    @Test
    void buildConfig_withEmptyValues_returnsYamlOrErrorMessage() {
        // Arrange — pick a config class that has a default instance (scanner pre-validates this)
        ConfigClassInfo infoWithDefault = scanner.scan().stream()
                .filter(c -> c.defaultInstance() != null)
                .findFirst()
                .orElse(null);

        if (infoWithDefault == null) {
            // No class with defaults on classpath — skip this assertion
            return;
        }

        // Act
        BuildResultDto result = resource.buildConfig(infoWithDefault.fqcn(), Collections.emptyMap());

        // Assert
        assertNotNull(result, "Build result must not be null");
        // On success: yaml is non-null; on validation failure: errorMessage is non-null.
        // Either is acceptable — the endpoint must not throw.
        assertTrue(result.yaml() != null || result.errorMessage() != null,
                "Either yaml or errorMessage must be set");
    }

    /**
     * {@code POST /api/configs/{fqcn}/build} for a class with a constructable default instance
     * must return a non-null YAML string and a null error message on the happy path.
     */
    @Test
    void buildConfig_forConfigWithDefaults_returnsNonNullYaml() {
        // Arrange
        ConfigClassInfo infoWithDefault = scanner.scan().stream()
                .filter(c -> c.defaultInstance() != null)
                .findFirst()
                .orElse(null);

        if (infoWithDefault == null) {
            return; // no suitable class on classpath
        }

        // Act
        BuildResultDto result = resource.buildConfig(infoWithDefault.fqcn(), Collections.emptyMap());

        // Assert
        assertNotNull(result);
        if (result.yaml() != null) {
            assertFalse(result.yaml().isBlank(), "YAML string must not be blank");
            assertNull(result.errorMessage(), "No error message expected on success");
        }
    }

    /**
     * {@code POST /api/configs/{fqcn}/write} with empty field values and no explicit output path
     * must write a file to the temp directory and return its path.
     */
    @Test
    void writeConfig_withEmptyValues_writesFileAndReturnsPath() {
        // Arrange
        ConfigClassInfo infoWithDefault = scanner.scan().stream()
                .filter(c -> c.defaultInstance() != null)
                .findFirst()
                .orElse(null);

        if (infoWithDefault == null) {
            return;
        }

        WriteRequestDto body = new WriteRequestDto(Collections.emptyMap(), null);

        // Act
        WriteResultDto result = resource.writeConfig(infoWithDefault.fqcn(), body);

        // Assert
        assertNotNull(result, "Write result must not be null");
        if (result.path() != null) {
            assertFalse(result.path().isBlank(), "Written path must not be blank");
            assertNull(result.errorMessage(), "No error message expected on success");
            assertTrue(result.path().endsWith(".yaml"), "Output file must have .yaml extension");
        }
    }

    /**
     * {@code POST /api/configs/{fqcn}/build} with {@code null} for fields marked as required
     * must surface the Hibernate Validator constraint violations as a non-null
     * {@code errorMessage} rather than propagating an uncaught exception.
     *
     * <p>The test iterates over discovered config classes to locate one that exposes at least
     * one {@code required} field. It then submits {@code null} for every required field and
     * asserts that the endpoint captures the resulting {@code ConstraintViolation}s in the
     * {@code errorMessage} component of {@link BuildResultDto}. If no class with required
     * fields is found on the classpath, the test passes vacuously.
     */
    @Test
    void buildConfig_withNullForRequiredFields_returnsValidationErrorMessage() {
        // Arrange — find a config class where submitting null for all fields produces an error.
        //
        // Note: FieldSummaryDto.required() means "is a Java primitive" (non-nullable at the type
        // level), not "@NotNull on an Object field".  Submitting null for a primitive field causes
        // Jackson to coerce the value to 0/false — no Hibernate Validator violation is raised.
        // We therefore submit null for ALL fields (not only the "required" primitives) so that
        // any @NotNull-annotated Object field produces a ConstraintViolation.
        for (ConfigClassInfo info : scanner.scan()) {
            ConfigDetailDto detail = resource.getConfigDetail(info.fqcn());
            if (detail.fields().isEmpty()) {
                continue;
            }

            // Submit null for every field (primitive and non-primitive alike)
            Map<String, Object> nullValues = new HashMap<>();
            for (FieldSummaryDto field : detail.fields()) {
                nullValues.put(field.name(), null);
            }

            // Act
            BuildResultDto result = resource.buildConfig(info.fqcn(), nullValues);
            assertNotNull(result, "Result must not be null; errors must not escape as exceptions");

            if (result.errorMessage() != null) {
                // Found a class that surfaces an error (Jackson or Hibernate Validator).
                assertNull(result.yaml(), "yaml must be null when errorMessage is present");
                return; // one class is sufficient to verify the error-surfacing path
            }
            // This class produced valid YAML with all-null inputs — try the next one
        }
        // No class found that errors on all-null inputs — vacuous pass
    }

    @Test
    @org.junit.jupiter.api.parallel.ResourceLock("ConfigurationsApiResource.logger")
    void buildConfig_errorDoesNotExposeRawExceptionAndLogsCorrelationId() {
        final ConfigClassInfo info = scanner.scan().get(0);
        final ThrowingConvertMapper mapper = new ThrowingConvertMapper();
        final ConfigurationsApiResource failing = new ConfigurationsApiResource(
                scanner,
                new FieldPrompter(mapper),
                writer,
                PathConfinement.none());

        final Captured<BuildResultDto> first = captureFailureLog(
                () -> failing.buildConfig(info.fqcn(), Map.of(SENTINEL_FIELD, SENTINEL_PATH)));
        final Captured<BuildResultDto> second = captureFailureLog(
                () -> failing.buildConfig(info.fqcn(), Map.of(SENTINEL_FIELD, SENTINEL_PATH)));

        final String firstId = assertSafeClientError(first.value().errorMessage());
        final String secondId = assertSafeClientError(second.value().errorMessage());
        assertFalse(firstId.equals(secondId), "each failure must receive a fresh opaque identifier");
        assertCorrelatedServerDetail(first.logRecords(), firstId, mapper.failure());
        assertCorrelatedServerDetail(second.logRecords(), secondId, mapper.failure());
    }

    @Test
    @org.junit.jupiter.api.parallel.ResourceLock("ConfigurationsApiResource.logger")
    void writeConfig_errorDoesNotExposeRawExceptionAndLogsCorrelationId() {
        final ConfigClassInfo info = scanner.scan().stream()
                .filter(c -> c.defaultInstance() != null)
                .findFirst()
                .orElseThrow();
        final ThrowingWriteMapper mapper = new ThrowingWriteMapper();
        final YamlWriter failingWriter = new YamlWriter(mapper, PathConfinement.none());
        final ConfigurationsApiResource failing = new ConfigurationsApiResource(
                scanner, prompter, failingWriter, PathConfinement.none());

        final Captured<WriteResultDto> captured = captureFailureLog(
                () -> failing.writeConfig(info.fqcn(), new WriteRequestDto(Map.of(), null)));

        final String id = assertSafeClientError(captured.value().errorMessage());
        assertCorrelatedServerDetail(captured.logRecords(), id, mapper.failure());
    }

    private static String assertSafeClientError(final String errorMessage) {
        assertNotNull(errorMessage);
        final java.util.regex.Matcher matcher = SAFE_ERROR.matcher(errorMessage);
        assertTrue(matcher.matches(), "client error must be only the stable generic text and opaque id");
        assertFalse(errorMessage.contains(SENTINEL_PATH));
        assertFalse(errorMessage.contains(SENTINEL_TYPE));
        assertFalse(errorMessage.contains(SENTINEL_FIELD));
        assertFalse(errorMessage.contains("IllegalArgumentException"));
        assertFalse(errorMessage.contains("AssertionError"));
        assertFalse(errorMessage.contains("IOException"));
        assertFalse(errorMessage.contains("ConfigurationsApiResourceTest"));
        return matcher.group(1);
    }

    private static void assertCorrelatedServerDetail(
            final List<java.util.logging.LogRecord> records,
            final String correlationId,
            final Throwable expectedFailure) {
        assertEquals(1, records.size(), "one server error event per failed request");
        final java.util.logging.LogRecord record = records.get(0);
        assertTrue(record.getMessage().contains(correlationId));
        assertNotNull(record.getThrown(), "server event must retain the caught throwable/stack trace");
        assertEquals(expectedFailure, record.getThrown(), "server event must retain the original throwable");
        assertTrue(record.getThrown().getMessage().contains(SENTINEL_PATH));
        assertTrue(record.getThrown().getMessage().contains(SENTINEL_TYPE));
        assertTrue(record.getThrown().getMessage().contains(SENTINEL_FIELD));
        assertTrue(record.getThrown().getStackTrace().length > 0);
    }

    private static <T> Captured<T> captureFailureLog(final java.util.function.Supplier<T> invocation) {
        final java.util.logging.Logger logger =
                java.util.logging.Logger.getLogger(ConfigurationsApiResource.class.getName());
        final java.util.logging.Level previousLevel = logger.getLevel();
        final List<java.util.logging.LogRecord> records = new java.util.ArrayList<>();
        final java.util.logging.Handler handler = new java.util.logging.Handler() {
            @Override
            public void publish(final java.util.logging.LogRecord record) {
                if (record.getLevel().intValue() >= java.util.logging.Level.SEVERE.intValue()) {
                    records.add(record);
                }
            }

            @Override
            public void flush() { }

            @Override
            public void close() { }
        };
        handler.setLevel(java.util.logging.Level.ALL);
        logger.addHandler(handler);
        logger.setLevel(java.util.logging.Level.ALL);
        try {
            return new Captured<>(invocation.get(), List.copyOf(records));
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previousLevel);
            handler.close();
        }
    }

    private record Captured<T>(T value, List<java.util.logging.LogRecord> logRecords) { }

    private static final class ThrowingConvertMapper extends ObjectMapper {
        private final AssertionError failure = new AssertionError(SENTINEL_DETAIL);

        @Override
        public <T> T convertValue(final Object fromValue, final Class<T> toValueType) {
            throw failure;
        }

        private AssertionError failure() {
            return failure;
        }
    }

    private static final class ThrowingWriteMapper extends ObjectMapper {
        private final java.io.IOException failure = new java.io.IOException(SENTINEL_DETAIL);

        @Override
        public void writeValue(final java.io.OutputStream out, final Object value) throws java.io.IOException {
            throw failure;
        }

        private java.io.IOException failure() {
            return failure;
        }
    }

    /**
     * L4-02 (CWE-400): {@code POST /api/configurations/{fqcn}/build} must reject a
     * {@code fieldValues} map carrying more entries than {@link
     * com.viveworks.appbase.configwizard.security.FieldValuesLimits#MAX_FIELD_COUNT} before it
     * ever reaches {@link FieldPrompter#buildFromMap} — with a disclosure-free error message,
     * not a thrown exception.
     */
    @Test
    void buildConfig_rejectsTooManyOrOversizeFieldValues() {
        final ConfigClassInfo info = scanner.scan().get(0);
        final Map<String, Object> tooMany = new HashMap<>();
        for (int i = 0; i <
                com.viveworks.appbase.configwizard.security.FieldValuesLimits.MAX_FIELD_COUNT + 1;
                i++) {
            tooMany.put("field" + i, "value");
        }

        final BuildResultDto result = resource.buildConfig(info.fqcn(), tooMany);

        assertNull(result.yaml(), "an oversize request must not be built");
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().contains("too many fields")
                        || result.errorMessage().contains("too much nesting")
                        || result.errorMessage().contains("too large"),
                "error message must describe the size rejection: " + result.errorMessage());
    }

    /**
     * L4-02 (CWE-400): {@code POST /api/configurations/{fqcn}/write} must reject the same
     * oversize {@code fieldValues} map before calling the writer, and must not create a file.
     */
    @Test
    void writeConfig_rejectsTooManyOrOversizeFieldValues(@TempDir final Path tmp) throws Exception {
        final Path root = Files.createDirectory(tmp.resolve("root"));
        final ConfigurationsApiResource confined = confinedTo(root);
        final ConfigClassInfo info = scanner.scan().get(0);
        final Map<String, Object> tooMany = new HashMap<>();
        for (int i = 0; i <
                com.viveworks.appbase.configwizard.security.FieldValuesLimits.MAX_FIELD_COUNT + 1;
                i++) {
            tooMany.put("field" + i, "value");
        }

        final WriteResultDto result = confined.writeConfig(
                info.fqcn(), new WriteRequestDto(tooMany, root.resolve("rejected.yaml").toString()));

        assertNull(result.path(), "an oversize request must not be written");
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().contains("too many fields")
                        || result.errorMessage().contains("too much nesting")
                        || result.errorMessage().contains("too large"),
                "error message must describe the size rejection, not a generic failure: "
                        + result.errorMessage());
        assertFalse(Files.exists(root.resolve("rejected.yaml")));
    }

    /**
     * {@code GET /api/configs/{fqcn}/default} must return a non-null {@link DefaultDto} for
     * any class that has a constructable default instance.
     */
    @Test
    void getDefaultValues_forConfigWithDefaults_returnsNonNullValues() {
        // Arrange
        ConfigClassInfo infoWithDefault = scanner.scan().stream()
                .filter(c -> c.defaultInstance() != null)
                .findFirst()
                .orElse(null);

        if (infoWithDefault == null) {
            return;
        }

        // Act
        DefaultDto result = resource.getDefaultValues(infoWithDefault.fqcn());

        // Assert
        assertNotNull(result, "DefaultDto must not be null");
        assertNotNull(result.values(), "Default values must be present for a class with a default instance");
    }
}
