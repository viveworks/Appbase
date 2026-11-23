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

package com.viveworks.appbase.core.service.metric.jmx;

import com.viveworks.appbase.core.service.jmx.AppbaseServiceMXBean;

import javax.management.MBeanException;
import javax.management.MXBean;
import java.util.Set;

/**
 * Represents a {@link MXBean} for {@link com.viveworks.appbase.core.service.metric.MetricService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@MXBean
public interface JmxMetricServiceMXBean extends AppbaseServiceMXBean {
    /**
     * Get registered {@link io.micrometer.core.instrument.MeterRegistry} names.
     *
     * @return Micrometer registry names
     */
    Set<String> getMicrometerRegistryNames();


    /**
     * Unregister {@link io.micrometer.core.instrument.MeterRegistry} by type name.
     *
     * @param meterRegistryTypeName MeterRegistry type name
     * @throws MBeanException if there were any errors
     */
    void unregisterMicrometerRegistry(String meterRegistryTypeName) throws MBeanException;


    /**
     * Clear {@link io.micrometer.core.instrument.MeterRegistry}s.
     */
    void clearMicrometerRegistries();


    /**
     * Return a JSON string with all the {@link org.eclipse.microprofile.metrics.Metric}s in
     * {@link io.micrometer.core.instrument.MeterRegistry}.
     *
     * @return JSON
     * @throws MBeanException if there were any errors
     */
    String serializeMetricRegistryAsJson() throws MBeanException;
}
