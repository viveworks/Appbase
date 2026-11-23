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

package com.viveworks.appbase.configwizard.configurations;

import com.viveworks.appbase.configwizard.configurations.prompt.FieldPrompter;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigClassInfo;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigScanner;
import com.viveworks.appbase.configwizard.configurations.writer.YamlWriter;
import com.viveworks.appbase.configwizard.menu.FilterMenu;
import com.viveworks.appbase.configwizard.menu.UserAbortException;
import com.viveworks.appbase.service.supportmessage.DefaultSupportMessageServiceConfiguration;
import com.viveworks.appbase.service.user.account.DefaultUserAccountServiceConfiguration;
import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConfigurationsConsoleFeature}.
 *
 * <p>Inline mocking ({@code mock-maker-inline}) is required because several dependencies
 * are declared {@code final}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ConfigurationsConsoleFeatureTest {

    @Mock
    private ConfigScanner scanner;

    @Mock
    private FieldPrompter prompter;

    @Mock
    private YamlWriter writer;

    @Mock
    private FilterMenu filterMenu;

    @Mock
    private Terminal terminal;

    @Mock
    private PrintWriter terminalWriter;

    @Mock
    private NonBlockingReader terminalReader;

    /** Shared config-class fixture. */
    private static final ConfigClassInfo DUMMY_INFO = new ConfigClassInfo(
            "com.example.TestConfig", "TestConfig", "core", "test", Object.class, null);

    @BeforeEach
    void setUp() {
        // scanner.scan() is called in the constructor of ConfigurationsConsoleFeature
        when(scanner.scan()).thenReturn(List.of(DUMMY_INFO));
        lenient().when(terminal.writer()).thenReturn(terminalWriter);
    }

    /**
     * Verifies that when {@link FilterMenu#pick} throws {@link UserAbortException} during
     * the Create flow the exception propagates through {@link ConfigurationsConsoleFeature#run}.
     */
    @Test
    void run_create_userAborts_throwsUserAbortException() throws Exception {
        when(terminal.reader()).thenReturn(terminalReader);
        // Sub-menu char '1' selects Create; then FilterMenu.pick() throws
        when(terminalReader.read()).thenReturn((int) '1');
        when(filterMenu.pick(any())).thenThrow(new UserAbortException("User quit"));

        ConfigurationsConsoleFeature feature = new ConfigurationsConsoleFeature(
                scanner, prompter, writer, t -> filterMenu);

        assertThrows(UserAbortException.class,
                () -> feature.run(terminal, Path.of(System.getProperty("java.io.tmpdir"))));
    }

    /**
     * Verifies the full happy-path Create flow: pick → prompt → preview → write → print path.
     * After writing, the user chooses not to create another, and then presses B to go back.
     */
    @Test
    void run_create_successPath_printsWrittenTo(@TempDir final Path tmpDir) throws Exception {
        Path outPath = tmpDir.resolve("out.yaml");
        // Use a serializable map so previewMapper.writeValueAsString() succeeds
        Object built = Map.of("ssl", false);

        when(terminal.reader()).thenReturn(terminalReader);
        // Reads (in order):
        //   1. sub-menu char = '1'  (select Create)
        //   2. askWrite = 'Y'       (confirm write)
        //   3. askContinue = 'n'    (stop creating, exit runCreate loop)
        //   4. sub-menu char = 'B'  (Back, exit run() loop)
        when(terminalReader.read())
                .thenReturn((int) '1', (int) 'Y', (int) 'n', (int) 'B');

        when(filterMenu.pick(any())).thenReturn(DUMMY_INFO);
        when(prompter.prompt(any(), any())).thenReturn(Map.of("ssl", false));
        when(prompter.buildFromMap(any(), any())).thenReturn(built);
        when(writer.write(any(), any())).thenReturn(outPath);

        ConfigurationsConsoleFeature feature = new ConfigurationsConsoleFeature(
                scanner, prompter, writer, t -> filterMenu);
        feature.run(terminal, tmpDir);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(terminalWriter, atLeastOnce()).println(captor.capture());
        String written = captor.getAllValues().stream()
                .filter(s -> s != null && s.contains("Written to:"))
                .findFirst()
                .orElse("");
        assertTrue(written.contains("Written to:"),
                "Expected 'Written to:' in terminal output but got: " + captor.getAllValues());
        assertTrue(written.contains(outPath.toString()),
                "Expected '" + outPath + "' in terminal output but got: " + captor.getAllValues());
    }

    static Stream<Object> shippedPropertylessConfigurations() {
        return Stream.of(
                new DefaultSupportMessageServiceConfiguration(),
                new DefaultUserAccountServiceConfiguration());
    }

    /**
     * Propertyless marker configurations must preview with the same configured YAML semantics
     * used by the real writer. In particular, an empty bean previews as {@code {}} rather than
     * being refused by Jackson's default {@code FAIL_ON_EMPTY_BEANS} behavior.
     */
    @ParameterizedTest
    @MethodSource("shippedPropertylessConfigurations")
    void run_create_propertylessShippedConfiguration_previewsAndWrites(
            final Object built, @TempDir final Path tmpDir) throws Exception {
        Path outPath = tmpDir.resolve(built.getClass().getSimpleName() + ".yaml");
        ConfigClassInfo info = new ConfigClassInfo(
                built.getClass().getName(), built.getClass().getSimpleName(),
                "service", "test", built.getClass(), built);

        when(terminal.reader()).thenReturn(terminalReader);
        when(terminalReader.read())
                .thenReturn((int) '1', (int) 'Y', (int) 'n', (int) 'B');
        when(filterMenu.pick(any())).thenReturn(info);
        when(prompter.prompt(info, terminal)).thenReturn(Map.of());
        when(prompter.buildFromMap(info, Map.of())).thenReturn(built);
        when(writer.write(same(built), any())).thenReturn(outPath);

        ConfigurationsConsoleFeature feature = new ConfigurationsConsoleFeature(
                scanner, prompter, writer, t -> filterMenu);
        feature.run(terminal, tmpDir);

        verify(terminalWriter).print(contains("{}"));
        verify(writer).write(same(built), any());
        ArgumentCaptor<String> lines = ArgumentCaptor.forClass(String.class);
        verify(terminalWriter, atLeastOnce()).println(lines.capture());
        assertFalse(lines.getAllValues().stream()
                .anyMatch(line -> line != null && line.contains("Cannot preview configuration")));
    }
}
