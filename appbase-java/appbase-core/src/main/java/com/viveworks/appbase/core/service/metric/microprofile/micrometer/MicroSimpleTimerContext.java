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

package com.viveworks.appbase.core.service.metric.microprofile.micrometer;

import com.viveworks.appbase.core.util.AppbaseUtil;
import io.micrometer.core.instrument.Timer;
import org.eclipse.microprofile.metrics.SimpleTimer;

/**
 * {@link SimpleTimer.Context} implementation using {@link Timer.Sample}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MicroSimpleTimerContext implements SimpleTimer.Context {
    private final io.micrometer.core.instrument.Timer timer;
    private final io.micrometer.core.instrument.Timer.Sample sample;

    public MicroSimpleTimerContext(final Timer timer, final Timer.Sample sample) {
        AppbaseUtil.notNull(timer, "timer");
        AppbaseUtil.notNull(sample, "sample");

        this.timer = timer;
        this.sample = sample;
    }

    @Override
    public long stop() {
        return sample.stop(timer);
    }

    @Override
    public void close() {
        stop();
    }
}
