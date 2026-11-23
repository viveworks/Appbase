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

package com.viveworks.appbase.configwizard.configurations.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.ObjectMapperConfigurator;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.configwizard.configurations.importcfg.web.ImportApiResource;
import com.viveworks.appbase.configwizard.configurations.prompt.FieldInfo;
import com.viveworks.appbase.configwizard.configurations.prompt.FieldPrompter;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigClassInfo;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigScanner;
import com.viveworks.appbase.configwizard.configurations.scan.SupportedConfigClassRegistry;
import com.viveworks.appbase.configwizard.configurations.web.dto.BuildResultDto;
import com.viveworks.appbase.configwizard.configurations.web.dto.ConfigDetailDto;
import com.viveworks.appbase.configwizard.configurations.web.dto.ConfigSummaryDto;
import com.viveworks.appbase.configwizard.configurations.web.dto.DefaultDto;
import com.viveworks.appbase.configwizard.configurations.web.dto.FieldSummaryDto;
import com.viveworks.appbase.configwizard.configurations.web.dto.WriteRequestDto;
import com.viveworks.appbase.configwizard.configurations.web.dto.WriteResultDto;
import com.viveworks.appbase.configwizard.configurations.writer.YamlWriter;
import com.viveworks.appbase.configwizard.security.FieldValuesLimits;
import com.viveworks.appbase.configwizard.security.FieldValuesTooLargeException;
import com.viveworks.appbase.configwizard.security.PathConfinement;
import com.viveworks.appbase.configwizard.security.PathOutsideRootException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import javax.annotation.Nullable;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * JAX-RS resource that exposes the config wizard REST API at {@code /api/configurations/**}.
 *
 * <p>All endpoints are unauthenticated — this server is intended to run on localhost only
 * as a developer tool. Responses are JSON-encoded.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /api/configurations} — lists all discovered configuration classes</li>
 *   <li>{@code GET /api/configurations/{fqcn}} — returns field details for one class</li>
 *   <li>{@code POST /api/configurations/{fqcn}/build} — builds and validates; returns YAML string</li>
 *   <li>{@code POST /api/configurations/{fqcn}/write} — builds and writes YAML to disk</li>
 *   <li>{@code GET /api/configurations/{fqcn}/default} — returns the default instance as JSON</li>
 * </ul>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Path("configurations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class ConfigurationsApiResource {

    private static final Logger LOGGER = Logger.getLogger(ConfigurationsApiResource.class.getName());
    private static final String FAILURE_MESSAGE = "Configuration request failed; reference=";
    private static final Validator VALIDATOR;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    private final ConfigScanner scanner;
    private final FieldPrompter prompter;
    private final SupportedConfigClassRegistry supportedClasses;
    private final YamlWriter writer;
    private final ObjectMapper objectMapper;
    private final ObjectMapper yamlMapper;
    private final ImportApiResource importResource;
    private final PathConfinement confinement;

    /**
     * Convenience constructor — creates an {@link ImportApiResource} internally from the same
     * collaborators. Suitable for production use and existing tests.
     *
     * @param scanner     discovers Appbase configuration classes on the classpath
     * @param prompter    introspects and builds configuration objects
     * @param writer      writes YAML files to disk
     * @param confinement bounds every caller-supplied path; use
     *                    {@link PathConfinement#none()} for the interactive console only
     */
    public ConfigurationsApiResource(final ConfigScanner scanner,
                                   final FieldPrompter prompter,
                                   final YamlWriter writer,
                                   final PathConfinement confinement) {
        this(scanner, prompter, writer,
                new ImportApiResource(scanner, prompter, writer, confinement), confinement);
    }

    /**
     * Full constructor — all collaborators injected explicitly.
     *
     * <p>Prefer this form when an explicit {@link ImportApiResource} instance is required
     * (e.g., for dependency injection containers or specialised testing).
     *
     * @param scanner        discovers Appbase configuration classes on the classpath
     * @param prompter       introspects and builds configuration objects
     * @param writer         writes YAML files to disk
     * @param importResource the import sub-resource to expose at {@code /configurations/import}
     * @param confinement    bounds every caller-supplied path; use
     *                       {@link PathConfinement#none()} for the interactive console only
     */
    public ConfigurationsApiResource(final ConfigScanner scanner,
                                   final FieldPrompter prompter,
                                   final YamlWriter writer,
                                   final ImportApiResource importResource,
                                   final PathConfinement confinement) {
        this.scanner = scanner;
        this.prompter = prompter;
        this.supportedClasses = new SupportedConfigClassRegistry(scanner, prompter);
        this.writer = writer;
        ObjectMapper om = new ObjectMapper();
        ObjectMapperConfigurator.configure(om);
        this.objectMapper = om;
        this.yamlMapper = YamlUtil.getYamlMapper();
        this.importResource = importResource;
        this.confinement = confinement;
    }

    /**
     * Lists all scanned Appbase configuration classes.
     *
     * @return non-null list of {@link ConfigSummaryDto}; may be empty if no configs are found
     */
    @Operation(
            summary = "List all discovered Appbase configuration classes",
            description = "Scans the classpath for classes that implement AppbaseConfiguration and returns "
                    + "a summary of each one, including its fully qualified name, simple name, "
                    + "architectural layer (e.g. 'client', 'core', 'service'), and module name "
                    + "(e.g. 'elasticsearch', 'kafka'). Use this endpoint to discover what can be configured."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful — zero or more configuration classes returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConfigSummaryDto.class))))
    })
    @GET
    public List<ConfigSummaryDto> listConfigs() {
        return scanner.scan().stream()
                .map(this::toSummaryDto)
                .toList();
    }

    /**
     * Returns field details for a single configuration class identified by its FQCN or simple name.
     *
     * @param fqcn the fully qualified class name, or simple name, of the configuration class
     * @return detail DTO with field list and default values
     * @throws NotFoundException when no matching configuration class is found
     */
    @Operation(
            summary = "Get field details for a configuration class by FQCN",
            description = "Returns the full list of configurable fields for the requested class, along with "
                    + "their types, whether they are required, default values, and whether any field "
                    + "is itself a nested configuration object. Also returns the class's default instance "
                    + "as a JSON map so the UI can pre-populate form fields. "
                    + "Pass the FQCN (e.g. com.viveworks.appbase.client.service.kafka.configuration.DefaultKafkaClientConfiguration) "
                    + "or the simple class name if it is unambiguous."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuration class found — field detail returned",
                    content = @Content(schema = @Schema(implementation = ConfigDetailDto.class))),
            @ApiResponse(responseCode = "404", description = "No configuration class found for the given FQCN or simple name")
    })
    @GET
    @Path("{fqcn}")
    public ConfigDetailDto getConfigDetail(
            @Parameter(description = "Fully qualified class name or unambiguous simple name of the configuration class",
                    example = "com.viveworks.appbase.client.service.kafka.configuration.DefaultKafkaClientConfiguration",
                    required = true)
            @PathParam("fqcn") final String fqcn) {
        ConfigClassInfo info = findByFqcn(fqcn);
        List<FieldInfo> fields = prompter.introspect(info);
        List<FieldSummaryDto> fieldDtos = fields.stream()
                .map(this::toFieldSummaryDto)
                .toList();
        Object defaultValues = toJsonValues(info.defaultInstance());
        return new ConfigDetailDto(info.fqcn(), fieldDtos, defaultValues);
    }

    /**
     * Builds a configuration object from the supplied field values, validates it with
     * Hibernate Validator, and returns the YAML representation on success.
     *
     * <p>Validation failures are returned as a non-null {@code errorMessage} rather than
     * an HTTP error status so the client can display them inline.
     *
     * @param fqcn        the fully qualified class name of the configuration class
     * @param fieldValues map of Jackson property names to values; may be empty to use all defaults
     * @return build result with YAML on success, or error message on failure
     */
    @Operation(
            summary = "Build YAML from field values without writing to disk",
            description = "Constructs a configuration object from the provided field values, runs Hibernate "
                    + "Validator against it, and returns the generated YAML string on success. "
                    + "Validation failures are returned in the errorMessage field (HTTP 200) rather than "
                    + "as an error status, so the UI can display them inline without treating them as "
                    + "network errors. Pass an empty object {} to preview YAML using all default values."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Build attempted — check errorMessage field for validation failures",
                    content = @Content(schema = @Schema(implementation = BuildResultDto.class))),
            @ApiResponse(responseCode = "400", description = "Malformed request body — JSON could not be parsed"),
            @ApiResponse(responseCode = "404", description = "No configuration class found for the given FQCN")
    })
    @POST
    @Path("{fqcn}/build")
    public BuildResultDto buildConfig(
            @Parameter(description = "Fully qualified class name of the configuration class to build",
                    example = "com.viveworks.appbase.client.service.kafka.configuration.DefaultKafkaClientConfiguration",
                    required = true)
            @PathParam("fqcn") final String fqcn,
            @RequestBody(
                    description = "Map of Jackson property names to their values. "
                            + "Omitted keys fall back to the class default. "
                            + "Pass an empty object {} to use all defaults.",
                    content = @Content(schema = @Schema(example = "{\"bootstrapServers\":\"localhost:9092\",\"groupId\":\"my-group\"}")))
            final Map<String, Object> fieldValues) {
        ConfigClassInfo info = findByFqcn(fqcn);
        Map<String, Object> safeFieldValues = fieldValues != null ? fieldValues : Collections.emptyMap();
        // L4-02 (CWE-400): reject an oversized/deeply-nested field-values map before it ever
        // reaches Jackson conversion or Hibernate Validator — both scale with the structure's
        // size, so an unbounded request amplifies work on every call.
        try {
            FieldValuesLimits.validate(safeFieldValues);
        } catch (FieldValuesTooLargeException e) {
            return new BuildResultDto(null, e.getMessage());
        }
        try {
            Object built = prompter.buildFromMap(info, safeFieldValues);
            Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(built);
            if (!violations.isEmpty()) {
                String msg = violations.stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .collect(Collectors.joining(", "));
                return new BuildResultDto(null, msg);
            }
            String yaml = yamlMapper.writeValueAsString(built);
            return new BuildResultDto(yaml, null);
        } catch (Throwable t) {
            // Single catch arm for both ordinary Exceptions and Errors (e.g. StackOverflowError
            // from Hibernate Validator on certain types), so Jetty does not close the connection
            // without sending a response. clientSafeFailure (AUD-09) routes both through the same
            // disclosure-free correlation-id response regardless of which one was thrown.
            return new BuildResultDto(null, clientSafeFailure(t));
        }
    }

    /**
     * Builds a configuration object from the supplied field values and writes it to a YAML file.
     *
     * <p>When {@code body.outputPath()} is {@code null}, the file is written to a generated
     * temp path. The written file path is returned on success.
     *
     * @param fqcn the fully qualified class name of the configuration class
     * @param body request containing field values and an optional explicit output path
     * @return write result with file path on success, or error message on failure
     */
    @Operation(
            summary = "Build YAML and write the configuration file to disk",
            description = "Constructs a configuration object from the provided field values and writes it "
                    + "as a YAML file to the specified outputPath. If outputPath is omitted, the file is "
                    + "written to a system-generated temporary path (useful for preview). "
                    + "The path of the written file, relative to the wizard's root, is returned on success. "
                    + "On error (e.g. I/O failure, invalid field values), errorMessage is populated instead."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Write attempted — check errorMessage field for errors",
                    content = @Content(schema = @Schema(implementation = WriteResultDto.class))),
            @ApiResponse(responseCode = "400", description = "Malformed request body — JSON could not be parsed"),
            @ApiResponse(responseCode = "404", description = "No configuration class found for the given FQCN")
    })
    @POST
    @Path("{fqcn}/write")
    public WriteResultDto writeConfig(
            @Parameter(description = "Fully qualified class name of the configuration class to write",
                    example = "com.viveworks.appbase.client.service.kafka.configuration.DefaultKafkaClientConfiguration",
                    required = true)
            @PathParam("fqcn") final String fqcn,
            @RequestBody(
                    description = "Field values to build the configuration from, plus an optional explicit output path.",
                    content = @Content(schema = @Schema(implementation = WriteRequestDto.class)))
            final WriteRequestDto body) {
        ConfigClassInfo info = findByFqcn(fqcn);
        // ⚠️ The confinement check runs BEFORE the try. The blocks below catch Exception AND
        // Throwable, folding everything into a 200 with an errorMessage, so a refusal thrown
        // inside would never reach the HTTP layer.
        //
        // This endpoint bypasses ImportService entirely and calls writer.write() directly. The
        // 2026-07-26 Copilot scan reported only the ImportApiResource instance; a fix applied
        // where it pointed would have left this one open (internal work item 751 F-08).
        if (body != null && body.outputPath() != null) {
            try {
                confinement.confine(body.outputPath());
            } catch (PathOutsideRootException e) {
                throw new WebApplicationException(
                        Response.status(Response.Status.FORBIDDEN)
                                .entity("The requested path is outside the permitted root.")
                                .build());
            }
        }
        Map<String, Object> fieldValues = body != null && body.fieldValues() != null
                ? body.fieldValues()
                : Collections.emptyMap();
        // L4-02 (CWE-400): same oversize/nesting/value-length rejection as buildConfig, before
        // any Jackson conversion or disk write is attempted.
        try {
            FieldValuesLimits.validate(fieldValues);
        } catch (FieldValuesTooLargeException e) {
            return new WriteResultDto(null, e.getMessage());
        }
        try {
            Object built = prompter.buildFromMap(info, fieldValues);
            java.nio.file.Path outputPath = body != null && body.outputPath() != null
                    ? Paths.get(body.outputPath())
                    : null;
            java.nio.file.Path written = writer.write(built, outputPath);
            // A10-01 (internal work item 1449): report the path relative to the root, never the absolute filesystem path.
            return new WriteResultDto(confinement.toDisplayPath(written), null);
        } catch (Throwable t) {
            return new WriteResultDto(null, clientSafeFailure(t));
        }
    }

    /**
     * AUD-09 (CWE-209/497): the raw exception message/type can disclose filesystem paths, Java
     * types, or field/property names. The client gets only a stable, secret-free message plus an
     * opaque correlation id; the full failure, including the original throwable and stack trace,
     * is logged server-side under that id.
     *
     * @param failure the caught throwable
     * @return a client-safe message carrying only an opaque correlation id
     */
    private static String clientSafeFailure(final Throwable failure) {
        final String correlationId = UUID.randomUUID().toString();
        LOGGER.log(Level.SEVERE, FAILURE_MESSAGE + correlationId, failure);
        return FAILURE_MESSAGE + correlationId;
    }

    /**
     * Returns the default instance of the requested configuration class as a JSON-compatible
     * value map.
     *
     * @param fqcn the fully qualified class name, or simple name, of the configuration class
     * @return default values DTO; {@code values} is {@code null} when no default can be constructed
     */
    @Operation(
            summary = "Get default field values for a configuration class",
            description = "Returns the default instance of the configuration class serialized as a flat "
                    + "JSON-compatible map (property name → value). This is the same data available in "
                    + "the 'defaultValues' field of GET /{fqcn}, exposed here as a standalone endpoint "
                    + "for clients that only need defaults without full field metadata. "
                    + "The 'values' field is null when the class has no zero-argument constructor and "
                    + "cannot produce a default instance."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default values returned — values field may be null if no default exists",
                    content = @Content(schema = @Schema(implementation = DefaultDto.class))),
            @ApiResponse(responseCode = "404", description = "No configuration class found for the given FQCN or simple name")
    })
    @GET
    @Path("{fqcn}/default")
    public DefaultDto getDefaultValues(
            @Parameter(description = "Fully qualified class name or unambiguous simple name of the configuration class",
                    example = "com.viveworks.appbase.client.service.kafka.configuration.DefaultKafkaClientConfiguration",
                    required = true)
            @PathParam("fqcn") final String fqcn) {
        ConfigClassInfo info = findByFqcn(fqcn);
        return new DefaultDto(toJsonValues(info.defaultInstance()));
    }

    /**
     * Sub-resource locator for import operations at {@code /configurations/import}.
     *
     * <p>Jersey dispatches any request to {@code /api/configurations/import/**} to the
     * returned {@link ImportApiResource} instance, which handles the {@code /scan} and
     * {@code /{fqcn}/write} endpoints.
     *
     * @return the import sub-resource; never {@code null}
     */
    @Path("import")
    public ImportApiResource getImportResource() {
        return importResource;
    }

    /**
     * Finds a {@link ConfigClassInfo} by exact FQCN or simple-name match within the supported
     * configuration registry.
     *
     * <p>The registry starts with scanned configurations and recursively includes only nested object
     * and collection-element types advertised by their field metadata. It therefore preserves the
     * nested form flow without accepting an arbitrary request-selected loadable class.
     *
     * @param fqcn the fully qualified class name or simple name to look up
     * @return the matching config class info
     * @throws NotFoundException when no class can be found by that name
     */
    private ConfigClassInfo findByFqcn(final String fqcn) {
        // The scanner's result is the ALLOWLIST, and it is the control -- not a hint. The previous
        // orElseGet(...loadClass(fqcn)) made a miss WIDEN instead of reject: an fqcn the scanner never
        // returned was loaded from the context class loader anyway and handed to scanner.infoFor(...),
        // so an unvalidated path parameter selected the class. orElseThrow keeps the allowlist
        // authoritative -- a name that is not in it is refused whether or not it happens to resolve.
        // (P-042 E01 N08, internal work item 1379 -- register row AUD-06, CWE-470.)
        ConfigClassInfo info = supportedClasses.resolve(fqcn);
        if (info == null) {
            throw new NotFoundException("Config class not found: " + fqcn);
        }
        return info;
    }

    /**
     * Converts a {@link ConfigClassInfo} to a lightweight {@link ConfigSummaryDto}.
     *
     * @param info the config class metadata
     * @return summary DTO
     */
    private ConfigSummaryDto toSummaryDto(final ConfigClassInfo info) {
        return new ConfigSummaryDto(info.fqcn(), info.simpleName(), info.layer(), info.module());
    }

    /**
     * Converts a {@link FieldInfo} to a {@link FieldSummaryDto}.
     *
     * @param field the field metadata
     * @return field summary DTO
     */
    private FieldSummaryDto toFieldSummaryDto(final FieldInfo field) {
        Object raw = field.defaultValue();
        // Only stringify leaf-type defaults (primitives, Strings). Nested config defaults are
        // objects whose toString() is meaningless — Flutter expects null for nested fields.
        String defaultStr = null;
        if (raw != null && field.nestedClass() == null) {
            defaultStr = raw.toString();
        }
        String nestedTypeFqcn = field.nestedClass() != null ? field.nestedClass().getName() : null;
        List<String> enumValues = null;
        if (field.type().isEnum()) {
            enumValues = Arrays.stream(field.type().getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        String elementNestedTypeFqcn = field.elementClass() != null ? field.elementClass().getName() : null;
        return new FieldSummaryDto(field.name(), field.typeName(), field.required(), defaultStr, nestedTypeFqcn, enumValues, elementNestedTypeFqcn);
    }

    /**
     * Serializes an object to a JSON-compatible value map via Jackson
     * {@link ObjectMapper#convertValue}.
     *
     * @param obj the object to convert; may be {@code null}
     * @return a JSON-compatible map, or {@code null} when {@code obj} is {@code null} or
     *         conversion is not possible
     */
    @Nullable
    private Object toJsonValues(@Nullable final Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() { });
        } catch (Exception e) {
            // Cannot convert to map (e.g. class lacks Jackson serialization) — return null
            // so the Flutter client gets an empty defaults map rather than a String that
            // would fail Dart's Map<String, dynamic> cast.
            return null;
        }
    }
}
