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

package com.viveworks.appbase.core.service.log.filter.burst;

import com.codahale.metrics.Meter;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Default implementation of {@link LogRateCalculator}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class DefaultLogRateCalculator implements LogRateCalculator {
    private final EwmaRate ewmaRate;
    private final Meter rateMeter;

    public DefaultLogRateCalculator(final DefaultLogRateCalculatorConfiguration defaultLogRateCalculatorConfiguration) {
        AppbaseUtil.notNull(defaultLogRateCalculatorConfiguration, "ewmaRate");

        DefaultLogRateCalculatorConfigurationValidator.validate(defaultLogRateCalculatorConfiguration.getEwmaRate());

        this.ewmaRate = defaultLogRateCalculatorConfiguration.getEwmaRate();
        this.rateMeter = new Meter();
    }

    @Override
    public double measure(final TurboLog turboLog) {
        rateMeter.mark();

        switch (ewmaRate) {
            case FIFTEEN_MINUTES:
                return rateMeter.getFifteenMinuteRate();

            case FIVE_MINUTES:
                return rateMeter.getFiveMinuteRate();

            default:
            case ONE_MINUTE:
                return rateMeter.getOneMinuteRate();
        }
    }
}
