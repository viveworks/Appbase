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
package com.viveworks.appbase.service.supportmessage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.LocalDateTime;
import java.util.Objects;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class SupportMessage {
    @ColumnName("messageId")
    private String messageId;

    @ColumnName("email")
    private String email;

    @ColumnName("message")
    private String message;

    @Nullable
    @ColumnName("appbaseUserId")
    private String appbaseUserId;

    @Nullable
    @ColumnName("status")
    private String status;

    @ColumnName("deleted")
    private Boolean deleted;

    @ColumnName("creationTimestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AppbaseServiceConstants.DATE_TIME_SERIALIZATION_FORMAT)
    private LocalDateTime creationTimestamp;

    @ColumnName("lastAccessTimestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AppbaseServiceConstants.DATE_TIME_SERIALIZATION_FORMAT)
    private LocalDateTime lastAccessTimestamp;

    public SupportMessage() {
    }

    @JsonCreator
    public SupportMessage(
            @JsonProperty("messageId")
            final String messageId,
            @JsonProperty("email")
            final String email,
            @JsonProperty("message")
            final String message,
            @Nullable
            @JsonProperty("appbaseUserId")
            final String appbaseUserId,
            @Nullable
            @JsonProperty("status")
            final String status,
            @JsonProperty("deleted")
            final Boolean deleted,
            @JsonProperty("creationTimestamp")
            final LocalDateTime creationTimestamp,
            @JsonProperty("lastAccessTimestamp")
            final LocalDateTime lastAccessTimestamp)
    {
        this.messageId = messageId;
        this.email = email;
        this.message = message;
        this.appbaseUserId = appbaseUserId;
        this.status = status;
        this.deleted = deleted;
        this.creationTimestamp = creationTimestamp;
        this.lastAccessTimestamp = lastAccessTimestamp;
    }

    private SupportMessage(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        messageId = builder.messageId;
        email = builder.email;
        message = builder.message;
        appbaseUserId = builder.appbaseUserId;
        status = builder.status;
        deleted = builder.deleted;
        creationTimestamp = builder.creationTimestamp;
        lastAccessTimestamp = builder.lastAccessTimestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    @Nullable
    public String getAppbaseUserId() {
        return appbaseUserId;
    }

    public void setAppbaseUserId(@Nullable final String appbaseUserId) {
        this.appbaseUserId = appbaseUserId;
    }

    @Nullable
    public String getStatus() {
        return status;
    }

    public void setStatus(@Nullable final String status) {
        this.status = status;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(final LocalDateTime creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public LocalDateTime getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }

    public void setLastAccessTimestamp(final LocalDateTime lastAccessTimestamp) {
        this.lastAccessTimestamp = lastAccessTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SupportMessage that = (SupportMessage) o;
        return Objects.equals(messageId, that.messageId) &&
                Objects.equals(email, that.email) &&
                Objects.equals(message, that.message) &&
                Objects.equals(appbaseUserId, that.appbaseUserId) &&
                Objects.equals(status, that.status) &&
                Objects.equals(deleted, that.deleted) &&
                Objects.equals(creationTimestamp, that.creationTimestamp) &&
                Objects.equals(lastAccessTimestamp, that.lastAccessTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                messageId,
                email,
                message,
                appbaseUserId,
                status,
                deleted,
                creationTimestamp,
                lastAccessTimestamp);
    }

    @Override
    public String toString() {
        return "SupportMessage{" +
                "messageId='" + messageId + '\'' +
                ", email='" + email + '\'' +
                ", message='" + message + '\'' +
                ", appbaseUserId='" + appbaseUserId + '\'' +
                ", status='" + status + '\'' +
                ", deleted=" + deleted +
                ", creationTimestamp=" + creationTimestamp +
                ", lastAccessTimestamp=" + lastAccessTimestamp +
                '}';
    }

    public static final class Builder {
        private String messageId;
        private String email;
        private String message;
        private String appbaseUserId;
        private String status;
        private Boolean deleted;
        private LocalDateTime creationTimestamp;
        private LocalDateTime lastAccessTimestamp;

        private Builder() {
        }

        public Builder messageId(final String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder email(final String email) {
            this.email = email;
            return this;
        }

        public Builder message(final String message) {
            this.message = message;
            return this;
        }

        public Builder appbaseUserId(final String appbaseUserId) {
            this.appbaseUserId = appbaseUserId;
            return this;
        }

        public Builder status(final String status) {
            this.status = status;
            return this;
        }

        public Builder deleted(final Boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public Builder creationTimestamp(final LocalDateTime creationTimestamp) {
            this.creationTimestamp = creationTimestamp;
            return this;
        }

        public Builder lastAccessTimestamp(final LocalDateTime lastAccessTimestamp) {
            this.lastAccessTimestamp = lastAccessTimestamp;
            return this;
        }

        public SupportMessage build() {
            return new SupportMessage(this);
        }
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = SupportMessage.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.supportmessage.SupportMessage";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator<SupportMessage.Valid, SupportMessage> {
        @Override
        public boolean isValid(
                final SupportMessage supportMessage,
                final ConstraintValidatorContext context)
        {
            return AppbaseUtil.isNotNull(supportMessage) &&
                    AppbaseUtil.isNotBlank(supportMessage.getMessageId()) &&
                    AppbaseUtil.isNotBlank(supportMessage.getEmail()) &&
                    AppbaseUtil.isNotBlank(supportMessage.getMessage());
        }
    }
}
