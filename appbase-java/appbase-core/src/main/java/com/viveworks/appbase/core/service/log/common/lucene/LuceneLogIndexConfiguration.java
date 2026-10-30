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

package com.viveworks.appbase.core.service.log.common.lucene;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.viveworks.appbase.core.util.lucene.LuceneServiceConfiguration;

/**
 * Configuration for {@link LuceneLogIndex}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
public interface LuceneLogIndexConfiguration {
    /**
     * Check if {@link LuceneLogIndex} is enabled.
     *
     * @return Enabled or not
     */
    boolean isEnabled();


    /**
     * Get the {@link LuceneServiceConfiguration} used.
     *
     * @return Configuration
     */
    LuceneServiceConfiguration getLuceneServiceConfiguration();


    /**
     * Get the timestamp {@link org.apache.lucene.document.StoredField} name.
     *
     * @return Timestamp stored field name
     */
    String getTimestampStoredFieldName();


    /**
     * Get the timestamp {@link org.apache.lucene.document.LongPoint} name.
     *
     * @return Timestamp long point name
     */
    String getTimestampLongPointName();


    /**
     * Get the thread name's field name.
     *
     * @return Thread name's field name
     */
    String getThreadNameFieldName();


    /**
     * Get the logger name's field name.
     *
     * @return Logger name's field name
     */
    String getLoggerNameFieldName();


    /**
     * Get the {@link com.viveworks.appbase.core.service.log.filter.LogLevel} index field name.
     *
     * @return Log level's field name
     */
    String getLogLevelFieldName();


    /**
     * Get the formatted message's field name.
     *
     * @return Formatted message's field name
     */
    String getFormattedMessageFieldName();


    /**
     * Get the message's field name.
     *
     * @return Message's field name
     */
    String getMessageFieldName();


    /**
     * Get the {@link Throwable} type name's field name.
     *
     * @return Throwable type name's field name
     */
    String getThrowableTypeNameFieldName();


    /**
     * Get the {@link Throwable} message's field name.
     *
     * @return Throwable message's field name
     */
    String getThrowableMessageFieldName();


    /**
     * Check if {@link Throwable} stack tracer indexing is enabled.
     *
     * @return Enabled or not
     */
    boolean isThrowableStackTraceIndexEnabled();


    /**
     * Get the {@link Throwable} stack tracer field name.
     *
     * @return Throwable stack tracer's field name
     */
    String getThrowableStackTraceFieldName();


    /**
     * Check if indexing {@link org.slf4j.MDC} is enabled.
     *
     * @return Enabled or not
     */
    boolean isMdcIndexEnabled();


    /**
     * Get the {@link org.slf4j.MDC}'s index field name.
     *
     * @param key Property key
     * @return MDC index field name
     */
    String getMdcFieldName(String key);


    /**
     * Get the {@link org.slf4j.MDC} key's index field name prefix.
     *
     * @return MDC key's index field name prefix
     */
    String getMdcFieldNamePrefix();


    /**
     * Get the {@link org.slf4j.MDC} key's index field name suffix.
     *
     * @return MDC key's index field name suffix
     */
    String getMdcFieldNameSuffix();


    /**
     * Check if indexing {@link org.slf4j.Marker} is enabled.
     *
     * @return Enabled or not
     */
    boolean isMarkerIndexEnabled();


    /**
     * Get the {@link org.slf4j.Marker} field name.
     *
     * @return Marker's field name
     */
    String getMarkerFieldName();


    /**
     * Get {@link Throwable} cause's type name field name.
     *
     * @return Throwable cause's type name field name
     */
    String getThrowableCauseTypeNameFieldName();


    /**
     * Get {@link Throwable} cause's message field name.
     *
     * @return Throwable cause's message field name
     */
    String getThrowableCauseMessageFieldName();


    /**
     * Check if {@link Throwable} cause's stack tracer indexing is enabled.
     *
     * @return Enabled or not
     */
    boolean isThrowableCauseStackTraceIndexEnabled();


    /**
     * Get {@link Throwable} cause's stack tracer field name.
     *
     * @return {@link Throwable} cause's stack tracer field name.
     */
    String getThrowableCauseStackTraceFieldName();


    /**
     * Check if caller's stack tracer indexing is enabled.
     *
     * @return Caller's stack tracer indexing is enabled.
     */
    boolean isCallerStackIndexEnabled();


    /**
     * Get the caller's stack tracer's field name.
     *
     * @return Caller's stack tracer's field name.
     */
    String getCallerStackFieldName();
}
