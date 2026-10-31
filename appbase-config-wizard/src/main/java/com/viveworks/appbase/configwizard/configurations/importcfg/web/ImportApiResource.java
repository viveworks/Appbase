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

package com.viveworks.appbase.configwizard.configurations.importcfg.web;

import com.viveworks.appbase.configwizard.configurations.importcfg.ImportService;
import com.viveworks.appbase.configwizard.configurations.importcfg.TypeInferenceEngine;
import com.viveworks.appbase.configwizard.configurations.importcfg.web.dto.FileEntryDto;
import com.viveworks.appbase.configwizard.configurations.importcfg.web.dto.ImportScanResultDto;
import com.viveworks.appbase.configwizard.configurations.importcfg.web.dto.ImportWriteRequestDto;
import com.viveworks.appbase.configwizard.configurations.importcfg.web.dto.TypeCandidateDto;
import com.viveworks.appbase.configwizard.configurations.prompt.FieldPrompter;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigScanner;
import com.viveworks.appbase.configwizard.configurations.web.ConfigurationsApiResource;
import com.viveworks.appbase.configwizard.configurations.web.dto.WriteResultDto;
import com.viveworks.appbase.configwizard.configurations.writer.YamlWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.viveworks.appbase.configwizard.security.FieldValuesLimits;
import com.viveworks.appbase.configwizard.security.FieldValuesTooLargeException;
import com.viveworks.appbase.configwizard.security.ImportScanLimits;
import com.viveworks.appbase.configwizard.security.ImportScanTooLargeException;
import com.viveworks.appbase.configwizard.security.PathConfinement;
import com.viveworks.appbase.configwizard.security.PathOutsideRootException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Sub-resource for YAML configuration import operations at
 * {@code /api/configurations/import}.
 *
 * <p>Injected as a sub-resource locator from
 * {@link ConfigurationsApiResource#getImportResource()}.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET  /api/configurations/import/scan?dir=…} — scans a directory for YAML files
 *       and returns each file annotated with up to 5 inferred Appbase configuration type
 *       candidates.</li>
 *   <li>{@code POST /api/configurations/import/{fqcn}/write} — builds a configuration object
 *       from the provided field values and writes it to disk, returning the written file path.</li>
 * </ul>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class ImportApiResource {

    private static final Logger LOGGER = Logger.getLogger(ImportApiResource.class.getName());
    private static final String FAILURE_MESSAGE = "Import request failed; reference=";

    private final TypeInferenceEngine engine;
    private final ImportService importService;
    private final PathConfinement confinement;

    /**
     * Constructs an {@code ImportApiResource} with the given collaborators.
     *
     * <p>Eagerly initialises the {@link TypeInferenceEngine} by scanning all config classes
     * so that inference calls are fast at request time.
     *
     * @param scanner     discovers Appbase configuration classes on the classpath
     * @param prompter    introspects and builds configuration objects
     * @param writer      writes YAML files to disk
     * @param confinement bounds every caller-supplied path; use
     *                    {@link PathConfinement#none()} for the interactive console only
     */
    public ImportApiResource(final ConfigScanner scanner,
                             final FieldPrompter prompter,
                             final YamlWriter writer,
                             final PathConfinement confinement) {
        this.engine = new TypeInferenceEngine(scanner.scan());
        this.importService = new ImportService(scanner, prompter, writer);
        this.confinement = confinement;
    }

    /**
     * Builds the disclosure-free 403 returned for any path outside the permitted root.
     *
     * @return a 403 {@link WebApplicationException}
     */
    private static WebApplicationException forbidden() {
        return new WebApplicationException(
                Response.status(Response.Status.FORBIDDEN)
                        .entity("The requested path is outside the permitted root.")
                        .build());
    }

    /**
     * Scans a directory for YAML files and returns each file with up to 5 inferred
     * Appbase configuration type candidates.
     *
     * <p>Non-existent or non-directory paths return an empty file list rather than an error.
     * Only files ending with {@code .yaml} or {@code .yml} are included.
     *
     * <p>L2-11 (CWE-400): the scan is bounded by {@link ImportScanLimits} — entry count,
     * per-file bytes, and aggregate bytes. A directory exceeding any bound is refused with a
     * stable 413 carrying a disclosure-free message, before the offending content is read.
     *
     * @param dir absolute path of the directory to scan; may be {@code null} or blank
     * @return scan result with zero or more file entries; never {@code null}
     */
    @Operation(
            summary = "Scan a directory for importable YAML configuration files",
            description = "Lists all .yaml / .yml files directly in the given directory (non-recursive) "
                    + "and returns each one annotated with up to 5 inferred Appbase configuration "
                    + "type candidates, scored by filename tokenization and field overlap. "
                    + "A confidence >= 0.7 is considered 'Suggested'. Returns an empty file list "
                    + "if the directory does not exist or is not readable."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Scan complete — zero or more YAML files returned",
                    content = @Content(schema = @Schema(implementation = ImportScanResultDto.class)))
    })
    @GET
    @jakarta.ws.rs.Path("scan")
    public ImportScanResultDto scan(
            @Parameter(description = "Absolute path of the directory to scan for YAML files.",
                    example = "/home/dev/configs",
                    required = true)
            @QueryParam("dir") final String dir) {
        if (dir == null || dir.isBlank()) {
            return new ImportScanResultDto(List.of());
        }
        final Path dirPath;
        try {
            dirPath = confinement.confine(dir);
        } catch (PathOutsideRootException e) {
            throw forbidden();
        }
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return new ImportScanResultDto(List.of());
        }
        // L2-11 (CWE-400): confinement bounds *where* this scan may read, not *how much*.
        // Every YAML candidate is admitted against the entry-count / per-file / aggregate
        // budget DURING enumeration — before any content is read and before the admitted set
        // is sorted. Admitting after a full sort would already have paid the unbounded
        // enumeration-and-buffering these bounds exist to prevent; admitting during
        // enumeration stops it at the cap by throwing ImportScanTooLargeException.
        final ImportScanLimits.Budget budget = new ImportScanLimits.Budget();
        final List<Path> admitted = new ArrayList<>();
        try (Stream<Path> entries = Files.list(dirPath)) {
            for (final Iterator<Path> it = entries.iterator(); it.hasNext(); ) {
                final Path p = it.next();
                final String name = p.getFileName().toString().toLowerCase();
                if (!name.endsWith(".yaml") && !name.endsWith(".yml")) {
                    continue;
                }
                try {
                    budget.admit(p);
                } catch (final UncheckedIOException e) {
                    // An entry that vanished between listing and admission races with a
                    // concurrent writer, not an attack; skip it, exactly like toFileEntry
                    // skips unreadable content below.
                    continue;
                }
                admitted.add(p);
            }
        } catch (final ImportScanTooLargeException e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
                            .entity(e.getMessage())
                            .build());
        } catch (final IOException e) {
            return new ImportScanResultDto(List.of());
        }
        admitted.sort(Comparator.naturalOrder());
        final List<FileEntryDto> files = new ArrayList<>(admitted.size());
        for (final Path p : admitted) {
            // ⚠️ The symlink check must stay immediately upstream of the content read in this
            // same iteration, with no intervening batch pass. The original pipeline enforced
            // this by placing the check after sorted() within one stream pass (see the L4-07
            // history): an op separated from the read by a stateful boundary reopens the
            // TOCTOU window where a checked-safe file is swapped for a symlink before its
            // read. The loop shape preserves the invariant the same way — check, then read,
            // back-to-back, one entry at a time.
            // L4-07 (CWE-59, CWE-200): the directory itself is confined, but a symlink entry
            // inside it is not — Files.readString follows symlinks, so a link planted inside
            // the root could disclose a file outside it. Reject symlink entries outright
            // rather than following and re-confining: this is a listing/read surface with no
            // legitimate need to resolve a link's target.
            if (Files.isSymbolicLink(p)) {
                continue;
            }
            files.add(toFileEntry(p));
        }
        return new ImportScanResultDto(files);
    }

    /**
     * Builds the configuration for the given FQCN from the request field values and writes it
     * to disk.
     *
     * <p>Output path resolution (first match wins):
     * <ol>
     *   <li>{@code body.outputPath()} non-null → write to that path.</li>
     *   <li>Both null → write to a system temp file.</li>
     * </ol>
     *
     * @param fqcn the fully qualified class name of the configuration to build
     * @param body the write request containing field overrides and optional output path
     * @return write result with the path relative to the root on success, or an error message on failure
     */
    @Operation(
            summary = "Build and write an imported configuration YAML to disk",
            description = "Constructs an Appbase configuration object from the provided field values "
                    + "and writes it as a YAML file. If outputPath is omitted, the file is written "
                    + "to a system-generated temp path. Returns the path of the written file relative to "
                    + "the wizard's root on success, or an errorMessage on failure."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Write attempted — check errorMessage field for failures",
                    content = @Content(schema = @Schema(implementation = WriteResultDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "Malformed request body — JSON could not be parsed"),
            @ApiResponse(responseCode = "404",
                    description = "No configuration class found for the given FQCN")
    })
    @POST
    @jakarta.ws.rs.Path("{fqcn}/write")
    public WriteResultDto write(
            @Parameter(description = "Fully qualified class name of the configuration to write.",
                    example = "com.viveworks.appbase.client.service.redis.configuration.DefaultRedisClientIntegrationServiceConfiguration",
                    required = true)
            @PathParam("fqcn") final String fqcn,
            @RequestBody(
                    description = "Field override values and optional explicit output path.",
                    content = @Content(schema = @Schema(implementation = ImportWriteRequestDto.class)))
            final ImportWriteRequestDto body) {
        // ⚠️ The confinement check runs BEFORE the try. The catch below folds every exception into
        // a 200 with an errorMessage, so a refusal thrown inside it would never reach the HTTP
        // layer — the caller would read a rejected write as an ordinary failure.
        final String outputPath = body != null ? body.outputPath() : null;
        if (outputPath != null) {
            try {
                confinement.confine(outputPath);
            } catch (PathOutsideRootException e) {
                throw forbidden();
            }
        }
        Map<String, Object> fieldValues = body != null && body.fieldValues() != null
                ? body.fieldValues()
                : Collections.emptyMap();
        // L4-02 (CWE-400): reject an oversized/deeply-nested field-values map before it reaches
        // ImportService#write / FieldPrompter#buildFromMap — matches ConfigurationsApiResource's
        // buildConfig/writeConfig bound on the same attacker-controlled map shape.
        try {
            FieldValuesLimits.validate(fieldValues);
        } catch (FieldValuesTooLargeException e) {
            return new WriteResultDto(null, e.getMessage());
        }
        try {
            String sourceFile = body != null ? body.sourceFile() : null;
            Path written = importService.write(fqcn, fieldValues, sourceFile,
                    outputPath, null);
            // A10-01 (internal work item 1449): report the path relative to the root, never the absolute filesystem path.
            return new WriteResultDto(confinement.toDisplayPath(written), null);
        } catch (Throwable t) {
            return new WriteResultDto(null, clientSafeFailure(t));
        }
    }

    /**
     * AUD-09 (CWE-209/497): the raw exception message/type can disclose filesystem paths, Java
     * types, or field/property names (e.g. a Jackson conversion failure that echoes the input
     * map's keys and values back into the exception text). The client gets only a stable,
     * secret-free message plus an opaque correlation id; the full failure, including the
     * original throwable and stack trace, is logged server-side under that id.
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
     * Converts a YAML file path to a {@link FileEntryDto} with inferred type candidates.
     *
     * <p>Inference errors are swallowed — the entry is returned with an empty candidates list
     * so a single unreadable file does not abort the scan of its siblings.
     *
     * @param yamlFile path of the YAML file to convert
     * @return populated file entry DTO
     */
    private FileEntryDto toFileEntry(final Path yamlFile) {
        List<TypeCandidateDto> candidates;
        try {
            candidates = engine.infer(yamlFile);
        } catch (IOException e) {
            candidates = List.of();
        }
        // toDisplayPath canonicalises the entry, which throws PathOutsideRootException for an
        // unresolvable component (an unreadable or cyclic symlink). Swallow it to the file name —
        // which never leaks the layout, the same fallback toDisplayPath uses — so a single bad entry
        // does not abort the scan of its siblings, matching the infer resilience above (internal work item 1513).
        String displayPath;
        try {
            displayPath = confinement.toDisplayPath(yamlFile);
        } catch (PathOutsideRootException e) {
            displayPath = yamlFile.getFileName().toString();
        }
        return new FileEntryDto(
                displayPath,
                yamlFile.getFileName().toString(),
                candidates);
    }
}
