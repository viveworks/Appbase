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
package com.viveworks.appbase.stacktest.dummytypes;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DummyMeterRegistry extends MeterRegistry {
    private final SimpleMeterRegistry simpleMeterRegistry;
    public DummyMeterRegistry() {
        this(Clock.SYSTEM);
    }

    public DummyMeterRegistry(final Clock clock) {
        super(clock);

        this.simpleMeterRegistry = new SimpleMeterRegistry();
    }

    @NotNull
    @Override
    protected <T> Gauge newGauge(
            @NotNull
            final Meter.Id id,
            final T obj,
            final ToDoubleFunction<T> valueFunction)
    {
        return Gauge.builder(id.getName(), obj, valueFunction).register(simpleMeterRegistry);
    }

    @NotNull
    @Override
    protected Counter newCounter(@NotNull final Meter.Id id) {
        return Counter.builder(id.getName()).register(simpleMeterRegistry);
    }

    @NotNull
    @Override
    protected LongTaskTimer newLongTaskTimer(@NotNull final Meter.Id id) {
        return LongTaskTimer.builder(id.getName()).register(simpleMeterRegistry);
    }

    @NotNull
    @Override
    protected Timer newTimer(
            @NotNull
            final Meter.Id id,
            @NotNull
            final DistributionStatisticConfig distributionStatisticConfig,
            @NotNull
            final PauseDetector pauseDetector)
    {
        return Timer.builder(id.getName()).register(simpleMeterRegistry);
    }

    @NotNull
    @Override
    protected DistributionSummary newDistributionSummary(
            @NotNull
            final Meter.Id id,
            @NotNull
            final DistributionStatisticConfig distributionStatisticConfig,
            final double scale)
    {
        return DistributionSummary.builder(id.getName()).register(simpleMeterRegistry);
    }

    @NotNull
    @Override
    protected Meter newMeter(
            @NotNull
            final Meter.Id id,
            @NotNull
            final Meter.Type type,
            @NotNull
            final Iterable<Measurement> measurements)
    {
        return Meter.builder(id.getName(), type, measurements).register(simpleMeterRegistry);
    }

    @NotNull
    @Override
    protected <T> FunctionTimer newFunctionTimer(
            @NotNull
            final Meter.Id id,
            @NotNull
            final T obj,
            @NotNull
            final ToLongFunction<T> countFunction,
            @NotNull
            final ToDoubleFunction<T> totalTimeFunction,
            @NotNull
            final TimeUnit totalTimeFunctionUnit)
    {
        return FunctionTimer
                .builder(id.getName(), obj, countFunction, totalTimeFunction, totalTimeFunctionUnit)
                .register(simpleMeterRegistry);
    }

    @NotNull
    @Override
    protected <T> FunctionCounter newFunctionCounter(
            @NotNull
            final Meter.Id id,
            @NotNull
            final T obj,
            @NotNull
            final ToDoubleFunction<T> countFunction)
    {
        return FunctionCounter.builder(id.getName(), obj, countFunction).register(simpleMeterRegistry);
    }

    @NotNull
    @Override
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    @NotNull
    @Override
    protected DistributionStatisticConfig defaultHistogramConfig() {
        return DistributionStatisticConfig.DEFAULT;
    }
}
