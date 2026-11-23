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

package com.viveworks.appbase.core.di.guice;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericInstanceGuiceModule;
import com.viveworks.appbase.core.util.handler.AppbaseHandler;
import com.viveworks.appbase.core.util.handler.AppbaseHandlerRequest;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * {@code Guice} module for {@link com.viveworks.appbase.core.util.handler.AppbaseHandler}.
 *
 * @author Navaneeth Mysore G
 * @param <T> Implementation type of {@link AppbaseHandlerRequest}
 * @since 1.0.0
 */
public class AppbaseHandlerGuiceModule<T extends AppbaseHandler<? extends AppbaseHandlerRequest>>
        extends AbstractGenericInstanceGuiceModule<T>
{
    /**
     * Supplies the handler binding as a fixed instance. The key remains unqualified.
     *
     * @param bindingType Guice key type for the handler
     * @param instance handler instance bound to the key
     */
    public AppbaseHandlerGuiceModule(final Class<T> bindingType, final T instance) {
        super(bindingType, instance);
    }

    /**
     * Supplies the handler binding as a fixed instance. The key carries the annotation qualifier.
     *
     * @param bindingType Guice key type for the handler
     * @param instance handler instance bound to the key
     * @param qualifierBindingAnnotation annotation qualifier applied to all bindings
     */
    public AppbaseHandlerGuiceModule(
            final Class<T> bindingType, final T instance, final Annotation qualifierBindingAnnotation)
    {
        super(bindingType, instance, qualifierBindingAnnotation);
    }

    /**
     * Supplies the handler binding as a fixed instance. The key carries the named qualifier.
     *
     * @param bindingType Guice key type for the handler
     * @param instance handler instance bound to the key
     * @param qualifierBindingName name qualifier applied to all bindings
     */
    public AppbaseHandlerGuiceModule(
            final Class<T> bindingType, final T instance, final String qualifierBindingName)
    {
        super(bindingType, instance, qualifierBindingName);
    }



    /**
     * Supplies the handler binding through a provider. The key remains unqualified.
     *
     * @param bindingType Guice key type for the handler
     * @param instanceProvider provider used to obtain the handler
     */
    public AppbaseHandlerGuiceModule(final Class<T> bindingType, final Provider<T> instanceProvider) {
        super(bindingType, instanceProvider);
    }

    /**
     * Supplies the handler binding through a provider. The key carries the annotation qualifier.
     *
     * @param bindingType Guice key type for the handler
     * @param instanceProvider provider used to obtain the handler
     * @param qualifierBindingAnnotation annotation qualifier applied to all bindings
     */
    public AppbaseHandlerGuiceModule(
            final Class<T> bindingType, final Provider<T> instanceProvider, final Annotation qualifierBindingAnnotation)
    {
        super(bindingType, instanceProvider, qualifierBindingAnnotation);
    }

    /**
     * Supplies the handler binding through a provider. The key carries the named qualifier.
     *
     * @param bindingType Guice key type for the handler
     * @param instanceProvider provider used to obtain the handler
     * @param qualifierBindingName name qualifier applied to all bindings
     */
    public AppbaseHandlerGuiceModule(
            final Class<T> bindingType, final Provider<T> instanceProvider, final String qualifierBindingName)
    {
        super(bindingType, instanceProvider, qualifierBindingName);
    }
}
