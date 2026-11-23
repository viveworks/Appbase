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

package com.viveworks.appbase.core.service.log.filter;

import ch.qos.logback.classic.Level;
import org.slf4j.spi.LocationAwareLogger;

/**
 * Represents a log level.
 *
 * <p>
 * ALL &lt; TRACE &lt; DEBUG &lt; INFO &lt; WARN &lt; ERROR &lt; OFF
 * </p>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum LogLevel {
    OFF(Level.OFF_INT, Level.OFF, -1),

    INFO(Level.INFO_INT, Level.INFO, LocationAwareLogger.INFO_INT),

    DEBUG(Level.DEBUG_INT, Level.DEBUG, LocationAwareLogger.DEBUG_INT),

    WARN(Level.WARN_INT, Level.WARN, LocationAwareLogger.WARN_INT),

    ERROR(Level.ERROR_INT, Level.ERROR, LocationAwareLogger.ERROR_INT),

    TRACE(Level.TRACE_INT, Level.TRACE, LocationAwareLogger.TRACE_INT),

    ALL(Level.ALL_INT, Level.ALL, -1);

    private final int logbackValue;
    private final Level logbackLevel;
    private final int locationAwareLoggerValue;

    LogLevel(final int logbackValue, final Level logbackLevel, final int locationAwareLoggerValue) {
        this.logbackValue = logbackValue;
        this.logbackLevel = logbackLevel;
        this.locationAwareLoggerValue = locationAwareLoggerValue;
    }

    public int getLogbackValue() {
        return logbackValue;
    }

    public Level getLogbackLevel() {
        return logbackLevel;
    }

    public int getLocationAwareLoggerValue() {
        return locationAwareLoggerValue;
    }

    /**
     * Convert <code>logback</code>'s log level to this.
     *
     * @param level <code>logback</code>'s log level
     * @return LogLevel
     */
    public static LogLevel from(final Level level) {
        if (level.levelInt == Level.OFF_INT) {
            return LogLevel.OFF;
        } else if (level.levelInt == Level.INFO_INT) {
            return LogLevel.INFO;
        } else if (level.levelInt == Level.DEBUG_INT) {
            return LogLevel.DEBUG;
        } else if (level.levelInt == Level.WARN_INT) {
            return LogLevel.WARN;
        } else if (level.levelInt == Level.ERROR_INT) {
            return LogLevel.ERROR;
        } else if (level.levelInt == Level.TRACE_INT) {
            return LogLevel.TRACE;
        }

        return LogLevel.ALL;
    }

    @Override
    public String toString() {
        return "LogLevel{" +
                "logbackValue=" + logbackValue +
                ", logbackLevel=" + logbackLevel +
                '}';
    }
}
