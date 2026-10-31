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

package appbase_mcp.generator;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Generates the deterministic schema-v1 MCP API catalog directly from Appbase Java source.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class ApiSurfaceGenerator {
    private static final int REQUIRED_ARGUMENTS = 3;

    private ApiSurfaceGenerator() {
    }

    /**
     * Generates an API-surface artifact.
     *
     * @param args repository root, source revision, and output path
     * @throws Exception when source discovery, parsing, or output fails
     * @throws IllegalArgumentException when the arguments or revision are invalid
     * @throws IllegalStateException when no source API can be cataloged
    */
    // Command-line entry point used by scripts/generate-mcp-api-surface.sh.
    @SuppressWarnings("checkstyle:UncommentedMain")
    public static void main(final String[] args) throws Exception {
        if (args.length != REQUIRED_ARGUMENTS) {
            throw new IllegalArgumentException(
                    "usage: ApiSurfaceGenerator <repository-root> <source-revision> <output>");
        }
        final Path root = Path.of(args[0]).toRealPath();
        final String revision = args[1];
        if (!revision.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("source revision must be a lowercase 40-character SHA");
        }
        final List<Path> sources = trackedJavaSources(root);
        if (sources.isEmpty()) {
            throw new IllegalStateException("no tracked Appbase Java sources found");
        }
        final List<Symbol> symbols = parse(root, sources);
        if (symbols.isEmpty()) {
            throw new IllegalStateException("no public interface or annotation API symbols found");
        }
        symbols.sort(Comparator.comparing(Symbol::id));
        writeArtifact(Path.of(args[2]), revision, symbols);
    }

    private static List<Path> trackedJavaSources(final Path root) throws IOException, InterruptedException {
        final Process process = new ProcessBuilder(
                "git", "-C", root.toString(), "ls-files", "-z", "--", "appbase-java/**/*.java")
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) {
            throw new IOException("git ls-files failed while selecting Java sources");
        }
        final List<Path> sources = new ArrayList<>();
        for (final String relative : output.split(String.valueOf(Character.MIN_VALUE))) {
            if (!relative.isEmpty() && relative.contains("/src/main/java/")) {
                sources.add(root.resolve(relative));
            }
        }
        sources.sort(Comparator.naturalOrder());
        return sources;
    }

    private static List<Symbol> parse(final Path root, final List<Path> sources) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("a JDK is required to generate the API surface");
        }
        try (StandardJavaFileManager files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            final JavacTask task = (JavacTask) compiler.getTask(
                    null, files, null, List.of("-proc:none"), null,
                    files.getJavaFileObjectsFromPaths(sources));
            final List<Symbol> result = new ArrayList<>();
            for (final CompilationUnitTree unit : task.parse()) {
                final String packageName = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
                final Path source = Path.of(unit.getSourceFile().toUri());
                final String relative = root.relativize(source).toString().replace('\\', '/');
                for (final Tree declaration : unit.getTypeDecls()) {
                    if (declaration instanceof ClassTree type) {
                        collectType(result, type, packageName, relative);
                    }
                }
            }
            return result;
        }
    }

    private static void collectType(
            final List<Symbol> result,
            final ClassTree type,
            final String owner,
            final String sourcePath)
    {
        final boolean apiType = (type.getKind() == Tree.Kind.INTERFACE ||
                type.getKind() == Tree.Kind.ANNOTATION_TYPE) &&
                type.getModifiers().getFlags().contains(Modifier.PUBLIC);
        final String qualified = owner.isEmpty() ?
                type.getSimpleName().toString() : owner + "." + type.getSimpleName();
        if (apiType) {
            final String kind = type.getKind() == Tree.Kind.ANNOTATION_TYPE ?
                    "annotation" : "interface";
            result.add(new Symbol(qualified, qualified, kind, typeSignature(type, kind), sourcePath));
            for (final Tree member : type.getMembers()) {
                if (member instanceof MethodTree method &&
                        !method.getModifiers().getFlags().contains(Modifier.PRIVATE))
                {
                    final String parameters = method.getParameters().stream()
                            .map(variable -> variable.getType().toString())
                            .reduce((left, right) -> left + "," + right)
                            .orElse("");
                    final String id = qualified + "#" + method.getName() + "(" + parameters + ")";
                    result.add(new Symbol(id, qualified, "method", methodSignature(method), sourcePath));
                }
            }
        }
        for (final Tree member : type.getMembers()) {
            if (member instanceof ClassTree nested) {
                collectType(result, nested, qualified, sourcePath);
            }
        }
    }

    private static String typeSignature(final ClassTree type, final String kind) {
        final StringBuilder value = new StringBuilder("public ").append(kind).append(' ')
                .append(type.getSimpleName());
        if (!type.getTypeParameters().isEmpty()) {
            value.append('<').append(String.join(", ", type.getTypeParameters().stream()
                    .map(Object::toString).toList())).append('>');
        }
        if (type.getExtendsClause() != null) {
            value.append(" extends ").append(type.getExtendsClause());
        }
        if (!type.getImplementsClause().isEmpty()) {
            value.append(" extends ").append(String.join(", ", type.getImplementsClause().stream()
                    .map(Object::toString).toList()));
        }
        return value.toString();
    }

    private static String methodSignature(final MethodTree method) {
        final StringBuilder value = new StringBuilder();
        final Set<Modifier> modifiers = method.getModifiers().getFlags();
        if (modifiers.contains(Modifier.STATIC)) {
            value.append("static ");
        } else if (modifiers.contains(Modifier.DEFAULT)) {
            value.append("default ");
        }
        if (!method.getTypeParameters().isEmpty()) {
            value.append('<').append(String.join(", ", method.getTypeParameters().stream()
                    .map(Object::toString).toList())).append("> ");
        }
        value.append(method.getReturnType()).append(' ').append(method.getName()).append('(');
        value.append(String.join(", ", method.getParameters().stream()
                .map(ApiSurfaceGenerator::parameterSignature).toList()));
        value.append(')');
        if (!method.getThrows().isEmpty()) {
            value.append(" throws ").append(String.join(", ", method.getThrows().stream()
                    .map(Object::toString).toList()));
        }
        if (method.getDefaultValue() != null) {
            value.append(" default ").append(method.getDefaultValue());
        }
        return value.toString();
    }

    private static String parameterSignature(final VariableTree parameter) {
        return parameter.getType() + " " + parameter.getName();
    }

    private static void writeArtifact(final Path output, final String revision, final List<Symbol> symbols)
            throws IOException
    {
        final StringBuilder json = new StringBuilder();
        json.append("{\n  \"schema_version\": 1,\n  \"source_revision\": \"")
                .append(revision).append("\",\n  \"symbols\": [\n");
        for (int index = 0; index < symbols.size(); index++) {
            final Symbol symbol = symbols.get(index);
            json.append("    {\"id\": \"").append(escape(symbol.id()))
                    .append("\", \"qualified_name\": \"").append(escape(symbol.qualifiedName()))
                    .append("\", \"kind\": \"").append(symbol.kind())
                    .append("\", \"signature\": \"").append(escape(symbol.signature()))
                    .append("\", \"source_path\": \"").append(escape(symbol.sourcePath())).append("\"}");
            json.append(index + 1 == symbols.size() ? "\n" : ",\n");
        }
        json.append("  ]\n}\n");
        final Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(output, json, StandardCharsets.UTF_8);
    }

    private static String escape(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
    }

    private record Symbol(String id, String qualifiedName, String kind, String signature, String sourcePath) {
    }
}
