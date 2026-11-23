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

package com.viveworks.appbase.core.util;

import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfigurationValidator;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.service.validation.ValidationServiceException;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import com.viveworks.appbase.core.util.objectpool.ObjectFactory;
import com.viveworks.appbase.core.util.objectpool.Poolable;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPool;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPoolConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import net.openhft.affinity.AffinityStrategies;
import net.openhft.affinity.AffinityThreadFactory;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Generic utility methods.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressWarnings("WeakerAccess")
public final class AppbaseUtil {
    public static final long DEFAULT_EXECUTOR_SHUTDOWN_SECONDS = 1;
    public static final Object AUTO_DETECTED_CONFIGURATION_VALUE = null;
    public static final String CONFIGURATION_STORE_ID_SEPARATOR = "+";

    public static final String OBFUSCATE = "********";
    public static final boolean TRUE = true;
    public static final boolean FALSE = false;
    public static final Object NULL = null;

    public static final String PERIOD = ".";
    public static final String COMMA = ",";
    public static final String COMMA_SPACE = COMMA + " ";
    public static final int COMMA_LENGTH = COMMA.length();
    public static final int COMMA_SPACE_LENGTH = COMMA_SPACE.length();

    public static final String COLON = ":";
    public static final String FORWARD_SLASH = "/";
    public static final String QUESTION_MARK = "?";
    public static final String EQUALS = "=";
    public static final String AMPERSAND = "&";
    public static final String AT = "@";
    public static final int ONE = 1;

    private AppbaseUtil() {
    }


    /**
     * Assert that an object is not null.
     *
     * @param object Object to check
     * @param argumentName Argument name for the exception message
     * @param <T> Type of the object
     * @return The input object
     * @throws AppbaseRuntimeException if the object is null
     */
    @SuppressFBWarnings("DRE_DECLARED_RUNTIME_EXCEPTION")
    public static <T> T notNull(final T object, final String argumentName) throws AppbaseRuntimeException {
        if (object == null) {
            final String suffix = " must not be null";
            final String msg = (argumentName == null) ? ("argument" + suffix) : (argumentName + suffix);
            throw new AppbaseRuntimeException(msg);
        }

        return object;
    }


    /**
     * Assert that an array is not empty.
     *
     * @param array Array to check
     * @param argumentName Argument name for the exception message
     * @param <T> Type of the array
     * @return The input array
     * @throws AppbaseRuntimeException if the array is empty
     */
    @SuppressFBWarnings("DRE_DECLARED_RUNTIME_EXCEPTION")
    public static <T> T[] notEmpty(final T[] array, final String argumentName) throws AppbaseRuntimeException {
        notNull(array, argumentName);

        if (array.length == 0) {
            final String suffix = " must not be empty";
            final String msg = (argumentName == null) ? ("argument" + suffix) : (argumentName + suffix);
            throw new AppbaseRuntimeException(msg);
        }

        return array;
    }


    /**
     * Assert that a collection is not empty.
     *
     * @param collection Collection to check
     * @param argumentName Argument name for the exception message
     * @param <T> Type of the collection
     * @return The input collection
     * @throws AppbaseRuntimeException if the collection is empty
     */
    @SuppressFBWarnings("DRE_DECLARED_RUNTIME_EXCEPTION")
    public static <T> Collection<T> notEmpty(final Collection<T> collection,
                                             final String argumentName) throws AppbaseRuntimeException
    {
        notNull(collection, argumentName);

        if (collection.isEmpty()) {
            final String suffix = " must not be empty";
            final String msg = (argumentName == null) ? ("argument" + suffix) : (argumentName + suffix);
            throw new AppbaseRuntimeException(msg);
        }

        return collection;
    }


    /**
     * Assert that a map is not empty.
     *
     * @param map Map to check
     * @param argumentName Argument name for the exception message
     * @param <K> Map's key type
     * @param <V> Map's value type
     * @return The input map
     * @throws AppbaseRuntimeException if the map is empty
     */
    @SuppressFBWarnings("DRE_DECLARED_RUNTIME_EXCEPTION")
    public static <K, V> Map<K, V> notEmpty(final Map<K, V> map,
                                            final String argumentName) throws AppbaseRuntimeException
    {
        notNull(map, argumentName);

        if (map.isEmpty()) {
            final String suffix = " must not be empty";
            final String msg = (argumentName == null) ? ("argument" + suffix) : (argumentName + suffix);
            throw new AppbaseRuntimeException(msg);
        }

        return map;
    }


