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
package com.viveworks.appbase.stacktest.data.pojo.customer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viveworks.appbase.core.service.tracer.Traceable;
import com.viveworks.appbase.core.util.AppbaseUtil;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Objects;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DownloadCustomerPhotoByCustomerIdResponse implements Traceable {
    @JsonIgnore
    @Nullable
    private final InputStream inputStream;

    @Nullable
    private String fileName;

    @Nullable
    private String mimeType;

    @Nullable
    private String traceId;

    @JsonCreator
    public DownloadCustomerPhotoByCustomerIdResponse(
            @Nullable
            final InputStream inputStream,
            @Nullable
            @JsonProperty("fileName")
            final String fileName,
            @Nullable
            @JsonProperty("mimeType")
            final String mimeType,
            @Nullable
            @JsonProperty("traceId")
            final String traceId)
    {
        AppbaseUtil.notNull(inputStream, "inputStream");

        this.inputStream = inputStream;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.traceId = traceId;
    }

    @Nullable
    public InputStream getInputStream() {
        return inputStream;
    }

    @Nullable
    public String getFileName() {
        return fileName;
    }

    public void setFileName(@Nullable final String fileName) {
        this.fileName = fileName;
    }

    @Nullable
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(@Nullable final String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    @Nullable
    public String getTraceId() {
        return traceId;
    }

    @Override
    public void setTraceId(@Nullable final String traceId) {
        this.traceId = traceId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DownloadCustomerPhotoByCustomerIdResponse that = (DownloadCustomerPhotoByCustomerIdResponse) o;
        return Objects.equals(inputStream, that.inputStream) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(mimeType, that.mimeType) &&
                Objects.equals(traceId, that.traceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputStream, fileName, mimeType, traceId);
    }

    @Override
    public String toString() {
        return "DownloadCustomerPhotoByCustomerIdResponse{" +
                "inputStream=" + inputStream +
                ", fileName='" + fileName + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", traceId='" + traceId + '\'' +
                '}';
    }


    @Target({ METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = DownloadCustomerPhotoByCustomerIdResponse.Validator.class)
    public @interface Valid {
        String message() default "{com.viveworks.appbase.stacktest.data.pojo.customer." +
                "DownloadCustomerPhotoByCustomerIdResponse}";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }


    public static class Validator
            implements ConstraintValidator<
            DownloadCustomerPhotoByCustomerIdResponse.Valid, DownloadCustomerPhotoByCustomerIdResponse>
    {
        @Override
        public boolean isValid(
                final DownloadCustomerPhotoByCustomerIdResponse value,
                final ConstraintValidatorContext context)
        {
            if (value != null) {
                return AppbaseUtil.isNotBlank(value.getFileName()) ||
                        AppbaseUtil.isNotNull(value.getInputStream());
            }

            return true;
        }
    }
}
