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

package com.viveworks.appbase.client.core.clientkey.comparablevalue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.objectpool.ObjectFactory;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a {@code host:port} value that can be used with {@link AppbaseClientKey}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class HostPortValue implements PoolableComparableValue {
    private final AtomicReference<String> hostRef;
    private final AtomicReference<Integer> portRef;

    public HostPortValue() {
        this.hostRef = new AtomicReference<>();
        this.portRef = new AtomicReference<>();
    }

    /**
     * Initialize object state.
     *
     * @param host Host
     */
    public void init(final String host) {
        AppbaseUtil.notNull(host, "host");

        this.hostRef.set(host);
    }

    /**
     * Initialize object state.
     *
     * @param host Host
     * @param port Port
     */
    public void init(final String host, final Integer port) {
        AppbaseUtil.notNull(host, "host");
        AppbaseUtil.notNull(port, "port");

        this.hostRef.set(host);
        this.portRef.set(port);
    }

    @JsonProperty("host")
    public String getHost() {
        return hostRef.get();
    }

    @Nullable
    @JsonProperty("port")
    public Integer getPort() {
        return portRef.get();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return getHost() != null;
    }

    @Override
    @JsonIgnore
    public boolean isClean() {
        return getHost() == null && getPort() == null;
    }

    @Override
    public void clean() {
        this.hostRef.set(null);
        this.portRef.set(null);
    }

    @Override
    public int compareTo(@NotNull final Object object) {
        AppbaseUtil.notNull(object, "object");

        if (!HostPortValue.class.isAssignableFrom(object.getClass())) {
            throw new AppbaseRuntimeException(
                    "Argument " + object.getClass() + " must be of type " + HostPortValue.class.getName());
        }

        final HostPortValue hostPortValue = (HostPortValue) object;
        AppbaseUtil.valid(this, "this");
        AppbaseUtil.valid(hostPortValue, "hostPortValue");

        final int hostCompareTo = getHost().compareTo(hostPortValue.getHost());
        if (hostCompareTo == 0) {
            final Integer port = getPort();
            final Integer port1 = hostPortValue.getPort();
            if (port != null && port1 != null) {
                return port.compareTo(port1);
            }
        }

        return hostCompareTo;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HostPortValue that = (HostPortValue) o;
        return Objects.equals(hostRef.get(), that.hostRef.get()) &&
                Objects.equals(portRef.get(), that.portRef.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostRef.get(), portRef.get());
    }

    @Override
    public String toString() {
        return "HostPortValue{" +
                "host=" + hostRef.get() +
                ", port=" + portRef.get() +
                '}';
    }

    /**
     * {@link ObjectFactory} for {@link HostPortValue}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    public static class Factory implements ObjectFactory<HostPortValue> {
        @Override
        public HostPortValue newObject() {
            return new HostPortValue();
        }
    }
}
