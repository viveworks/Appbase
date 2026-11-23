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

package com.viveworks.appbase.core.service.metric.microprofile.micrometer;

import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.Counter;

import java.util.Objects;

/**
 * {@link Counter} implementation using <code>Micrometer</code>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MicroCounter implements Counter {
    private final io.micrometer.core.instrument.Counter micrometerCounter;

    public MicroCounter(final io.micrometer.core.instrument.Counter micrometerCounter) {
        AppbaseUtil.notNull(micrometerCounter, "micrometerCounter");

        this.micrometerCounter = micrometerCounter;
    }

    @Override
    public void inc() {
        micrometerCounter.increment();
    }

    @Override
    public void inc(final long n) {
        micrometerCounter.increment(n);
    }

    @Override
    public long getCount() {
        return (long) micrometerCounter.count();
    }

    public io.micrometer.core.instrument.Counter getMicrometerCounter() {
        return micrometerCounter;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MicroCounter that = (MicroCounter) o;
        return Objects.equals(micrometerCounter, that.micrometerCounter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(micrometerCounter);
    }

    @Override
    public String toString() {
        return "MicroCounter{" +
                "micrometerCounter=" + micrometerCounter +
                '}';
    }
}
