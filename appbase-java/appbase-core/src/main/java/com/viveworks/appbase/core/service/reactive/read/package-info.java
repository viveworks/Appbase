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
 * Contains a bounded reactive read boundary that bridges blocking reads (e.g. Lucene file I/O) onto a
 * {@link reactor.core.publisher.Mono}, offloaded to a {@link reactor.core.scheduler.Scheduler} resolved via
 * {@link com.viveworks.appbase.core.service.reactive.reactor.ReactorService} and bounded by a deadline and
 * cooperative cancellation.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
package com.viveworks.appbase.core.service.reactive.read;
