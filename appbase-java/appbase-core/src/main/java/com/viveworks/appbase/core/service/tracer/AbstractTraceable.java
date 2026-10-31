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
package com.viveworks.appbase.core.service.tracer;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractTraceable implements Traceable {
    @Nullable
    protected String traceId;

    public AbstractTraceable() {
    }

    public AbstractTraceable(@Nullable final String traceId) {
        this.traceId = traceId;
    }

    @Nullable
    @Override
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

        final AbstractTraceable that = (AbstractTraceable) o;
        return Objects.equals(traceId, that.traceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId);
    }

    @Override
    public String toString() {
        return "AbstractTraceable{" +
                "traceId='" + traceId + '\'' +
                '}';
    }
}
