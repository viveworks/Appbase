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

package com.viveworks.appbase.core.jmx;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Thrown by implementations of {@link JmxAppbaseCoreMXBean}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class PermissionDeniedException extends AppbaseRuntimeException {
    private static final long serialVersionUID = 7952485390937450953L;

    private final String environmentVariable;
    private final String property;
    private final boolean defaultValue;
    private final String operationName;

    /**
     * Records the external permission keys and denied operation.
     *
     * @param environmentVariable nonblank environment-variable permission key
     * @param property nonblank system-property permission key
     * @param defaultValue fallback permission when neither key is set
     * @param operationName nonblank name of the rejected operation
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if either key or the operation name is null or blank
     */
    public PermissionDeniedException(
            final String environmentVariable,
            final String property,
            final boolean defaultValue,
            final String operationName)
    {
        AppbaseUtil.notBlank(environmentVariable, "environmentVariable");
        AppbaseUtil.notBlank(property, "property");
        AppbaseUtil.notBlank(operationName, "operationName");

        this.environmentVariable = environmentVariable;
        this.property = property;
        this.defaultValue = defaultValue;
        this.operationName = operationName;
    }

    /**
     * Records the external permission keys and denied operation with a caller-supplied detail message.
     *
     * @param message detail message, which may be null
     * @param environmentVariable nonblank environment-variable permission key
     * @param property nonblank system-property permission key
     * @param defaultValue fallback permission when neither key is set
     * @param operationName nonblank name of the rejected operation
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if either key or the operation name is null or blank
     */
    public PermissionDeniedException(
            final String message,
            final String environmentVariable,
            final String property,
            final boolean defaultValue,
            final String operationName)
    {
        super(message);

        AppbaseUtil.notBlank(environmentVariable, "environmentVariable");
        AppbaseUtil.notBlank(property, "property");
        AppbaseUtil.notBlank(operationName, "operationName");

        this.environmentVariable = environmentVariable;
        this.property = property;
        this.defaultValue = defaultValue;
        this.operationName = operationName;
    }

    /**
     * Records the external permission keys and denied operation with a caller-supplied detail message and an underlying
     * cause.
     *
     * @param message detail message, which may be null
     * @param cause underlying failure, which may be null
     * @param environmentVariable nonblank environment-variable permission key
     * @param property nonblank system-property permission key
     * @param defaultValue fallback permission when neither key is set
     * @param operationName nonblank name of the rejected operation
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if either key or the operation name is null or blank
     */
    public PermissionDeniedException(
            final String message,
            final Throwable cause,
            final String environmentVariable,
            final String property,
            final boolean defaultValue,
            final String operationName)
    {
        super(message, cause);

        AppbaseUtil.notBlank(environmentVariable, "environmentVariable");
        AppbaseUtil.notBlank(property, "property");
        AppbaseUtil.notBlank(operationName, "operationName");

        this.environmentVariable = environmentVariable;
        this.property = property;
        this.defaultValue = defaultValue;
        this.operationName = operationName;
    }

    /**
     * Records the external permission keys and denied operation and an underlying cause.
     *
     * @param cause underlying failure, which may be null
     * @param environmentVariable nonblank environment-variable permission key
     * @param property nonblank system-property permission key
     * @param defaultValue fallback permission when neither key is set
     * @param operationName nonblank name of the rejected operation
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if either key or the operation name is null or blank
     */
    public PermissionDeniedException(
            final Throwable cause,
            final String environmentVariable,
            final String property,
            final boolean defaultValue,
            final String operationName)
    {
        super(cause);

        AppbaseUtil.notBlank(environmentVariable, "environmentVariable");
        AppbaseUtil.notBlank(property, "property");
        AppbaseUtil.notBlank(operationName, "operationName");

        this.environmentVariable = environmentVariable;
        this.property = property;
        this.defaultValue = defaultValue;
        this.operationName = operationName;
    }

    /**
     * Records the external permission keys and denied operation with a caller-supplied detail message and an underlying
     * cause.
     *
     * @param message detail message, which may be null
     * @param cause underlying failure, which may be null
     * @param enableSuppression whether suppressed exceptions can be recorded
     * @param writableStackTrace whether this exception captures a stack trace
     * @param environmentVariable nonblank environment-variable permission key
     * @param property nonblank system-property permission key
     * @param defaultValue fallback permission when neither key is set
     * @param operationName nonblank name of the rejected operation
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if either key or the operation name is null or blank
     */
    public PermissionDeniedException(
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace,
            final String environmentVariable,
            final String property,
            final boolean defaultValue,
            final String operationName)
    {
        super(message, cause, enableSuppression, writableStackTrace);

        AppbaseUtil.notBlank(environmentVariable, "environmentVariable");
        AppbaseUtil.notBlank(property, "property");
        AppbaseUtil.notBlank(operationName, "operationName");

        this.environmentVariable = environmentVariable;
        this.property = property;
        this.defaultValue = defaultValue;
        this.operationName = operationName;
    }

    /**
     * Returns the environment-variable name consulted for the denied operation.
     *
     * @return the environment-variable name consulted for the denied operation
     */
    public String getEnvironmentVariable() {
        return environmentVariable;
    }

    /**
     * Returns the system-property name consulted for the denied operation.
     *
     * @return the system-property name consulted for the denied operation
     */
    public String getProperty() {
        return property;
    }

    /**
     * Returns the permission value used when neither external setting is present.
     *
     * @return the permission value used when neither external setting is present
     */
    public boolean isDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns the JMX operation that failed its permission check.
     *
     * @return the JMX operation that failed its permission check
     */
    public String getOperationName() {
        return operationName;
    }

    @Override
    public String toString() {
        return "PermissionDeniedException{" +
                "environmentVariable='" + environmentVariable + '\'' +
                ", property='" + property + '\'' +
                ", defaultValue=" + defaultValue +
                ", operationName='" + operationName + '\'' +
                "} " + super.toString();
    }
}
