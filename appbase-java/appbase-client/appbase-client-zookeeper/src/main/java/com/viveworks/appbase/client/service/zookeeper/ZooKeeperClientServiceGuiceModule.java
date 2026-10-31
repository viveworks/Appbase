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

package com.viveworks.appbase.client.service.zookeeper;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * {@code Guice} module for {@link ZooKeeperClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ZooKeeperClientServiceGuiceModule extends AbstractGenericGuiceModule<ZooKeeperClientService> {
    public ZooKeeperClientServiceGuiceModule() {
        super(ZooKeeperClientService.class, DefaultZooKeeperClientService.class);
    }

    public ZooKeeperClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(ZooKeeperClientService.class, DefaultZooKeeperClientService.class, qualifierBindingAnnotation);
    }

    public ZooKeeperClientServiceGuiceModule(final String qualifierBindingName) {
        super(ZooKeeperClientService.class, DefaultZooKeeperClientService.class, qualifierBindingName);
    }



    public ZooKeeperClientServiceGuiceModule(final ZooKeeperClientService zooKeeperClientService) {
        super(ZooKeeperClientService.class, zooKeeperClientService);
    }

    public ZooKeeperClientServiceGuiceModule(
            final ZooKeeperClientService zooKeeperClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(ZooKeeperClientService.class, zooKeeperClientService, qualifierBindingAnnotation);
    }

    public ZooKeeperClientServiceGuiceModule(
            final ZooKeeperClientService zooKeeperClientService,
            final String qualifierBindingName)
    {
        super(ZooKeeperClientService.class, zooKeeperClientService, qualifierBindingName);
    }



    public ZooKeeperClientServiceGuiceModule(
            final Provider<? extends ZooKeeperClientService> zookeeperClientServiceProvider)
    {
        super(ZooKeeperClientService.class, zookeeperClientServiceProvider);
    }

    public ZooKeeperClientServiceGuiceModule(
            final Provider<? extends ZooKeeperClientService> zookeeperClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(ZooKeeperClientService.class, zookeeperClientServiceProvider, qualifierBindingAnnotation);
    }

    public ZooKeeperClientServiceGuiceModule(
            final Provider<? extends ZooKeeperClientService> zookeeperClientServiceProvider,
            final String qualifierBindingName)
    {
        super(ZooKeeperClientService.class, zookeeperClientServiceProvider, qualifierBindingName);
    }



    static {
        DefaultZooKeeperClientService.registerObjectMapperSubtypes();
    }
}
