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

package com.viveworks.appbase.client.service.elasticsearch.restclient.api;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Query-string encoding tests for {@link DefaultElasticsearchRestClient} — internal work item 806 R-3, closing the
 * gap internal work item 802 left when it fixed F-06.
 *
 * <p>⚠️ Deliberately a <strong>separate class</strong> from {@code DefaultElasticsearchRestClientTest},
 * which needs a live Elasticsearch cluster in {@code @BeforeAll} and deletes indices in
 * {@code @AfterAll}. F-06's percent-encoding fix shipped with no regression test because the only
 * home for one carried that dependency — the encoding itself needs no cluster, and this class proves
 * it by running without one.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultElasticsearchRestClientQueryStringTest {

    /**
     * F-06 itself: a value carrying query-string metacharacters must arrive as one parameter.
     *
     * <p>Unencoded, {@code a&refresh=wait_for} would end the {@code q} parameter and start a second
     * one that the caller never asked for — parameter injection into the Elasticsearch request.
     */
    @Test
    void appendQueryParameters_valueContainingSeparators_isEncodedNotInjected() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", "a&refresh=wait_for#frag");

        String result = DefaultElasticsearchRestClient.appendQueryParameters("/index/_search", params);

        assertEquals("/index/_search?q=a%26refresh%3Dwait_for%23frag", result);
        assertFalse(result.contains("&refresh="),
                "an unencoded '&' would inject a second parameter into the request");
        assertFalse(result.contains("#frag"),
                "an unencoded '#' would truncate the request at a fragment marker");
    }

    /**
     * The key is encoded too, not only the value — both halves were named in F-06.
     */
    @Test
    void appendQueryParameters_keyContainingSeparators_isEncoded() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("we&ird=key", "value");

        String result = DefaultElasticsearchRestClient.appendQueryParameters("/index", params);

        assertEquals("/index?we%26ird%3Dkey=value", result);
    }

    @Test
    void appendQueryParameters_multipleParameters_areAmpersandSeparatedWithNoTrailingSeparator() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("one", "1");
        params.put("two", "2");

        String result = DefaultElasticsearchRestClient.appendQueryParameters("/index", params);

        assertEquals("/index?one=1&two=2", result);
        assertFalse(result.endsWith("&"), "the trailing separator must be removed");
    }

    /**
     * {@code URLEncoder} implements {@code application/x-www-form-urlencoded}, which maps a
     * space to {@code +}. That is correct in a query string and is what Elasticsearch expects — the
     * javadoc on the production method says so, and this pins it.
     */
    @Test
    void appendQueryParameters_spaceInValue_becomesPlus() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", "hello world");

        assertEquals("/index?q=hello+world",
                DefaultElasticsearchRestClient.appendQueryParameters("/index", params));
    }

    @Test
    void appendQueryParameters_nullOrEmptyMap_returnsPathUnchanged() {
        assertEquals("/index", DefaultElasticsearchRestClient.appendQueryParameters("/index", null));
        assertEquals("/index", DefaultElasticsearchRestClient.appendQueryParameters("/index", Map.of()));
    }

    /**
     * Documents the existing behaviour rather than asserting it is desirable: a null value is
     * encoded as the literal string {@code "null"}, via {@code String.valueOf}.
     */
    @Test
    void appendQueryParameter_nullValue_isEncodedAsTheLiteralNull() {
        StringBuilder sb = new StringBuilder();
        DefaultElasticsearchRestClient.appendQueryParameter("k", null, sb);
        assertTrue(sb.toString().startsWith("k=null"), "actual: " + sb);
    }
}