    /**
     * Assert that an argument is true.
     *
     * @param value Value to check
     * @param argumentName Name of the argument
     */
    public static void notFalse(final boolean value, final String argumentName) {
        if (!value) {
            final String suffix = " must not be empty";
            final String msg = (argumentName == null) ? ("argument" + suffix) : (argumentName + suffix);
            throw new AppbaseRuntimeException(msg);
        }
    }


    /**
     * Check if a character sequence is blank. A character sequence is considered blank if it is null, has zero length
     * or contains only whitespaces.
     *
     * @param charSequence Character sequence to check
     * @return true if blank, false otherwise
     */
    public static boolean isBlank(final CharSequence charSequence) {
        if (charSequence == null) {
            return true;
        }

        final int strLen = charSequence.length();
        if (strLen == 0) {
            return true;
        }

        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(charSequence.charAt(i))) {
                return false;
            }
        }

        return true;
    }


    /**
     * Check if the argument is not blank.
     *
     * @param charSequence Character sequence to check
     * @return true if not blank, false otherwise
     */
    public static boolean isNotBlank(final CharSequence charSequence) {
        return !isBlank(charSequence);
    }


    /**
     * Check if the number is positive or 0.
     *
     * @param number Number to check
     * @return true if greater than or equal to 0, false otherwise
     */
    public static boolean isPositive(final Number number) {
        if (number == null) {
            return false;
        }

        return number.intValue() > 0;
    }


    /**
     * Check if the number is positive or 0.
     *
     * @param number Number to check
     * @return true if greater than or equal to 0, false otherwise
     */
    public static boolean isPositiveOrZero(final Number number) {
        if (number == null) {
            return false;
        }

        return number.intValue() >= 0;
    }


    /**
     * Check if an object is not null.
     *
     * @param object Object to check
     * @return true if not null, false otherwise
     */
    public static boolean isNotNull(final Object object) {
        return object != null;
    }


    /**
     * Check if an object is null.
     *
     * @param object Object to check
     * @return true if null, false otherwise
     */
    public static boolean isNull(final Object object) {
        return object == null;
    }


    /**
     * Check if an object is valid by calling the {@link ConstraintValidator}.
     *
     * @param object Object to validate
     * @param validator {@link ConstraintValidator}
     * @param context {@link ConstraintValidatorContext}
     * @param <T> Type of the object
     * @return true if valid, false otherwise
     */
    public static <T> boolean isValid(
            final T object,
            final ConstraintValidator<?, T> validator,
            final ConstraintValidatorContext context)
    {
        if (object == null) {
            return false;
        }

        return validator.isValid(object, context);
    }


    /**
     * If the object is not null, check if an object is valid by calling the {@link ConstraintValidator}.
     *
     * @param object Object to validate or null
     * @param validator {@link ConstraintValidator}
     * @param context {@link ConstraintValidatorContext}
     * @param <T> Type of the object
     * @return true if null or if not null and valid, false otherwise
     */
    public static <T> boolean isValidIfNotNull(
            final T object,
            final ConstraintValidator<?, T> validator,
            final ConstraintValidatorContext context)
    {
        if (object == null) {
            return true;
        }

        return isValid(object, validator, context);
    }


    /**
     * Assert that a character sequence is not blank. A character sequence is considered blank if it is null, has zero
     * length or contains only whitespaces.
     *
     * @param charSequence The character sequence to check
     * @param argumentName Argument name for the exception message
     * @return The input character sequence
     * @throws AppbaseRuntimeException if the character sequence is blank
     */
    @SuppressFBWarnings("DRE_DECLARED_RUNTIME_EXCEPTION")
    public static CharSequence notBlank(final CharSequence charSequence,
                                        final String argumentName) throws AppbaseRuntimeException
    {
        if (isBlank(charSequence)) {
            final String suffix = " must not be blank";
            final String msg = (argumentName == null) ? ("argument" + suffix) : (argumentName + suffix);
            throw new AppbaseRuntimeException(msg);
        }

        return charSequence;
    }


    /**
     * ´Assert that a number is positive.
     *
     * @param number The number to check
     * @param argumentName Argument name for the exception message
     * @throws AppbaseRuntimeException if the number is not positive
     */
    @SuppressFBWarnings("DRE_DECLARED_RUNTIME_EXCEPTION")
    public static void positive(final int number, final String argumentName) throws AppbaseRuntimeException {
        positive((long) number, argumentName);
    }


    /**
     * ´Assert that a number is positive.
     *
     * @param number The number to check
     * @param argumentName Argument name for the exception message
     * @throws AppbaseRuntimeException if the number is not positive
     */
    @SuppressFBWarnings("DRE_DECLARED_RUNTIME_EXCEPTION")
    public static void positive(final long number, final String argumentName) throws AppbaseRuntimeException {
        if (number < 1) {
            final String suffix = " must be positive";
            final String msg = (argumentName == null) ? ("argument" + suffix) : (argumentName + suffix);
            throw new AppbaseRuntimeException(msg);
        }
    }


    /**
     * ´Assert that a number is positive.
     *
     * @param number The number to check
     * @param argumentName Argument name for the exception message
     * @throws AppbaseRuntimeException if the number is not positive
     */
    @SuppressFBWarnings("DRE_DECLARED_RUNTIME_EXCEPTION")
    public static void positive(final double number, final String argumentName) throws AppbaseRuntimeException {
        if (number < 1) {
            final String suffix = " must be positive";
            final String msg = (argumentName == null) ? ("argument" + suffix) : (argumentName + suffix);
            throw new AppbaseRuntimeException(msg);
        }
    }


    /**
     * Assert that a number is positive or zero.
     *
     * @param number The number to check
     * @param argumentName Argument name for the exception message
     * @throws AppbaseRuntimeException if the number is not positive or 0
     */
    @SuppressFBWarnings("DRE_DECLARED_RUNTIME_EXCEPTION")
    public static void positiveOrZero(final long number, final String argumentName) throws AppbaseRuntimeException {
        if (number < 0) {
            final String suffix = " must be positive or 0";
            final String msg = (argumentName == null) ? ("argument" + suffix) : (argumentName + suffix);
            throw new AppbaseRuntimeException(msg);
        }
    }


    /**
     * Assert that a number is positive or zero.
     *
     * @param number The number to check
     * @param argumentName Argument name for the exception message
     * @throws AppbaseRuntimeException if the number is not positive or 0
     */
    @SuppressFBWarnings("DRE_DECLARED_RUNTIME_EXCEPTION")
    public static void positiveOrZero(final double number, final String argumentName) throws AppbaseRuntimeException {
        if (number < 0) {
            final String suffix = " must be positive or 0";
            final String msg = (argumentName == null) ? ("argument" + suffix) : (argumentName + suffix);
            throw new AppbaseRuntimeException(msg);
        }
    }


    /**
     * Check if a {@link Poolable} is valid by calling {@link Poolable#isValid()}.
     *
     * @param poolable {@link Poolable}
     * @param argumentName Argument name for the exception message
     * @throws AppbaseRuntimeException if invalid
     */
    public static void valid(final Poolable poolable, final String argumentName) throws AppbaseRuntimeException {
        AppbaseUtil.notNull(poolable, "poolable");

        if (!poolable.isValid()) {
            final String suffix = " must be valid";
            final String msg = (argumentName == null) ? ("argument" + suffix) : (argumentName + suffix);
            throw new AppbaseRuntimeException(msg);
        }
    }


    /**
     * Return a new default {@link java.lang.Thread.UncaughtExceptionHandler}.
     *
     * @param logger Logger to use
     * @return Uncaught exception handler
     */
    public static Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler(final Logger logger) {
        return (t, e) ->
                logger.error("Uncaught exception [threadGroup={}, thread={}]",
                        t.getThreadGroup().getName(), t.getName(), e);
    }


    /**
     * Return a new default {@link java.lang.Thread.UncaughtExceptionHandler}.
     *
     * @param loggerRef Atomic reference to the logger to use
     * @return Uncaught exception handler
     */
    public static Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler(
            final AtomicReference<Logger> loggerRef)
    {
        return (t, e) -> {
            final Logger logger = loggerRef.get();
            if (logger != null) {
                logger.error("Uncaught exception [threadGroup={}, thread={}]",
                        t.getThreadGroup().getName(), t.getName(), e);
            }
        };
    }


    /**
     * Create a new thread factory using the name format argument. For example, if the <code>nameFormat</code> is
     * <code>client-thread-%d</code>, each thread created will have <code>%d</code> replaced with a counter
     * increment, i.e client-thread-1, client-thread-2 and so forth.
     *
     * @param threadFactoryConfiguration Thread factory configuration
     * @param uncaughtExceptionHandler The handler for uncaught exceptions
     * @return Thread factory
     */
    public static ThreadFactory newThreadFactory(final ThreadFactoryConfiguration threadFactoryConfiguration,
                                                 final Thread.UncaughtExceptionHandler uncaughtExceptionHandler)
    {
        notNull(threadFactoryConfiguration, "threadFactoryConfiguration");
        notNull(uncaughtExceptionHandler, "uncaughtExceptionHandler");
        ThreadFactoryConfigurationValidator.validate(threadFactoryConfiguration.getNameFormat());

        return new ThreadFactory() {
            private final AtomicLong counter = new AtomicLong(0);
            private final ThreadFactory backingThreadFactory =
                    threadFactoryConfiguration.isEnableThreadAffinity() ?
                            new AffinityThreadFactory(
                                    affinityThreadName(threadFactoryConfiguration.getNameFormat()),
                                    threadFactoryConfiguration.isDaemon(),
                                    AffinityStrategies.SAME_CORE) :
                            Executors.defaultThreadFactory();

            @Override
            @SuppressFBWarnings("FORMAT_STRING_MANIPULATION")
            public Thread newThread(@Nonnull final Runnable runnable) {
                notNull(runnable, "runnable");

                final Thread thread = backingThreadFactory.newThread(runnable);
                if (!threadFactoryConfiguration.isEnableThreadAffinity()) {
                    final String format = String.format(
                            Locale.ROOT,
                            threadFactoryConfiguration.getNameFormat(),
                            counter.getAndIncrement());
                    thread.setName(format);
                    thread.setDaemon(threadFactoryConfiguration.isDaemon());
                }
                thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);

                return thread;
            }
        };
    }

    private static String affinityThreadName(final String nameFormat) {
        final int index = nameFormat.lastIndexOf("-%d");
        return index > -1 ? nameFormat.substring(0, index) : nameFormat;
    }


    /**
     * Shutdown an executor service using default seconds ({@link #DEFAULT_EXECUTOR_SHUTDOWN_SECONDS}).
     *
     * @param executorService The executor service to shutdown
     */
    public static void shutdownExecutor(final ExecutorService executorService) {
        shutdownExecutor(executorService, DEFAULT_EXECUTOR_SHUTDOWN_SECONDS, TimeUnit.SECONDS);
    }


    /**
     * Shutdown an executor service.
     *
     * @param executorService The executor service to shutdown
     * @param timeout Timeout to wait
     * @param timeUnit Time unit of the timeout parameter
     */
    @SuppressFBWarnings("OPM_OVERLY_PERMISSIVE_METHOD")
    public static void shutdownExecutor(final ExecutorService executorService,
                                        final long timeout,
                                        final TimeUnit timeUnit)
    {
        notNull(executorService, "executorService");
        positiveOrZero(timeout, "timeout");
        notNull(timeUnit, "timeUnit");

        if (!executorService.isShutdown() && !executorService.isTerminated()) {
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(timeout, timeUnit)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            if (!executorService.isShutdown()) {
                executorService.shutdownNow();
            }
        }
    }


    /**
     * Get the system property or environment variable, if set.
     *
     * @param propertyName Property name; {@code null} is treated as absent
     * @param envName Environment variable name; {@code null} is treated as absent
     * @return A property or environment variable if they exist, null otherwise
     */
    @Nullable
    public static String propertyOrEnv(@Nullable final String propertyName, @Nullable final String envName) {
        final String property = propertyName != null ? System.getProperty(propertyName) : null;
        return isBlank(property) ? (envName != null ? System.getenv(envName) : null) : property;
    }

    /**
     * Get the current thread name.
     *
     * @return Current thread name
     */
    public static String threadName() {
        return Thread.currentThread().getName();
    }

    /**
     * Converts comma-separated values to a set.
     *
     * @param csv Comma separated values
     * @return Set of values
     */
    @Unmodifiable
    public static Set<String> csvToSet(final String csv) {
        final Set<String> set = new HashSet<>();
        csvToSet(csv, set);

        return Collections.unmodifiableSet(set);
    }

    /**
     * Converts comma-separated values to a set.
     *
     * @param csv Comma separated values
     * @param set Set to add the values into
     */
    public static void csvToSet(final String csv, final Set<String> set) {
        if (csv != null) {
            final String[] csvArray = csv.trim().split(COMMA);
            if (csvArray.length > 0) {
                for (final String value : csvArray) {
                    final String trimmedValue = value.trim();
                    if (!trimmedValue.isBlank()) {
                        set.add(trimmedValue);
                    }
                }
            }
        }
    }

    /**
     * Converts set to comma-separated values.
     *
     * @param set Set
     * @param spaceAfterComma If a space needs to be added after comma
     * @return Comma-separated values
     */
    public static String setToCsv(final Set<String> set, final boolean spaceAfterComma) {
        final StringBuilder csvBuilder = new StringBuilder();

        if (set != null) {
            set.forEach(s -> {
                if (s != null) {
                    csvBuilder.append(s.trim());
                    if (spaceAfterComma) {
                        csvBuilder.append(COMMA_SPACE);
                    } else {
                        csvBuilder.append(COMMA);
                    }
                }
            });

            final int length = csvBuilder.length();
            if (length > 0) {
                if (spaceAfterComma) {
                    csvBuilder.delete(length - COMMA_SPACE_LENGTH, length);
                } else {
                    csvBuilder.delete(length - COMMA_LENGTH, length);
                }
            }
        }

        return csvBuilder.toString();
    }

    /**
     * Validate an object using JSR-303 validator.
     *
     * @param validationService Validation service to retrieve the validator
     * @param object Object to be validated.
     * @param groups Constraint group
     */
    @SuppressFBWarnings("DRE_DECLARED_RUNTIME_EXCEPTION")
    public static void validate(
            final ValidationService validationService,
            final Object object,
            final Class<?>... groups)
    {
        AppbaseUtil.notNull(object, "object");

        final Set<ConstraintViolation<Object>> constraintViolations =
                validationService.getValidator().validate(object, groups);
        if (constraintViolations != null && !constraintViolations.isEmpty()) {
            final ValidationServiceException validationServiceException =
                    new ValidationServiceException(object, constraintViolations);
            throw new AppbaseRuntimeException("Invalid object", validationServiceException);
        }
    }

    public static <T> void validate(final ConstraintValidator<?, T> validator, final T object) {
        AppbaseUtil.notNull(validator, "validator");
        AppbaseUtil.notNull(object, "object");

        if (!validator.isValid(object, null)) {
            throw new AppbaseRuntimeException("Invalid object " + object.getClass().getName());
        }
    }

    public static void checkedValidation(
            final ValidationService validationService,
            final Object object,
            final Class<?>... groups)
            throws ValidationServiceException
    {
        AppbaseUtil.notNull(object, "object");

        final Set<ConstraintViolation<Object>> constraintViolations =
                validationService.getValidator().validate(object, groups);
        if (constraintViolations != null && !constraintViolations.isEmpty()) {
            throw new ValidationServiceException(object, constraintViolations);
        }
    }

    public static <T> void checkedValidation(
            final ConstraintValidator<?, T> validator,
            final T object)
            throws ValidationServiceException
    {
        AppbaseUtil.notNull(object, "object");

        if (!validator.isValid(object, null)) {
            throw new ValidationServiceException("Invalid object " + object.getClass().getName());
        }
    }

    /**
     * Check if OS is <code>Windows</code>.
     *
     * @return true if <code>Windows</code>, false otherwise
     */
    public static boolean isWindows() {
        return System.getProperty("os.name").trim().toLowerCase().startsWith("windows");
    }


    /**
     * Get instance of an injected object from {@link GuiceInjectorService} using {@link InjectedTypeConfiguration}.
     *
     * @param guiceInjectorService {@link GuiceInjectorService}
     * @param injectedTypeConfiguration {@link InjectedTypeConfiguration}
     * @param <T> Object type
     * @return Instane of an injected object
     */
    public static <T> T injectedInstance(
            final GuiceInjectorService guiceInjectorService,
            final InjectedTypeConfiguration<T> injectedTypeConfiguration)
    {
        notNull(guiceInjectorService, "guiceInjectorService");
        notNull(injectedTypeConfiguration, "injectedTypeConfiguration");

        final Class<T> objectType = injectedTypeConfiguration.getObjectType();
        final Class<? extends Annotation> qualifierAnnotationType = injectedTypeConfiguration.getQualifierAnnotationType();

        Key<T> key = null;
        if (qualifierAnnotationType != null) {
            key = Key.get(objectType, qualifierAnnotationType);
        }

        final String qualifierName = injectedTypeConfiguration.getQualifierName();
        if (qualifierName != null) {
            final Named named = Names.named(qualifierName);
            key = Key.get(objectType, named);
        }

        if (key != null) {
            return guiceInjectorService.getInjector().getInstance(key);
        }

        return guiceInjectorService.getInjector().getInstance(objectType);
    }

    /**
     * Get a {@link ManyToManyQueuedFixedObjectPool}.
     *
     * @param metricService {@link MetricService}
     * @param manyToManyQueuedFixedObjectPoolConfiguration {@link ManyToManyQueuedFixedObjectPoolConfiguration}
     * @param objectFactory {@link ObjectFactory}
     * @param <T> Object type
     * @return {@link ManyToManyQueuedFixedObjectPool}
     */
    public static <T extends Poolable> ManyToManyQueuedFixedObjectPool<T> manyToManyQueuedFixedObjectPool(
            final MetricService metricService,
            final ManyToManyQueuedFixedObjectPoolConfiguration manyToManyQueuedFixedObjectPoolConfiguration,
            final ObjectFactory<T> objectFactory)
    {
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(
                manyToManyQueuedFixedObjectPoolConfiguration, "manyToManyQueuedFixedObjectPoolConfiguration");
        AppbaseUtil.notNull(objectFactory, "objectFactory");

        return new ManyToManyQueuedFixedObjectPool<>(
                metricService,
                manyToManyQueuedFixedObjectPoolConfiguration.getObjectPoolCapacity(),
                objectFactory);
    }

    public static String configurationStoreId(final ConfigurationStore configurationStore) {
        notNull(configurationStore, "configurationStore");

        return configurationStore.getClass().getName();
    }

    public static String configurationStoreId(final Class<? extends ConfigurationStore> configurationStoreType) {
        notNull(configurationStoreType, "configurationStoreType");

        return configurationStoreType.getName();
    }

    public static String configurationStoreIdPrefix(final ConfigurationStore configurationStore) {
        notNull(configurationStore, "configurationStore");

        return configurationStore.getClass().getName() + CONFIGURATION_STORE_ID_SEPARATOR;
    }

    public static String configurationStoreIdPrefix(final Class<? extends ConfigurationStore> configurationStoreType) {
        notNull(configurationStoreType, "configurationStoreType");

        return configurationStoreType.getName() + CONFIGURATION_STORE_ID_SEPARATOR;
    }


    /**
     * Assert that an object is not null.
     *
     * @param type {@link Class} instance
     * @return The input object
     * @throws AppbaseRuntimeException if the object is null
     */
    public static Class<?> notNullType(final Class<?> type) {
        notNull(type, "type");

        return type;
    }


    /**
     * Strip {@link AppbaseConstants#CLASSPATH_FILE_PREFIX} from the given filename.
     *
     * @param filename File name
     * @return File name without {@link AppbaseConstants#CLASSPATH_FILE_PREFIX}
     */
    public static String stripClasspathPrefix(final String filename) {
        if (filename.startsWith(AppbaseConstants.CLASSPATH_FILE_PREFIX)) {
            return filename.substring(AppbaseConstants.CLASSPATH_FILE_PREFIX.length());
        }

        return filename;
    }

    public static boolean completeIfNull(
            final Object object,
            final String argumentName,
            final CompletableFuture<?> future)
    {
        AppbaseUtil.notBlank(argumentName, "argumentName");
        AppbaseUtil.notNull(future, "future");

        if (object == null) {
            future.completeExceptionally(new IllegalArgumentException(argumentName + " must not be null"));
            return true;
        }

        return false;
    }

    public static boolean completeIfBlank(
            final String text,
            final String argumentName,
            final CompletableFuture<?> future)
    {
        AppbaseUtil.notBlank(argumentName, "argumentName");
        AppbaseUtil.notNull(future, "future");

        if (AppbaseUtil.isBlank(text)) {
            future.completeExceptionally(new IllegalArgumentException(argumentName + " must not be blank"));
            return true;
        }

        return false;
    }
}
