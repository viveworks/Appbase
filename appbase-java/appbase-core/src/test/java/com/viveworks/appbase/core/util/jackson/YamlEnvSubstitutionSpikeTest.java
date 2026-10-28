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

package com.viveworks.appbase.core.util.jackson;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

/**
 * Characterization test for P-050 N02 (internal work item 921), question Q1: <b>does the YAML load path interpolate
 * {@code ${VAR}} inside a configuration VALUE?</b>
 *
 * <p>This exists because test-resource YAMLs hard-code {@code localhost} as the integration-test
 * infrastructure endpoint, and the multi-container topology needs that endpoint to become a variable.
 * Counted 2026-08-01: <b>16</b> files whose host-style KEY is literally {@code localhost}, and <b>26</b>
 * containing the string anywhere, both under {@code appbase-java/**}{@code /src/test/resources}.
 *
 * <p>⚠️ An earlier draft of this comment said <b>37</b>. That number came from a real command which
 * counted files matching {@code hostnameOrIp} <i>regardless of its value</i>, plus two literal
 * spellings, across two source trees — so it measured something other than what the sentence claimed.
 * <b>A number produced by a command is not thereby a number that answers your question.</b>
 *
 * <p>Whether routing those files is a two-line change each or needs a new mechanism depends entirely on
 * Q1's answer, so the answer is established <b>by execution</b> rather than by reading the loader.
 *
 * <p><b>It asserts today's behaviour, and it is expected to be INVERTED by N02's implementation.</b> A
 * characterization test is not a statement that the behaviour is desirable — it is a statement that the
 * behaviour is <i>known</i>, so that a change to it cannot pass unnoticed.
 *
 * <p>⚠️ The environment variable is deliberately NOT set by this test. Setting it would prove only that
 * an unset variable stays literal, which is a weaker claim and is not the one being made. The claim is
 * that the loader performs no substitution step at all — {@code readConfigurationNodes} runs
 * {@code readTree} then {@code treeToValue} with nothing in between.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class YamlEnvSubstitutionSpikeTest {
    private static final String FIXTURE = "classpath:///spikeEnvSubstitution.yaml";

    @SuppressWarnings("unchecked")
    private static Map<String, Object> spikeNode() throws IOException {
        final Map<String, Object> nodes = YamlUtil.readConfigurationNodes(FIXTURE);
        return (Map<String, Object>) nodes.get("spikeNode");
    }

    /**
     * Q1, the bare form: a {@code ${VAR}} value survives the load VERBATIM.
     */
    @Test
    void interpolatedValueIsNotSubstituted() throws IOException {
        Assertions.assertEquals(
                "${APPBASE_SPIKE_HOST}",
                spikeNode().get("interpolated"),
                "Q1: the YAML loader performs no ${VAR} substitution — the value arrives verbatim");
    }

    /**
     * Q1, the default-bearing form. Shell-style defaults are the shape an author would reach for first,
     * so it is asserted separately: a mechanism could plausibly support one and not the other.
     */
    @Test
    void interpolatedValueWithDefaultIsNotSubstituted() throws IOException {
        Assertions.assertEquals(
                "${APPBASE_SPIKE_HOST:-fallback}",
                spikeNode().get("withDefault"),
                "Q1: the ${VAR:-default} form is not substituted either");
    }

    /**
     * The control. Without it, a loader that returned {@code null} for every key would pass both
     * assertions above by accident — the fixture would look substituted-and-empty rather than literal.
     */
    @Test
    void plainValueLoadsUnchanged() throws IOException {
        Assertions.assertEquals(
                "localhost",
                spikeNode().get("literal"),
                "control: the fixture really is being read, so the assertions above mean what they say");
    }
}
