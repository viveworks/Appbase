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

package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response;

import com.viveworks.appbase.core.util.jackson.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Offline check of the error body Elasticsearch returns for a missing index, in the shape Elasticsearch
 * 8.x writes it. The type and reason must survive parsing, or a negative search test cannot assert them.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ErrorResponseBodyTest {

    private static final String MISSING_INDEX_404 =
            "{\"error\":{\"root_cause\":[{\"type\":\"index_not_found_exception\",\"reason\":\"no such index [idx]\","
                    + "\"resource.type\":\"index_or_alias\",\"resource.id\":\"idx\",\"index_uuid\":\"_na_\",\"index\":\"idx\"}],"
                    + "\"type\":\"index_not_found_exception\",\"reason\":\"no such index [idx]\","
                    + "\"resource.type\":\"index_or_alias\",\"resource.id\":\"idx\",\"index_uuid\":\"_na_\",\"index\":\"idx\"},"
                    + "\"status\":404}";

    @Test
    void missingIndexEnvelopeKeepsTypeReasonAndRootCause() throws Exception {
        final ErrorResponseBody body = JsonUtil.getJsonMapper()
                .readerFor(ErrorResponseBody.class)
                .readValue(MISSING_INDEX_404);

        Assertions.assertEquals("index_not_found_exception", body.getType());
        Assertions.assertEquals("no such index [idx]", body.getReason());
        Assertions.assertNotNull(body.getRootCause());
        Assertions.assertEquals("index_not_found_exception", body.getRootCause().getType());
        Assertions.assertEquals("no such index [idx]", body.getRootCause().getReason());
        Assertions.assertEquals(Integer.valueOf(404), body.getStatus());
    }

    @Test
    void flatFormStillParses() throws Exception {
        final ErrorResponseBody body = JsonUtil.getJsonMapper()
                .readerFor(ErrorResponseBody.class)
                .readValue("{\"type\":\"x_exception\",\"reason\":\"flat\"}");

        Assertions.assertEquals("x_exception", body.getType());
        Assertions.assertEquals("flat", body.getReason());
    }
}
