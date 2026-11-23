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
package com.viveworks.appbase.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DummyAppbaseServiceConfiguration1 implements AppbaseConfiguration {
    @NotNull
    private final String hostname;
    @PositiveOrZero
    private final Integer port;

    @JsonCreator
    public DummyAppbaseServiceConfiguration1(
            @JsonProperty("hostname") final String hostname,
            @JsonProperty("port") final Integer port) {
        this.hostname = hostname;
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DummyAppbaseServiceConfiguration1 that = (DummyAppbaseServiceConfiguration1) o;
        return Objects.equals(hostname, that.hostname) &&
                Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, port);
    }

    @Override
    public String toString() {
        return "DummyAppbaseServiceConfiguration1{" +
                "hostname='" + hostname + '\'' +
                ", port=" + port +
                '}';
    }
}
