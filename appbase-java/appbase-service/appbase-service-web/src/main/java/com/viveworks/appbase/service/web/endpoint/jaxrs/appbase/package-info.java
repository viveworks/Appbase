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

/**
 * {@link com.viveworks.appbase.core.AppbaseCore} JAX-RS resources.
 *
 * <b>118</b> endpoints / operations:
 * <ul>
 *     <li>{@link com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.ConfigurationJaxrsResource}: 34</li>
 *     <li>{@link com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.CoreJaxrsResource}: 7</li>
 *     <li>{@link com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.ExceptionJaxrsResource}: 23</li>
 *     <li>{@link com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.JmxJaxrsResource}: 9</li>
 *     <li>{@link com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.LogJaxrsResource}: 17</li>
 *     <li>{@link com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.MediatorJaxrsResource}: 7</li>
 *     <li>{@link com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.MetricJaxrsResource}: 8</li>
 *     <li>{@link com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.ServiceRegistryJaxrsResource}: 11</li>
 *     <li>{@link com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.StatusJaxrsResource}: 2</li>
 * </ul>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@OpenAPIDefinition(info = @Info(title = "Appbase Core REST API"))
package com.viveworks.appbase.service.web.endpoint.jaxrs.appbase;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
