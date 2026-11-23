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

package com.viveworks.appbase.core.util.disruptor.configuration;

import com.lmax.disruptor.dsl.ProducerType;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.WaitStrategyConfiguration;

/**
 * Configuration for {@link com.viveworks.appbase.core.util.disruptor.DisruptorService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface DisruptorServiceConfiguration extends AppbaseConfiguration {
    /**
     * Get {@link WaitStrategyConfiguration}.
     *
     * @return WaitStrategyConfiguration
     */
    WaitStrategyConfiguration getWaitStrategyConfiguration();


    /**
     * Get event handler count.
     *
     * @return Event handler count
     */
    int getEventHandlerCount();


    /**
     * Get ring buffer size.
     *
     * @return Ring buffer size
     */
    int getRingBufferSize();


    /**
     * Get {@link ProducerType}.
     *
     * @return ProducerType
     */
    ProducerType getProducerType();


    /**
     * Get {@link ThreadFactoryConfiguration}.
     *
     * @return ThreadFactoryConfiguration
     */
    ThreadFactoryConfiguration getThreadFactoryConfiguration();
}
