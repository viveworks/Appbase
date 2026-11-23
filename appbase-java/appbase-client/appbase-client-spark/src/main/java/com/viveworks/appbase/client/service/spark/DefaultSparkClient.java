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

package com.viveworks.appbase.client.service.spark;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.streaming.StreamingContext;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

/**
 * Default implementation of {@link SparkClient}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class DefaultSparkClient implements SparkClient {
    private final SparkContext sparkContext;
    private final JavaSparkContext javaSparkContext;
    private final SparkSession sparkSession;
    private final StreamingContext streamingContext;
    private final JavaStreamingContext javaStreamingContext;

    private DefaultSparkClient(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.sparkContext, "builder.sparkContext");
        AppbaseUtil.notNull(builder.javaSparkContext, "builder.javaSparkContext");
        AppbaseUtil.notNull(builder.sparkSession, "builder.sparkSession");
        AppbaseUtil.notNull(builder.streamingContext, "builder.streamingContext");
        AppbaseUtil.notNull(builder.javaStreamingContext, "builder.javaStreamingContext");

        sparkContext = builder.sparkContext;
        javaSparkContext = builder.javaSparkContext;
        sparkSession = builder.sparkSession;
        streamingContext = builder.streamingContext;
        javaStreamingContext = builder.javaStreamingContext;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SparkContext getSparkContext() {
        return sparkContext;
    }

    @Override
    public JavaSparkContext getJavaSparkContext() {
        return javaSparkContext;
    }

    @Override
    public SparkSession getSparkSession() {
        return sparkSession;
    }

    @Override
    public StreamingContext getStreamingContext() {
        return streamingContext;
    }

    @Override
    public JavaStreamingContext getJavaStreamingContext() {
        return javaStreamingContext;
    }

    @Override
    public String toString() {
        return "DefaultSparkClient{" +
                "sparkContext=" + sparkContext +
                ", javaSparkContext=" + javaSparkContext +
                ", sparkSession=" + sparkSession +
                ", streamingContext=" + streamingContext +
                ", javaStreamingContext=" + javaStreamingContext +
                '}';
    }

    /**
     * Builder for {@link DefaultSparkClient}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private SparkContext sparkContext;
        private JavaSparkContext javaSparkContext;
        private SparkSession sparkSession;
        private StreamingContext streamingContext;
        private JavaStreamingContext javaStreamingContext;

        private Builder() {
        }

        public Builder sparkContext(final SparkContext sparkContext) {
            this.sparkContext = sparkContext;
            return this;
        }

        public Builder javaSparkContext(final JavaSparkContext javaSparkContext) {
            this.javaSparkContext = javaSparkContext;
            return this;
        }

        public Builder sparkSession(final SparkSession sparkSession) {
            this.sparkSession = sparkSession;
            return this;
        }

        public Builder streamingContext(final StreamingContext streamingContext) {
            this.streamingContext = streamingContext;
            return this;
        }

        public Builder javaStreamingContext(final JavaStreamingContext javaStreamingContext) {
            this.javaStreamingContext = javaStreamingContext;
            return this;
        }

        public DefaultSparkClient build() {
            return new DefaultSparkClient(this);
        }
    }
    // CheckStyle: Builder end
}
