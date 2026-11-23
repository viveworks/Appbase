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

package com.viveworks.appbase.client.service.zookeeper.accessor;

import org.apache.curator.framework.CuratorFramework;

/**
 * A checked-exception-throwing command over a {@link CuratorFramework}, used by {@link
 * ZooKeeperAccessor#command(String, ZooKeeperCommand)}. Unlike {@link java.util.function.Function},
 * its {@link #apply(CuratorFramework)} declares {@code throws Exception}, so callers can invoke
 * Curator's blocking fluent API (whose {@code forPath(...)} terminals throw checked {@link
 * Exception}) directly inside the lambda.
 *
 * @param <T> Result type
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@FunctionalInterface
public interface ZooKeeperCommand<T> {

    /**
     * Run the command against {@code client}.
     *
     * @param client ZooKeeper Curator client
     * @return Result of the command
     * @throws Exception If the Curator/ZooKeeper call fails
     */
    T apply(CuratorFramework client) throws Exception;
}
