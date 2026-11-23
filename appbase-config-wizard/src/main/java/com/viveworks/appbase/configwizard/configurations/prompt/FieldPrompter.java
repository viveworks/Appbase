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

package com.viveworks.appbase.configwizard.configurations.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.node.NullNode;

import java.lang.reflect.Modifier;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.configwizard.ConsoleColors;
import com.viveworks.appbase.configwizard.menu.PromptUtil;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigClassInfo;
import org.jline.terminal.Terminal;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for:
 * <ol>
 *   <li>Introspecting Appbase config classes via Jackson {@link BeanDescription} to produce
 *       a {@link FieldInfo} list.</li>
 *   <li>Building a config instance from a {@link Map} of field values (REST path).</li>
 *   <li>Prompting the user interactively via JLine3 (console path — added in Task 4).</li>
 * </ol>
 *
 * <p>Both Jackson patterns are supported:
 * <ul>
 *   <li>{@code @JsonCreator} + {@code @JsonProperty} (AppbaseConfiguration impls in service-core)</li>
 *   <li>Builder + {@code @JsonDeserialize(builder=…)} + {@code @JsonPOJOBuilder}
 *       (AbstractAppbaseManagedServiceConfiguration subclasses)</li>
 * </ul>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class FieldPrompter {

    private final ObjectMapper mapper;

    /**
     * Constructs a {@code FieldPrompter} using the supplied Jackson {@link ObjectMapper}.
     *
     * @param mapper the Jackson mapper used for introspection and value conversion
     */
    public FieldPrompter(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Introspects {@code info.configClass()} via {@code mapper.getDeserializationConfig().introspect()}.
     *
     * <p>For each deserializable property the following metadata is derived:
     * <ul>
     *   <li>{@code typeName} — simple Java class name (e.g. "String", "boolean", "Integer")</li>
     *   <li>{@code defaultValue} — string representation of the value from
     *       {@code info.defaultInstance()}, or {@code null} when no default is available</li>
     *   <li>{@code required} — {@code true} for Java primitive types (non-nullable)</li>
     *   <li>{@code nestedClass} — set when the property type carries
     *       {@code @JsonDeserialize(builder=…)} or implements {@link AppbaseConfiguration}</li>
     * </ul>
     *
     * @param info the config class metadata whose class to introspect
     * @return ordered list of field metadata matching Jackson property order
     */
    public List<FieldInfo> introspect(final ConfigClassInfo info) {
        JavaType javaType = mapper.constructType(info.configClass());
        BeanDescription beanDesc = mapper.getDeserializationConfig().introspect(javaType);
        return beanDesc.findProperties().stream()
                .map(prop -> toFieldInfo(prop, info.defaultInstance()))
                .toList();
    }

    /**
     * Builds a config instance from a string/object map (for the REST endpoint).
     *
     * <p>Converts {@code fieldValues} to the target type via
     * {@code mapper.convertValue(fieldValues, info.configClass())}.
     * Any properties absent from the map retain their builder defaults.
     *
     * @param info        the config class metadata identifying the target class
     * @param fieldValues map of Jackson property names to values (may be nested maps for sub-objects)
     * @return the built config instance
     * @throws IllegalArgumentException when conversion fails due to a type mismatch or validation error
     */
    public Object buildFromMap(final ConfigClassInfo info, final Map<String, Object> fieldValues) {
        try {
            return mapper.convertValue(fieldValues, info.configClass());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Failed to convert values to " + info.configClass().getSimpleName()
                            + ": " + e.getMessage(), e);
        }
    }

    /**
     * Interactively prompts the user for each field of {@code info.configClass()}.
     *
     * <p>Shows the existing defaults (from {@code info.defaultInstance()}) next to each
     * prompt. Recurses into nested {@link AppbaseConfiguration} fields up to depth
     * {@value #MAX_RECURSION_DEPTH}; at that depth offers {@code [use defaults]} to skip
     * without prompting sub-fields.
     *
     * @param info     config class to build
     * @param terminal JLine3 terminal (caller owns lifecycle)
     * @return raw map of Jackson property names to values (not yet converted to the config type)
     * @throws Exception on terminal I/O failure or Jackson conversion error
     */
    public Map<String, Object> prompt(final ConfigClassInfo info, final Terminal terminal)
            throws Exception {
        PromptUtil promptUtil = new PromptUtil(terminal);
        return promptInternal(info, promptUtil, terminal, 0);
    }

    /**
     * Internal recursive implementation shared by {@link #prompt} and nested-field recursion.
     *
     * @param info       config class to build
     * @param promptUtil the shared readline helper
     * @param terminal   JLine3 terminal used for header lines printed outside PromptUtil
     * @param depth      current recursion depth
     * @return raw map of Jackson property names to prompted values
     * @throws Exception on terminal I/O failure or Jackson conversion error
     */
    private Map<String, Object> promptInternal(final ConfigClassInfo info,
            final PromptUtil promptUtil, final Terminal terminal, final int depth) throws Exception {
        List<FieldInfo> fields = introspect(info);
        Map<String, Object> fieldValues = new LinkedHashMap<>();

        for (FieldInfo field : fields) {
            if (field.nestedClass() != null) {
                handleNestedField(field, info, promptUtil, terminal, fieldValues, depth);
            } else if (Map.class.isAssignableFrom(field.type())) {
                handleMapField(field, promptUtil, terminal, fieldValues, depth);
            } else if (Collection.class.isAssignableFrom(field.type())) {
                handleCollectionField(field, promptUtil, terminal, fieldValues, depth);
            } else {
                handleSimpleField(field, promptUtil, fieldValues);
            }
        }

        return fieldValues;
    }

    /**
     * Handles a single nested/complex field by offering a "use defaults" shortcut or recursing.
     *
     * <p>At recursion depth {@value #MAX_RECURSION_DEPTH} the nested object is taken from
     * the parent's {@code defaultInstance} without further prompting.
     *
     * @param field       the nested field descriptor
     * @param parentInfo  the enclosing config class (provides the parent default instance)
     * @param promptUtil  the shared readline helper
     * @param terminal    the JLine3 terminal, used to print section headers
     * @param fieldValues the map being assembled for the enclosing class
     * @param depth       current recursion depth
     * @throws Exception on terminal I/O failure or Jackson conversion error
     */
    private void handleNestedField(final FieldInfo field, final ConfigClassInfo parentInfo,
            final PromptUtil promptUtil, final Terminal terminal,
            final Map<String, Object> fieldValues, final int depth) throws Exception {
        Object nestedDefault = extractNestedDefault(
                parentInfo.defaultInstance(), field.name(), field.nestedClass());
        ConfigClassInfo nestedInfo = new ConfigClassInfo(
                field.nestedClass().getName(),
                field.nestedClass().getSimpleName(),
                "", "",
                field.nestedClass(),
                nestedDefault);

        if (depth >= MAX_RECURSION_DEPTH) {
            // Reached max depth — use defaults silently
            if (nestedDefault != null) {
                fieldValues.put(field.name(), mapper.convertValue(
                        nestedDefault, new TypeReference<Map<String, Object>>() { }));
            }
            return;
        }

        String label = "  " + field.name() + " (" + field.typeName() + ") — use defaults? [y]: ";
        String answer = promptUtil.readString(label, "y");
        boolean useDefaults = answer == null || "y".equalsIgnoreCase(answer)
                || "yes".equalsIgnoreCase(answer);

        if (useDefaults) {
            if (nestedDefault != null) {
                fieldValues.put(field.name(), mapper.convertValue(
                        nestedDefault, new TypeReference<Map<String, Object>>() { }));
            }
        } else {
            terminal.writer().println(ConsoleColors.cyan("  +-- " + field.name() + " (" + field.typeName() + ") --", terminal));
            terminal.writer().flush();
            Map<String, Object> nested = promptInternal(nestedInfo, promptUtil, terminal, depth + 1);
            fieldValues.put(field.name(), nested);
            terminal.writer().println(ConsoleColors.cyan("  +-- end " + field.name() + " --", terminal));
            terminal.writer().flush();
        }
    }

    /**
     * Handles a single primitive, string, or enum field by dispatching to the correct
     * {@link PromptUtil} method based on the field's {@link FieldInfo#typeName()}.
     *
     * @param field       the simple field descriptor
     * @param promptUtil  the shared readline helper
     * @param fieldValues the map being assembled
     * @throws IOException on terminal I/O failure
     */
    private void handleSimpleField(final FieldInfo field, final PromptUtil promptUtil,
            final Map<String, Object> fieldValues) throws IOException {
        String fieldName = field.name();
        Object rawDefault = field.defaultValue();
        String defaultStr = rawDefault != null ? rawDefault.toString() : null;

        if (field.type().isEnum()) {
            String selected = promptEnumValue(promptUtil, "  " + fieldName, field.type(), defaultStr);
            fieldValues.put(fieldName, selected);
            return;
        }

        switch (field.typeName()) {
            case "int", "Integer", "long", "Long", "short", "Short" -> {
                int intDefault = rawDefault instanceof Number n ? n.intValue() : 0;
                String prompt = "  " + fieldName + " (int) [" + intDefault + "]: ";
                fieldValues.put(fieldName, promptUtil.readInt(prompt, intDefault));
            }
            case "boolean", "Boolean" -> {
                boolean boolDefault = rawDefault instanceof Boolean b ? b : false;
                String prompt = "  " + fieldName + " (bool) [" + boolDefault + "]: ";
                fieldValues.put(fieldName, promptUtil.readBoolean(prompt, boolDefault));
            }
            default -> {
                if (isPromptableAsString(field.type())) {
                    String prompt = buildFieldPrompt(fieldName, defaultStr);
                    String value = promptUtil.readString(prompt, defaultStr);
                    if (value != null) {
                        fieldValues.put(fieldName, value);
                    }
                }
                // Abstract types and interfaces not handled above — omit from the map.
                // Jackson's convertValue will leave these at the builder's default value.
            }
        }
    }

    /**
     * Builds a display prompt of the form {@code "  fieldName [default]: "}.
     *
     * @param fieldName    the field name to show
     * @param defaultValue the default value to show in brackets, or {@code null}
     * @return formatted prompt string ready for a {@link PromptUtil} {@code read*} call
     */
    private String buildFieldPrompt(final String fieldName, @Nullable final String defaultValue) {
        StringBuilder sb = new StringBuilder("  ").append(fieldName);
        if (defaultValue != null) {
            sb.append(" [").append(defaultValue).append(']');
        }
        sb.append(": ");
        return sb.toString();
    }

    /**
     * Bridges the reflection-derived {@code Class<?>} to the generic
     * {@code Class<E extends Enum<E>>} required by {@link PromptUtil#readEnum}.
     *
     * <p>The unchecked cast is safe because {@code rawEnumType} is verified
     * to be an enum class by the caller via {@link Class#isEnum()}.
     *
     * @param promptUtil  the readline helper
     * @param prompt      full prompt string to display
     * @param rawEnumType the enum class (as {@code Class<?>} from reflection)
     * @param defaultStr  the name of the default constant, or {@code null}
     * @return the name of the selected constant
     * @throws IOException on terminal I/O failure
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private String promptEnumValue(final PromptUtil promptUtil, final String prompt,
            final Class<?> rawEnumType, @Nullable final String defaultStr) throws IOException {
        Class<Enum> enumType = (Class<Enum>) rawEnumType;
        Enum defaultVal = null;
        if (defaultStr != null) {
            try {
                defaultVal = Enum.valueOf(enumType, defaultStr);
            } catch (IllegalArgumentException ignored) {
                // Default name does not match any constant — proceed with no default.
            }
        }
        return promptUtil.readEnum(prompt, enumType, defaultVal).name();
    }

    /**
     * Extracts the default sub-object for a nested field from the parent's default instance.
     *
     * <p>Serialises {@code parentDefault} to a JsonNode tree and reads the field by name,
     * then deserialises it to {@code nestedClass}. Returns {@code null} if the parent
     * default is unavailable, the field is absent, or deserialisation fails.
     *
     * @param parentDefault the parent config's default instance, or {@code null}
     * @param fieldName     the Jackson property name of the nested field
     * @param nestedClass   the target type for deserialisation
     * @return a typed nested default instance, or {@code null}
     */
    @Nullable
    private Object extractNestedDefault(final Object parentDefault, final String fieldName,
            final Class<?> nestedClass) {
        if (parentDefault == null) {
            return null;
        }
        try {
            var parentNode = mapper.valueToTree(parentDefault);
            var nestedNode = parentNode.get(fieldName);
            if (nestedNode == null || nestedNode.isNull()) {
                return null;
            }
            return mapper.treeToValue(nestedNode, nestedClass);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Interactively collects key-value pairs for a {@code Map} field.
     *
     * <p>Shows a menu-driven loop where the user can add entries (key then value),
     * delete entries by index number, or finish. The default value (if any) is
     * offered as a one-keystroke shortcut. A tip is shown when the value type is
     * itself a collection or object (to explain JSON syntax).
     */
    private void handleMapField(final FieldInfo field, final PromptUtil promptUtil,
            final Terminal terminal, final Map<String, Object> fieldValues,
            final int depth) throws Exception {
        PrintWriter out = terminal.writer();
        String hint = field.collectionHint() != null ? field.collectionHint() : "Map";
        out.println(ConsoleColors.cyan("  " + field.name() + " (" + hint + ")", terminal));

        // Derive default entries
        @SuppressWarnings("unchecked")
        Map<String, Object> defaultEntries = field.defaultValue() instanceof Map<?, ?> dm
                ? new LinkedHashMap<>((Map<String, Object>) dm)
                : new LinkedHashMap<>();

        // Show type tip for nested values
        printCollectionValueTip(hint, out, terminal);

        if (!defaultEntries.isEmpty()) {
            String useDefault = promptUtil.readString(
                    "  Use defaults and skip? [y]: ", "y");
            if (useDefault == null || "y".equalsIgnoreCase(useDefault)
                    || "yes".equalsIgnoreCase(useDefault)) {
                fieldValues.put(field.name(), defaultEntries);
                return;
            }
        }

        // Seed from defaults so the user can edit rather than re-enter everything
        LinkedHashMap<String, Object> entries = new LinkedHashMap<>(defaultEntries);

        while (true) {
            printMapTable(entries, out, terminal);
            boolean hasEntries = !entries.isEmpty();
            out.println("  Options:");
            out.println("    [1] Add entry");
            if (hasEntries) {
                out.println("    [2] Delete entry by number");
                out.println("    [3] Done (" + entries.size() + " "
                        + (entries.size() == 1 ? "entry" : "entries") + ")");
            } else {
                out.println("    [2] Done (no entries)");
            }
            out.flush();

            String choice = promptUtil.readString("  > ", "");
            if (choice == null || choice.isBlank()) {
                continue;
            }
            int doneOption = hasEntries ? 3 : 2;

            if ("1".equals(choice)) {
                String key = promptUtil.readString("  Key: ", null);
                if (key == null || key.isBlank()) {
                    continue;
                }
                if (field.elementClass() != null) {
                    Object nested = promptNestedElement(field.elementClass(), promptUtil, terminal, depth + 1);
                    if (nested != null) {
                        entries.put(key, nested);
                    }
                } else {
                    String value = promptUtil.readString("  Value: ", "");
                    entries.put(key, decodeCollectionValue(value));
                }
            } else if ("2".equals(choice) && hasEntries) {
                String idxStr = promptUtil.readString("  Delete entry number: ", null);
                deleteMapEntryByIndex(entries, idxStr, out, terminal);
            } else if (String.valueOf(doneOption).equals(choice)) {
                break;
            }
        }

        if (!entries.isEmpty()) {
            fieldValues.put(field.name(), entries);
        }
    }

    /**
     * Interactively collects values for a {@code List} or {@code Set} field.
     *
     * <p>Shows a menu-driven loop where the user can add values, delete values by
     * index number, or finish. JSON arrays/objects are accepted when the element type
     * is complex.
     */
    private void handleCollectionField(final FieldInfo field, final PromptUtil promptUtil,
            final Terminal terminal, final Map<String, Object> fieldValues,
            final int depth) throws Exception {
        PrintWriter out = terminal.writer();
        String hint = field.collectionHint() != null ? field.collectionHint() : field.typeName();
        out.println(ConsoleColors.cyan("  " + field.name() + " (" + hint + ")", terminal));

        // Derive default values
        @SuppressWarnings("unchecked")
        List<Object> defaultValues = field.defaultValue() instanceof List<?> dl
                ? new ArrayList<>((List<Object>) dl)
                : field.defaultValue() instanceof Collection<?> col
                        ? new ArrayList<>((Collection<Object>) col)
                        : new ArrayList<>();

        // Show type tip for complex element types
        printCollectionValueTip(hint, out, terminal);

        if (!defaultValues.isEmpty()) {
            String useDefault = promptUtil.readString(
                    "  Use defaults and skip? [y]: ", "y");
            if (useDefault == null || "y".equalsIgnoreCase(useDefault)
                    || "yes".equalsIgnoreCase(useDefault)) {
                fieldValues.put(field.name(), defaultValues);
                return;
            }
        }

        // Seed from defaults
        List<Object> values = new ArrayList<>(defaultValues);

        while (true) {
            printCollectionTable(values, out, terminal);
            boolean hasValues = !values.isEmpty();
            out.println("  Options:");
            out.println("    [1] Add value");
            if (hasValues) {
                out.println("    [2] Delete value by number");
                out.println("    [3] Done (" + values.size() + " "
                        + (values.size() == 1 ? "value" : "values") + ")");
            } else {
                out.println("    [2] Done (no values)");
            }
            out.flush();

            String choice = promptUtil.readString("  > ", "");
            if (choice == null || choice.isBlank()) {
                continue;
            }
            int doneOption = hasValues ? 3 : 2;

            if ("1".equals(choice)) {
                if (field.elementClass() != null) {
                    Object nested = promptNestedElement(field.elementClass(), promptUtil, terminal, depth + 1);
                    if (nested != null) {
                        values.add(nested);
                    }
                } else {
                    String value = promptUtil.readString("  Value: ", null);
                    if (value == null) {
                        continue;
                    }
                    values.add(decodeCollectionValue(value));
                }
            } else if ("2".equals(choice) && hasValues) {
                String idxStr = promptUtil.readString("  Delete value number: ", null);
                try {
                    int idx = Integer.parseInt(idxStr == null ? "" : idxStr.trim()) - 1;
                    if (idx >= 0 && idx < values.size()) {
                        values.remove(idx);
                    } else {
                        out.println(ConsoleColors.red(
                                "  (invalid number — must be 1.." + values.size() + ")", terminal));
                        out.flush();
                    }
                } catch (NumberFormatException e) {
                    out.println(ConsoleColors.red("  (enter a number)", terminal));
                    out.flush();
                }
            } else if (String.valueOf(doneOption).equals(choice)) {
                break;
            }
        }

        if (!values.isEmpty()) {
            fieldValues.put(field.name(), values);
        }
    }

    /**
     * Opens a recursive sub-form for a single collection element whose type is a buildable
     * nested configuration. Returns the collected field-value map for the element, or
     * {@code null} when the recursion depth is exhausted.
     *
     * @param elementClass the runtime class of the element; must not be null
     * @param promptUtil   the shared readline helper
     * @param terminal     the JLine3 terminal
     * @param depth        current recursion depth (stops at {@value #MAX_RECURSION_DEPTH})
     * @return collected values for the element, or {@code null} when depth exceeded
     */
    @Nullable
    private Map<String, Object> promptNestedElement(final Class<?> elementClass,
            final PromptUtil promptUtil, final Terminal terminal, final int depth) throws Exception {
        PrintWriter out = terminal.writer();
        if (depth >= MAX_RECURSION_DEPTH) {
            out.println(ConsoleColors.yellow(
                    "  (max nesting depth reached — skipping element sub-form)", terminal));
            out.flush();
            return null;
        }
        ConfigClassInfo elementInfo = new ConfigClassInfo(
                elementClass.getName(), elementClass.getSimpleName(), "", "", elementClass, null);
        out.println(ConsoleColors.cyan(
                "  +-- " + elementClass.getSimpleName() + " (element) --", terminal));
        out.flush();
        Map<String, Object> nested = promptInternal(elementInfo, promptUtil, terminal, depth);
        out.println(ConsoleColors.cyan(
                "  +-- end " + elementClass.getSimpleName() + " --", terminal));
        out.flush();
        return nested.isEmpty() ? null : nested;
    }

    /** Prints a numbered table of map entries to the terminal. */
    private static void printMapTable(final Map<String, Object> entries,
            final PrintWriter out, final Terminal terminal) {
        if (entries.isEmpty()) {
            out.println(ConsoleColors.yellow("  (no entries yet)", terminal));
        } else {
            out.println(ConsoleColors.cyan("  Current entries (" + entries.size() + "):", terminal));
            int i = 1;
            for (Map.Entry<String, Object> e : entries.entrySet()) {
                String valStr = encodeForDisplay(e.getValue());
                out.println("    #" + i + "  " + ConsoleColors.bold(e.getKey(), terminal)
                        + "  →  " + valStr);
                i++;
            }
        }
        out.flush();
    }

    /** Prints a numbered list of collection values to the terminal. */
    private static void printCollectionTable(final List<Object> values,
            final PrintWriter out, final Terminal terminal) {
        if (values.isEmpty()) {
            out.println(ConsoleColors.yellow("  (no values yet)", terminal));
        } else {
            out.println(ConsoleColors.cyan("  Current values (" + values.size() + "):", terminal));
            for (int i = 0; i < values.size(); i++) {
                out.println("    #" + (i + 1) + "  " + encodeForDisplay(values.get(i)));
            }
        }
        out.flush();
    }

    /** Prints a syntax tip when the collection's value/element type is complex. */
    private static void printCollectionValueTip(final String hint,
            final PrintWriter out, final Terminal terminal) {
        if (hint == null) {
            return;
        }
        boolean isNestedList = hint.contains("List<") || hint.contains("Set<")
                || hint.contains("Collection<");
        boolean isNestedMap = hint.contains("Map<") && hint.indexOf("Map<") < hint.lastIndexOf("Map<");
        boolean isNestedObject = !hint.matches("(?i)Map<String,\\s*String>")
                && !hint.matches("(?i)List<String>")
                && !hint.matches("(?i)Set<String>")
                && (isNestedList || isNestedMap);

        if (isNestedObject) {
            out.println(ConsoleColors.yellow(
                    "  Tip: value type is complex — enter values as JSON.", terminal));
            if (isNestedList) {
                out.println(ConsoleColors.yellow(
                        "       Example for List/Set value:  [\"READ\",\"WRITE\"]", terminal));
            }
            if (isNestedMap) {
                out.println(ConsoleColors.yellow(
                        "       Example for Map value:       {\"host\":\"localhost\",\"port\":\"9092\"}",
                        terminal));
            }
            out.flush();
        }
    }

    /** Removes a map entry by 1-based index string. */
    private static void deleteMapEntryByIndex(final LinkedHashMap<String, Object> entries,
            final String idxStr, final PrintWriter out, final Terminal terminal) {
        try {
            int idx = Integer.parseInt(idxStr == null ? "" : idxStr.trim()) - 1;
            List<String> keys = new ArrayList<>(entries.keySet());
            if (idx >= 0 && idx < keys.size()) {
                entries.remove(keys.get(idx));
            } else {
                out.println(ConsoleColors.red(
                        "  (invalid number — must be 1.." + keys.size() + ")", terminal));
                out.flush();
            }
        } catch (NumberFormatException e) {
            out.println(ConsoleColors.red("  (enter a number)", terminal));
            out.flush();
        }
    }

    /**
     * Decodes a user-entered string as a JSON value if it looks like JSON, else
     * returns it as a plain string. This allows entering {@code ["READ","WRITE"]}
     * for {@code Map<String, List<String>>} value cells.
     */
    private static Object decodeCollectionValue(final String text) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        if ((t.startsWith("[") && t.endsWith("]"))
                || (t.startsWith("{") && t.endsWith("}"))) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(t, Object.class);
            } catch (JsonProcessingException ignored) {
                // Not valid JSON — treat as plain string
            }
        }
        return text;
    }

    /** Formats a value for display in the table — JSON-encodes non-string types. */
    private static String encodeForDisplay(final Object value) {
        if (value instanceof String s) {
            return s;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return value.toString();
        }
    }

    /**
     * Derives a human-readable generic type hint for Map/List/Set fields.
     * Returns {@code null} for non-collection types.
     */
    @Nullable
    private static String resolveCollectionHint(final JavaType javaType) {
        if (javaType == null) {
            return null;
        }
        Class<?> raw = javaType.getRawClass();
        if (Map.class.isAssignableFrom(raw)) {
            String keyType = simpleTypeName(javaType.getKeyType());
            String valType = simpleTypeName(javaType.getContentType());
            return "Map<" + keyType + ", " + valType + ">";
        }
        if (Set.class.isAssignableFrom(raw)) {
            return "Set<" + simpleTypeName(javaType.getContentType()) + ">";
        }
        if (Collection.class.isAssignableFrom(raw)) {
            return "List<" + simpleTypeName(javaType.getContentType()) + ">";
        }
        return null;
    }

    /**
     * Resolves the nested configuration class for the element type (or map value type) of a
     * collection field. Returns non-null only when the element type qualifies as a buildable
     * nested configuration (same criteria as {@link #resolveNestedClass}).
     *
     * @param javaType the Jackson type of the field; may be {@code null}
     * @return the element's raw class if it is a nested config class, {@code null} otherwise
     */
    @Nullable
    private Class<?> resolveCollectionElementClass(final JavaType javaType) {
        if (javaType == null) {
            return null;
        }
        Class<?> raw = javaType.getRawClass();
        if (!Map.class.isAssignableFrom(raw) && !Collection.class.isAssignableFrom(raw)) {
            return null;
        }
        JavaType contentType = javaType.getContentType();
        if (contentType == null) {
            return null;
        }
        return resolveNestedClass(contentType.getRawClass());
    }

    /** Converts a {@link JavaType} to a short human-readable name, recursing into generics. */
    private static String simpleTypeName(final JavaType javaType) {
        if (javaType == null) {
            return "?";
        }
        Class<?> raw = javaType.getRawClass();
        if (Map.class.isAssignableFrom(raw)) {
            return "Map<" + simpleTypeName(javaType.getKeyType())
                    + ", " + simpleTypeName(javaType.getContentType()) + ">";
        }
        if (Set.class.isAssignableFrom(raw)) {
            return "Set<" + simpleTypeName(javaType.getContentType()) + ">";
        }
        if (Collection.class.isAssignableFrom(raw)) {
            return "List<" + simpleTypeName(javaType.getContentType()) + ">";
        }
        return raw.getSimpleName();
    }

    /** Maximum recursion depth for nested configuration objects. */
    private static final int MAX_RECURSION_DEPTH = 3;

    /**
     * Returns {@code true} when the type can be meaningfully prompted as a plain string.
     *
     * <p>Collection, Map, array, and arbitrary object types are excluded — they must either be
     * handled as nested configs or kept at their default value, not entered as a raw string
     * which would fail Jackson deserialization.
     *
     * @param type the raw class to evaluate
     * @return {@code true} for String/CharSequence types and their subclasses
     */
    private static boolean isPromptableAsString(final Class<?> type) {
        return CharSequence.class.isAssignableFrom(type);
    }

    /**
     * Converts a single Jackson property definition into a {@link FieldInfo}.
     *
     * @param prop            the Jackson property definition
     * @param defaultInstance the default-constructed config instance (may be null)
     * @return populated field metadata
     */
    private FieldInfo toFieldInfo(final BeanPropertyDefinition prop, final Object defaultInstance) {
        JavaType javaType = prop.getPrimaryType();
        Class<?> type = javaType.getRawClass();
        String typeName = type.getSimpleName();
        Object defaultValue = deriveDefaultValue(prop.getName(), defaultInstance);
        boolean required = type.isPrimitive();
        Class<?> nestedClass = resolveNestedClass(type);
        // For abstract/interface declared types (e.g. @JsonTypeInfo polymorphic sampler/scope-manager),
        // the concrete runtime class of the default value is what the UI should load fields for.
        if (nestedClass != null
                && (Modifier.isAbstract(nestedClass.getModifiers()) || nestedClass.isInterface())
                && defaultInstance != null) {
            Object concreteDefault = extractNestedDefault(defaultInstance, prop.getName(), nestedClass);
            if (concreteDefault != null) {
                Class<?> concreteClass = concreteDefault.getClass();
                if (!Modifier.isAbstract(concreteClass.getModifiers())
                        && !concreteClass.isInterface()
                        && concreteClass != nestedClass) {
                    nestedClass = concreteClass;
                }
            }
        }
        String collectionHint = resolveCollectionHint(javaType);
        Class<?> elementClass = resolveCollectionElementClass(javaType);
        return new FieldInfo(prop.getName(), type, typeName, defaultValue, required, nestedClass, collectionHint, elementClass);
    }

    /**
     * Extracts the default value for a named property from the default instance.
     *
     * <p>Serialises {@code defaultInstance} to a {@link com.fasterxml.jackson.databind.JsonNode}
     * tree and reads the field by name. Returns {@code null} when the default instance is
     * {@code null} or when the property value is JSON null.
     *
     * @param propName        the Jackson property name to look up
     * @param defaultInstance a default-constructed config instance, or {@code null}
     * @return Java object (String/Integer/Boolean/etc.) for the default value, or {@code null}
     */
    @Nullable
    private Object deriveDefaultValue(final String propName, final Object defaultInstance) {
        if (defaultInstance == null) {
            return null;
        }
        try {
            var node = mapper.valueToTree(defaultInstance);
            var fieldNode = node.get(propName);
            if (fieldNode == null || fieldNode instanceof NullNode) {
                return null;
            }
            return mapper.treeToValue(fieldNode, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Determines whether the given raw class qualifies as a nested buildable configuration.
     *
     * <p>A class qualifies when any of the following is true:
     * <ul>
     *   <li>it is annotated with {@code @JsonDeserialize(builder=…)}, indicating it uses the
     *       builder deserialization pattern; or</li>
     *   <li>it implements {@link AppbaseConfiguration}; or</li>
     *   <li>it is a non-primitive, non-simple, non-collection/map/array/enum type with at least
     *       one Jackson-deserializable property — catches polymorphic abstract config types
     *       (e.g. {@code @JsonTypeInfo}-annotated sub-config interfaces) that do not carry
     *       the above annotations but are still structurally editable as nested forms.</li>
     * </ul>
     *
     * @param rawClass the raw Java class to inspect
     * @return the class itself when it qualifies as a nested config, {@code null} otherwise
     */
    @Nullable
    private Class<?> resolveNestedClass(final Class<?> rawClass) {
        if (rawClass == null || rawClass.isPrimitive()) {
            return null;
        }
        JsonDeserialize jd = rawClass.getAnnotation(JsonDeserialize.class);
        if (jd != null && jd.builder() != Void.class) {
            return rawClass;
        }
        if (AppbaseConfiguration.class.isAssignableFrom(rawClass)) {
            return rawClass;
        }
        // @JsonTypeInfo marks polymorphic abstract types (e.g. JaegerSamplerConfiguration).
        // The concrete runtime class is resolved later in toFieldInfo via extractNestedDefault.
        if (rawClass.getAnnotation(JsonTypeInfo.class) != null) {
            return rawClass;
        }
        // Fallback: any complex POJO with Jackson properties — catches @JsonTypeInfo sub-configs.
        // Exclude JDK types (java.*, javax.*, sun.*, jdk.*): they are never Appbase config classes
        // and often have Jackson-visible properties that would misclassify them as nested forms
        // (e.g. java.lang.Class has simpleName, name, interface, etc.).
        String pkg = rawClass.getPackageName();
        if (!isSimpleType(rawClass)
                && !rawClass.isEnum()
                && !rawClass.isArray()
                && !Collection.class.isAssignableFrom(rawClass)
                && !Map.class.isAssignableFrom(rawClass)
                && !pkg.startsWith("java.")
                && !pkg.startsWith("javax.")
                && !pkg.startsWith("sun.")
                && !pkg.startsWith("jdk.")) {
            try {
                JavaType jt = mapper.constructType(rawClass);
                List<BeanPropertyDefinition> props =
                        mapper.getDeserializationConfig().introspect(jt).findProperties();
                if (!props.isEmpty()) {
                    return rawClass;
                }
            } catch (Exception ignored) {
                // Cannot introspect — treat as non-nested scalar
            }
        }
        return null;
    }

    /**
     * Returns {@code true} when {@code type} is a scalar value that can be meaningfully
     * displayed and edited as a simple string, number, or boolean input.
     *
     * <p>Excludes complex POJOs, collections, maps, and arrays — those are either
     * nested configs or omitted from the prompt form entirely.
     *
     * @param type the raw class to evaluate
     * @return {@code true} for String/CharSequence, Number subtypes, Boolean, and date/time types
     */
    private static boolean isSimpleType(final Class<?> type) {
        if (CharSequence.class.isAssignableFrom(type)) {
            return true;
        }
        if (Number.class.isAssignableFrom(type)) {
            return true;
        }
        if (type == Boolean.class) {
            return true;
        }
        final String pkg = type.getPackageName();
        return pkg.startsWith("java.time") || "java.net".equals(pkg);
    }
}
