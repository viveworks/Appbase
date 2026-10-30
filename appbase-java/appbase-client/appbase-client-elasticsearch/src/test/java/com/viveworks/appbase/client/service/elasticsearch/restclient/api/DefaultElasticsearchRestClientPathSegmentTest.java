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

package com.viveworks.appbase.client.service.elasticsearch.restclient.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * AUD-32 (CWE-116 family): path-segment encoding tests for {@link DefaultElasticsearchRestClient}.
 *
 * <p>{@code deleteDocumentRequest}, {@code createIndexElasticsearchRequest},
 * {@code existsIndexRequest}, {@code deleteIndexRequest} and {@code cancelTaskRequest}
 * raw-concatenated a caller-supplied identifier (index name, document id, task id) straight into
 * the REST request path. A {@code '/'} let one identifier introduce EXTRA path segments; a
 * {@code '?'} let it introduce an unintended query string; traversal-shaped content changed which
 * endpoint the path actually named — while query PARAMETERS on the very same requests were
 * already percent-encoded via {@code appendQueryParameter} (F-06, internal work item 806 R-3). All five call sites
 * now go through {@link DefaultElasticsearchRestClient#encodePathSegment(String)}, tested here
 * directly, matching the sibling {@code DefaultElasticsearchRestClientQueryStringTest}'s own
 * no-live-cluster-needed approach for the production methods it covers.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultElasticsearchRestClientPathSegmentTest {

    @Test
    void encodePathSegment_slash_isEncodedNotLeftAsASeparator() {
        final String encoded = DefaultElasticsearchRestClient.encodePathSegment("a/b");

        assertEquals("a%2Fb", encoded);
        assertFalse(encoded.contains("/"),
                "an unencoded '/' would let the identifier introduce an EXTRA path segment");
    }

    @Test
    void encodePathSegment_questionMark_isEncodedNotLeftAsAQueryStringStart() {
        final String encoded = DefaultElasticsearchRestClient.encodePathSegment("myindex?pretty=true");

        assertEquals("myindex%3Fpretty%3Dtrue", encoded);
        assertFalse(encoded.contains("?"),
                "an unencoded '?' would let the identifier start an UNINTENDED query string");
    }

    @Test
    void encodePathSegment_traversalShapedContent_noLongerNamesADifferentEndpoint() {
        final String encoded = DefaultElasticsearchRestClient.encodePathSegment("../../_cluster/settings");

        assertEquals("..%2F..%2F_cluster%2Fsettings", encoded);
        assertFalse(encoded.contains("/"),
                "with every '/' encoded, the segment can no longer traverse to a different endpoint");
    }

    /**
     * Unlike {@code appendQueryParameter}'s {@link java.net.URLEncoder}-based query-string
     * encoding (which deliberately maps a space to {@code '+'}), a path segment must keep a
     * space as {@code %20} -- {@code '+'} has no special meaning in a URI path, so leaving it as
     * {@code '+'} would silently change a legitimate identifier containing one.
     */
    @Test
    void encodePathSegment_space_becomesPercentTwenty_notPlus() {
        assertEquals("my%20index", DefaultElasticsearchRestClient.encodePathSegment("my index"));
    }

    @Test
    void encodePathSegment_unreservedCharacters_passThroughUnchanged() {
        assertEquals("abc-DEF_123.ghi~",
                DefaultElasticsearchRestClient.encodePathSegment("abc-DEF_123.ghi~"));
    }
}
