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

package com.viveworks.appbase.core.service.exception.store.jpa;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.exception.store.EntityManagerConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.ExceptionStore;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreException;
import com.viveworks.appbase.core.service.exception.store.StackTraceHashGenerator;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceFactory;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceType;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link ExceptionStore} implementation that uses <code>JPA</code>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
@SuppressWarnings("checkstyle:IllegalCatch")
@SuppressFBWarnings({"HES_EXECUTOR_NEVER_SHUTDOWN", "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE"})
public class JpaExceptionStore implements ExceptionStore {
    private static final String FALLBACK_THREAD_NAME_FORMAT = "appbase-jpa-exception-store-%d";

    private static final String ID_ATTRIBUTE_NAME = "id";
    private static final String DISCRIMINATOR_ATTRIBUTE_NAME = "discriminator";
    private static final String MESSAGE_ATTRIBUTE_NAME = "message";
    private static final String THROWABLE_TYPE_NAME_ATTRIBUTE_NAME = "throwableTypeName";
    private static final String TIMESTAMP_ATTRIBUTE_NAME = "timestamp";

    private final Logger logger;
    private final ValidationService validationService;
    private final AtomicReference<EntityManager> entityManagerRef;
    private final AtomicReference<EntityManagerConfiguration> entityManagerConfigurationRef;
    private final AtomicReference<String> discriminatorRef;
    private final AtomicReference<String> idColumnNameRef;
    private final AtomicReference<String> discriminatorColumnNameRef;
    private final AtomicReference<String> messageColumnNameRef;
    private final AtomicReference<String> throwableTypeNameColumnNameRef;
    private final AtomicReference<String> timestampColumnNameRef;
    private final AtomicReference<Integer> stackTraceSerializationMaxLengthRef;
    private final ExecutorService executorService;
    private final StackTraceHashGenerator stackTraceHashGenerator;
    private final ExceptionMessageContextCodec exceptionMessageContextCodec;

    private final Timer readExceptionMessageDetailsByIdTimer;
    private final Counter readExceptionMessageDetailsByIdErrorCounter;
    private final Timer asyncReadExceptionMessageDetailsByIdTimer;
    private final Counter asyncReadExceptionMessageDetailsByIdErrorCounter;

    private final Timer readAllExceptionMessageDetailsTimer;
    private final Counter readAllExceptionMessageDetailsErrorCounter;
    private final Timer asyncReadAllExceptionMessageDetailsTimer;
    private final Counter asyncReadAllExceptionMessageDetailsErrorCounter;

    private final Timer readAllExceptionMessageDetailsByTimestampsTimer;
    private final Counter readAllExceptionMessageDetailsByTimestampsErrorCounter;
    private final Timer asyncReadAllExceptionMessageDetailsByTimestampsTimer;
    private final Counter asyncReadAllExceptionMessageDetailsByTimestampsErrorCounter;

    private final Timer readUniqueExceptionTypeNamesTimer;
    private final Counter readUniqueExceptionTypeNamesErrorCounter;
    private final Timer asyncReadUniqueExceptionTypeNamesTimer;
    private final Counter asyncReadUniqueExceptionTypeNamesErrorCounter;

    private final Timer readExceptionMessageDetailsByThrowableTypeNameTimer;
    private final Counter readExceptionMessageDetailsByThrowableTypeNameErrorCounter;
    private final Timer asyncReadExceptionMessageDetailsByThrowableTypeNameTimer;
    private final Counter asyncReadExceptionMessageDetailsByThrowableTypeNameErrorCounter;

    private final Timer writeExceptionMessageTimer;
    private final Counter writeExceptionMessageErrorCounter;
    private final Timer asyncWriteExceptionMessageTimer;
    private final Counter asyncWriteExceptionMessageErrorCounter;

    private final Timer removeExceptionMessageDetailsByIdTimer;
    private final Counter removeExceptionMessageDetailsByIdErrorCounter;
    private final Timer asyncRemoveExceptionMessageDetailsByIdTimer;
    private final Counter asyncRemoveExceptionMessageDetailsByIdErrorCounter;

    private final Timer removeExceptionMessageDetailsBeforeTimer;
    private final Counter removeExceptionMessageDetailsBeforeErrorCounter;
    private final Timer asyncRemoveExceptionMessageDetailsBeforeTimer;
    private final Counter asyncRemoveExceptionMessageDetailsBeforeErrorCounter;

    private final AtomicBoolean shutdown;

    public JpaExceptionStore(final LogService logService,
                             final MetricService metricService,
                             final MediatorService mediatorService,
                             final ValidationService validationService,
                             final EntityManager entityManager,
                             final StackTraceHashGenerator stackTraceHashGenerator,
                             final JpaExceptionStoreConfiguration jpaExceptionStoreConfiguration,
                             final ExceptionMessageContextCodec exceptionMessageContextCodec)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(mediatorService, "mediatorService");
        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(entityManager, "entityManager");
        AppbaseUtil.notNull(stackTraceHashGenerator, "stackTraceHashGenerator");
        AppbaseUtil.notNull(jpaExceptionStoreConfiguration, "jpaExceptionStoreConfiguration");
        AppbaseUtil.validate(validationService, jpaExceptionStoreConfiguration);
        AppbaseUtil.notNull(exceptionMessageContextCodec, "exceptionMessageContextCodec");

        this.logger = logService.getLogger(getClass());
        this.validationService = validationService;
        this.entityManagerRef = new AtomicReference<>();
        this.entityManagerRef.set(entityManager);
        this.entityManagerConfigurationRef = new AtomicReference<>();
        this.entityManagerConfigurationRef.set(jpaExceptionStoreConfiguration.getEntityManagerConfiguration());

        ExecutorService tmpExecutorService = null;
        final ExecutorService sharedExecutorService = mediatorService.getMediator().getSharedExecutorService();
        if (sharedExecutorService instanceof ThreadPoolExecutor) {
            final int maximumPoolSize = ((ThreadPoolExecutor) sharedExecutorService).getMaximumPoolSize();
            if (maximumPoolSize == 1) {
                tmpExecutorService = sharedExecutorService;
            }
        }

        if (tmpExecutorService == null) {
            final String threadNameFormat = Objects.requireNonNullElse(
                    jpaExceptionStoreConfiguration
                            .getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                    FALLBACK_THREAD_NAME_FORMAT);
            final ExecutorServiceConfiguration executorServiceConfiguration = ExecutorServiceConfiguration
                    .builder()
                    .threadFactoryConfiguration(
                            ThreadFactoryConfiguration
                                    .builder()
                                    .nameFormat(threadNameFormat)
                                    .daemon(false)
                                    .enableThreadAffinity(false)
                                    .build())
                    .executorType(ExecutorServiceType.SINGLE_THREADED)
                    .build();
            this.executorService = ExecutorServiceFactory.newExecutorService(
                    executorServiceConfiguration,
                    (t, e) -> logger.error("Uncaught exception in thread {}", t.getName(), e));
        } else {
            this.executorService = tmpExecutorService;
        }

        this.stackTraceHashGenerator = stackTraceHashGenerator;
        this.exceptionMessageContextCodec = exceptionMessageContextCodec;

        this.discriminatorRef =
                new AtomicReference<>(jpaExceptionStoreConfiguration.getDiscriminator());
        this.discriminatorColumnNameRef =
                new AtomicReference<>(jpaExceptionStoreConfiguration.getDiscriminatorColumnName());
        this.idColumnNameRef =
                new AtomicReference<>(jpaExceptionStoreConfiguration.getIdColumnName());
        this.messageColumnNameRef =
                new AtomicReference<>(jpaExceptionStoreConfiguration.getMessageColumnName());
        this.throwableTypeNameColumnNameRef =
                new AtomicReference<>(jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName());
        this.timestampColumnNameRef =
                new AtomicReference<>(jpaExceptionStoreConfiguration.getTimestampColumnName());
        this.stackTraceSerializationMaxLengthRef =
                new AtomicReference<>(jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength());

        readExceptionMessageDetailsByIdTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "readExceptionMessageDetailsById", "timer"));
        readExceptionMessageDetailsByIdErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "readExceptionMessageDetailsById", "error", "counter"));
        asyncReadExceptionMessageDetailsByIdTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncReadExceptionMessageDetailsById", "timer"));
        asyncReadExceptionMessageDetailsByIdErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncReadExceptionMessageDetailsById", "error", "counter"));

        readAllExceptionMessageDetailsTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "readAllExceptionMessageDetails", "timer"));
        readAllExceptionMessageDetailsErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "readAllExceptionMessageDetails", "error", "counter"));
        asyncReadAllExceptionMessageDetailsTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncReadAllExceptionMessageDetails", "timer"));
        asyncReadAllExceptionMessageDetailsErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncReadAllExceptionMessageDetails", "error", "counter"));

        readAllExceptionMessageDetailsByTimestampsTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "readAllExceptionMessageDetailsByTimestamps", "timer"));
        readAllExceptionMessageDetailsByTimestampsErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(
                        getClass(), "readAllExceptionMessageDetailsByTimestamps", "error", "counter"));
        asyncReadAllExceptionMessageDetailsByTimestampsTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncReadAllExceptionMessageDetailsByTimestamps", "timer"));
        asyncReadAllExceptionMessageDetailsByTimestampsErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(
                        getClass(), "asyncReadAllExceptionMessageDetailsByTimestamps", "error", "counter"));

        readUniqueExceptionTypeNamesTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "readUniqueExceptionTypeNames", "timer"));
        readUniqueExceptionTypeNamesErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "readUniqueExceptionTypeNames", "error", "counter"));
        asyncReadUniqueExceptionTypeNamesTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncReadUniqueExceptionTypeNames", "timer"));
        asyncReadUniqueExceptionTypeNamesErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncReadUniqueExceptionTypeNames", "error", "counter"));

        readExceptionMessageDetailsByThrowableTypeNameTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "readExceptionMessageDetailsByThrowableTypeName", "timer"));
        readExceptionMessageDetailsByThrowableTypeNameErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(
                        getClass(), "readExceptionMessageDetailsByThrowableTypeName", "error", "counter"));
        asyncReadExceptionMessageDetailsByThrowableTypeNameTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncReadExceptionMessageDetailsByThrowableTypeName", "timer"));
        asyncReadExceptionMessageDetailsByThrowableTypeNameErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(
                        getClass(), "asyncReadExceptionMessageDetailsByThrowableTypeName", "error", "counter"));

        writeExceptionMessageTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "writeExceptionMessage", "timer"));
        writeExceptionMessageErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "writeExceptionMessage", "error", "counter"));
        asyncWriteExceptionMessageTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncWriteExceptionMessage", "timer"));
        asyncWriteExceptionMessageErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncWriteExceptionMessage", "error", "counter"));

        removeExceptionMessageDetailsByIdTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "removeExceptionMessageDetailsById", "timer"));
        removeExceptionMessageDetailsByIdErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "removeExceptionMessageDetailsById", "error", "counter"));
        asyncRemoveExceptionMessageDetailsByIdTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncRemoveExceptionMessageDetailsById", "timer"));
        asyncRemoveExceptionMessageDetailsByIdErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncRemoveExceptionMessageDetailsById", "error", "counter"));

        removeExceptionMessageDetailsBeforeTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "removeExceptionMessageDetailsBefore", "timer"));
        removeExceptionMessageDetailsBeforeErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(
                        getClass(), "removeExceptionMessageDetailsBefore", "error", "counter"));
        asyncRemoveExceptionMessageDetailsBeforeTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncRemoveExceptionMessageDetailsBefore", "timer"));
        asyncRemoveExceptionMessageDetailsBeforeErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(
                        getClass(), "asyncRemoveExceptionMessageDetailsBefore", "error", "counter"));

        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public ExceptionMessageDetails readExceptionMessageDetailsById(final int id) throws ExceptionStoreException {
        final Timer.Context context = readExceptionMessageDetailsByIdTimer.time();

        final EntityManager entityManager = entityManagerRef.get();
        entityManager.getTransaction().begin();
        try {
            return entityManager.find(ExceptionMessageDetails.class, id);
        } catch (Exception ex) {
            logger.error("Error in 'readExceptionMessageDetailsById()'", ex);
            readExceptionMessageDetailsByIdErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            entityManager.getTransaction().commit();
            context.stop();
        }
    }

    @Override
    public CompletableFuture<ExceptionMessageDetails> asyncReadExceptionMessageDetailsById(final int id) {
        final CompletableFuture<ExceptionMessageDetails> resultFuture = new CompletableFuture<>();
        final Timer.Context context = asyncReadExceptionMessageDetailsByIdTimer.time();

        executorService.submit(() -> {
            try {
                resultFuture.complete(readExceptionMessageDetailsById(id));
            } catch (Exception ex) {
                logger.error("Error in 'asyncReadExceptionMessageDetailsById()'", ex);
                asyncReadExceptionMessageDetailsByIdErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public Collection<ExceptionMessageDetails> readAllExceptionMessageDetails() throws ExceptionStoreException {
        final Timer.Context context = readAllExceptionMessageDetailsTimer.time();
        final EntityManager entityManager = entityManagerRef.get();
        entityManager.getTransaction().begin();

        try {
            final CriteriaQuery<ExceptionMessageDetails> criteriaQuery = entityManager
                    .getCriteriaBuilder()
                    .createQuery(ExceptionMessageDetails.class);
            final Root<ExceptionMessageDetails> root = criteriaQuery.from(ExceptionMessageDetails.class);
            criteriaQuery.select(root);
            return entityManager
                    .createQuery(criteriaQuery)
                    .getResultList();
        } catch (Exception ex) {
            logger.error("Error in 'readAllExceptionMessageDetails()'", ex);
            readAllExceptionMessageDetailsErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            entityManager.getTransaction().commit();
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadAllExceptionMessageDetails() {
        final CompletableFuture<Collection<ExceptionMessageDetails>> resultFuture = new CompletableFuture<>();
        final Timer.Context context = asyncReadAllExceptionMessageDetailsTimer.time();
        executorService.submit(() -> {
            try {
                resultFuture.complete(readAllExceptionMessageDetails());
            } catch (Exception ex) {
                logger.error("Error in 'asyncReadAllExceptionMessageDetails()'", ex);
                asyncReadAllExceptionMessageDetailsErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public Collection<ExceptionMessageDetails> readAllExceptionMessageDetailsByTimestamps(
            final long inclusiveMinTimestamp,
            final long exclusiveMaxTimestamp) throws ExceptionStoreException {
        AppbaseUtil.positive(inclusiveMinTimestamp, "inclusiveMinTimestamp");
        AppbaseUtil.positive(exclusiveMaxTimestamp, "exclusiveMaxTimestamp");

        final Timer.Context context = readAllExceptionMessageDetailsByTimestampsTimer.time();

        final EntityManager entityManager = entityManagerRef.get();
        entityManager.getTransaction().begin();

        try {
            final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            final CriteriaQuery<ExceptionMessageDetails> criteriaQuery =
                    criteriaBuilder.createQuery(ExceptionMessageDetails.class);
            final Root<ExceptionMessageDetails> root = criteriaQuery.from(ExceptionMessageDetails.class);
            criteriaQuery.select(root);
            final Predicate predicateMin =
                    criteriaBuilder.ge(root.get(TIMESTAMP_ATTRIBUTE_NAME), inclusiveMinTimestamp);
            final Predicate predicateMax =
                    criteriaBuilder.lessThan(root.get(TIMESTAMP_ATTRIBUTE_NAME), exclusiveMaxTimestamp);
            criteriaQuery
                    .where(predicateMin, predicateMax)
                    .orderBy(criteriaBuilder.asc(root.get(ID_ATTRIBUTE_NAME)));
            return entityManager
                    .createQuery(criteriaQuery)
                    .getResultList();
        } catch (Exception ex) {
            logger.error("Error in 'readAllExceptionMessageDetailsByTimestamps()'", ex);
            readAllExceptionMessageDetailsByTimestampsErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            entityManager.getTransaction().commit();
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadAllExceptionMessageDetailsByTimestamps(
            final long inclusiveMinTimestamp, final long exclusiveMaxTimestamp)
    {
        AppbaseUtil.positive(inclusiveMinTimestamp, "inclusiveMinTimestamp");
        AppbaseUtil.positive(exclusiveMaxTimestamp, "exclusiveMaxTimestamp");

        final CompletableFuture<Collection<ExceptionMessageDetails>> resultFuture = new CompletableFuture<>();
        final Timer.Context context = asyncReadAllExceptionMessageDetailsByTimestampsTimer.time();
        executorService.submit(() -> {
            try {
                resultFuture.complete(
                        readAllExceptionMessageDetailsByTimestamps(inclusiveMinTimestamp, exclusiveMaxTimestamp));
            } catch (Exception ex) {
                logger.error("Error in 'asyncReadAllExceptionMessageDetailsByTimestamps()'", ex);
                asyncReadAllExceptionMessageDetailsByTimestampsErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public Collection<String> readUniqueThrowableTypeNames() throws ExceptionStoreException {
        final Timer.Context context = readUniqueExceptionTypeNamesTimer.time();

        final EntityManager entityManager = entityManagerRef.get();
        entityManager.getTransaction().begin();

        try {
            final CriteriaQuery<String> criteriaQuery = entityManager
                    .getCriteriaBuilder()
                    .createQuery(String.class);
            final Root<ExceptionMessageDetails> root = criteriaQuery.from(ExceptionMessageDetails.class);
            criteriaQuery
                    .select(root.get(THROWABLE_TYPE_NAME_ATTRIBUTE_NAME))
                    .distinct(true);
            final TypedQuery<String> typedQuery = entityManager.createQuery(criteriaQuery);
            return typedQuery.getResultList();
        } catch (Exception ex) {
            logger.error("Error in 'readUniqueThrowableTypeNames()'", ex);
            readUniqueExceptionTypeNamesErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            entityManager.getTransaction().commit();
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Collection<String>> asyncReadUniqueThrowableTypeNames() {
        final CompletableFuture<Collection<String>> resultFuture = new CompletableFuture<>();

        final Timer.Context context = asyncReadUniqueExceptionTypeNamesTimer.time();
        executorService.submit(() -> {
            try {
                resultFuture.complete(readUniqueThrowableTypeNames());
            } catch (Exception ex) {
                logger.error("Error in 'asyncReadUniqueThrowableTypeNames()'", ex);
                asyncReadUniqueExceptionTypeNamesErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public Collection<ExceptionMessageDetails> readExceptionMessageDetailsByThrowableTypeName(
            final String throwableTypeName) throws ExceptionStoreException
    {
        AppbaseUtil.notBlank(throwableTypeName, "throwableTypeName");

        final Timer.Context context = readExceptionMessageDetailsByThrowableTypeNameTimer.time();
        final EntityManager entityManager = entityManagerRef.get();
        entityManager.getTransaction().begin();

        try {
            final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            final CriteriaQuery<ExceptionMessageDetails> criteriaQuery =
                    criteriaBuilder.createQuery(ExceptionMessageDetails.class);
            final Root<ExceptionMessageDetails> root = criteriaQuery.from(ExceptionMessageDetails.class);
            criteriaQuery.select(root);
            final Predicate predicate =
                    criteriaBuilder.equal(root.get(THROWABLE_TYPE_NAME_ATTRIBUTE_NAME), throwableTypeName);
            criteriaQuery
                    .where(predicate)
                    .orderBy(criteriaBuilder.asc(root.get(ID_ATTRIBUTE_NAME)));
            return entityManager
                    .createQuery(criteriaQuery)
                    .getResultList();
        } catch (Exception ex) {
            logger.error("Error in 'readExceptionMessageDetailsByThrowableTypeName()'", ex);
            readExceptionMessageDetailsByThrowableTypeNameErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            entityManager.getTransaction().commit();
            context.stop();
        }
    }

    @Override
    @SuppressFBWarnings({"INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE"})
    public CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadExceptionMessageDetailsByThrowableTypeName(
            final String throwableTypeName)
    {
        AppbaseUtil.notBlank(throwableTypeName, "throwableTypeName");

        final CompletableFuture<Collection<ExceptionMessageDetails>> resultFuture = new CompletableFuture<>();
        final Timer.Context context = asyncReadExceptionMessageDetailsByThrowableTypeNameTimer.time();
        executorService.submit(() -> {
            try {
                resultFuture.complete(
                        readExceptionMessageDetailsByThrowableTypeName(throwableTypeName));
            } catch (Exception ex) {
                logger.error("Error in 'asyncReadExceptionMessageDetailsByThrowableTypeName()'", ex);
                asyncReadExceptionMessageDetailsByThrowableTypeNameErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public void writeExceptionMessage(final ExceptionMessage exceptionMessage) throws ExceptionStoreException {
        AppbaseUtil.notNull(exceptionMessage, "exceptionMessage");

        final Timer.Context timerContext = writeExceptionMessageTimer.time();

        final EntityManager entityManager = entityManagerRef.get();
        entityManager.getTransaction().begin();
        try {
            final ExceptionMessageDetails exceptionMessageDetails = new ExceptionMessageDetails();
            exceptionMessageDetails.setDiscriminator(discriminatorRef.get());

            final Throwable throwable = exceptionMessage.getThrowable();
            if (throwable != null) {
                final String stackTraceString = ExceptionStore.serializeThrowable(
                        throwable, stackTraceSerializationMaxLengthRef.get());
                final int hash = stackTraceHashGenerator.generateHash(stackTraceString);
                exceptionMessageDetails.setStackTrace(stackTraceString);
                exceptionMessageDetails.setStackTraceHash(hash);
                exceptionMessageDetails.setThrowableMessage(throwable.getMessage());
                exceptionMessageDetails.setThrowableTypeName(throwable.getClass().getName());
            }

            exceptionMessageDetails.setMessage(exceptionMessage.getMessage());
            exceptionMessageDetails.setThreadName(exceptionMessage.getThreadName());

            final Class<?> sourceType = exceptionMessage.getSourceType();
            exceptionMessageDetails.setSourceTypeName(sourceType.getName());

            final Map<String, String> context = exceptionMessage.getContext();
            if (context != null) {
                final String encodedContext = exceptionMessageContextCodec.encode(context);
                exceptionMessageDetails.setContext(encodedContext);
            }

            exceptionMessageDetails.setTimestamp(exceptionMessage.getTimestamp());
            AppbaseUtil.validate(
                    validationService,
                    exceptionMessageDetails,
                    ExceptionMessageDetails.WriteChecks.class);

            entityManager.persist(exceptionMessageDetails);
        } catch (Exception ex) {
            logger.error("Error in 'writeExceptionMessage()'", ex);
            writeExceptionMessageErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            entityManager.getTransaction().commit();
            timerContext.stop();
        }
    }

    @Override
    public CompletableFuture<Void> asyncWriteExceptionMessage(final ExceptionMessage exceptionMessage) {
        AppbaseUtil.notNull(exceptionMessage, "exceptionMessage");

        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        final Timer.Context timerContext = asyncWriteExceptionMessageTimer.time();
        executorService.submit(() -> {
            try {
                writeExceptionMessage(exceptionMessage);
                resultFuture.complete(null);
            } catch (Exception ex) {
                logger.error("Error in 'asyncWriteExceptionMessage()'", ex);
                asyncWriteExceptionMessageErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                timerContext.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public void removeExceptionMessageDetailsById(final int id) throws ExceptionStoreException {
        AppbaseUtil.positive(id, "id");

        final Timer.Context context = removeExceptionMessageDetailsByIdTimer.time();

        final EntityManager entityManager = entityManagerRef.get();
        entityManager.getTransaction().begin();
        try {
            final ExceptionMessageDetails exceptionMessageDetails =
                    entityManager.find(ExceptionMessageDetails.class, id);
            if (exceptionMessageDetails != null) {
                entityManager.remove(exceptionMessageDetails);
            }

            logger.debug("Remove ExceptionMessageDetails by [id={}]", id);
        } catch (Exception ex) {
            logger.error("Error in 'removeExceptionMessageDetailsById()'", ex);
            removeExceptionMessageDetailsByIdErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            entityManager.getTransaction().commit();
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Void> asyncRemoveExceptionMessageDetailsById(final int id) {
        AppbaseUtil.positive(id, "id");

        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        final Timer.Context context = asyncRemoveExceptionMessageDetailsByIdTimer.time();
        executorService.submit(() -> {
            try {
                removeExceptionMessageDetailsById(id);
                resultFuture.complete(null);
            } catch (Exception ex) {
                logger.error("Error in 'asyncRemoveExceptionMessageDetailsById()'", ex);
                asyncRemoveExceptionMessageDetailsByIdErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public void removeExceptionMessageDetailsExclusiveBefore(long timestamp) throws ExceptionStoreException {
        AppbaseUtil.positive(timestamp, "timestamp");

        final Timer.Context context = removeExceptionMessageDetailsBeforeTimer.time();

        final EntityManager entityManager = entityManagerRef.get();
        entityManager.getTransaction().begin();
        try {
            final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            final CriteriaQuery<ExceptionMessageDetails> criteriaQuery =
                    criteriaBuilder.createQuery(ExceptionMessageDetails.class);
            final Root<ExceptionMessageDetails> queryRoot = criteriaQuery.from(ExceptionMessageDetails.class);
            criteriaQuery.select(queryRoot);
            final Predicate readPredicate =
                    criteriaBuilder.lessThan(queryRoot.get(TIMESTAMP_ATTRIBUTE_NAME), timestamp);
            criteriaQuery
                    .where(readPredicate)
                    .orderBy(criteriaBuilder.asc(queryRoot.get(ID_ATTRIBUTE_NAME)));
            final Collection<ExceptionMessageDetails> resultList = entityManager
                    .createQuery(criteriaQuery)
                    .getResultList();

            if (resultList != null) {
                final CriteriaDelete<ExceptionMessageDetails> criteriaDelete =
                        criteriaBuilder.createCriteriaDelete(ExceptionMessageDetails.class);
                final Root<ExceptionMessageDetails> deleteRoot = criteriaDelete.from(ExceptionMessageDetails.class);
                final Predicate deletePredicate =
                        criteriaBuilder.lessThan(deleteRoot.get(TIMESTAMP_ATTRIBUTE_NAME), timestamp);
                criteriaDelete.where(deletePredicate);
                final int update = entityManager
                        .createQuery(criteriaDelete)
                        .executeUpdate();
                logger.debug("Remove ExceptionMessageDetails before [timestamp={}, result={}]", timestamp, update);

                resultList.forEach(entityManager::detach);
            }
        } catch (Exception ex) {
            logger.error("Error in 'removeExceptionMessageDetailsExclusiveBefore()'", ex);
            removeExceptionMessageDetailsBeforeErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            entityManager.getTransaction().commit();
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Void> asyncRemoveExceptionMessageDetailsExclusiveBefore(final long timestamp) {
        AppbaseUtil.positive(timestamp, "timestamp");

        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        final Timer.Context context = asyncRemoveExceptionMessageDetailsBeforeTimer.time();
        executorService.submit(() -> {
            try {
                removeExceptionMessageDetailsExclusiveBefore(timestamp);
                resultFuture.complete(null);
            } catch (Exception ex) {
                logger.error("Error in 'asyncRemoveExceptionMessageDetailsExclusiveBefore()'", ex);
                asyncRemoveExceptionMessageDetailsBeforeErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public void reload(
            final ExceptionStoreConfiguration exceptionStoreConfiguration) throws ExceptionStoreException
    {
        AppbaseUtil.notNull(exceptionStoreConfiguration, "exceptionStoreConfiguration");

        if (!(exceptionStoreConfiguration instanceof JpaExceptionStoreConfiguration)) {
            throw new ExceptionStoreException(
                    "exceptionStoreConfiguration must be of type " + JpaExceptionStoreConfiguration.class.getName());
        }

        final JpaExceptionStoreConfiguration jpaExceptionStoreConfiguration =
                (JpaExceptionStoreConfiguration) exceptionStoreConfiguration;
        AppbaseUtil.validate(validationService, jpaExceptionStoreConfiguration);

        entityManagerConfigurationRef.set(jpaExceptionStoreConfiguration.getEntityManagerConfiguration());
        discriminatorRef.set(jpaExceptionStoreConfiguration.getDiscriminator());
        idColumnNameRef.set(jpaExceptionStoreConfiguration.getIdColumnName());
        discriminatorColumnNameRef.set(jpaExceptionStoreConfiguration.getDiscriminatorColumnName());
        messageColumnNameRef.set(jpaExceptionStoreConfiguration.getMessageColumnName());
        throwableTypeNameColumnNameRef.set(jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName());
        timestampColumnNameRef.set(jpaExceptionStoreConfiguration.getTimestampColumnName());
        stackTraceSerializationMaxLengthRef.set(jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength());

        final EntityManagerConfiguration currentEntityManagerConfiguration = entityManagerConfigurationRef.get();
        final EntityManagerConfiguration newEntityManagerConfiguration =
                jpaExceptionStoreConfiguration.getEntityManagerConfiguration();
        if (!currentEntityManagerConfiguration.equals(newEntityManagerConfiguration)) {
            final String persistenceUnitName =
                    newEntityManagerConfiguration.getPersistenceUnitName();
            final Map<String, String> entityManagerProperties =
                    newEntityManagerConfiguration.getEntityManagerProperties();
            final Map<String, String> entityManagerFactoryProperties =
                    newEntityManagerConfiguration.getEntityManagerFactoryProperties();
            final EntityManagerFactory entityManagerFactory =
                    Persistence.createEntityManagerFactory(persistenceUnitName, entityManagerFactoryProperties);
            final EntityManager entityManager = entityManagerFactory.createEntityManager(entityManagerProperties);
            shutdown();
            this.entityManagerRef.set(entityManager);
        }
    }

    @Override
    public void synchronize() throws ExceptionStoreException {
        logger.info("Synchronizing {}", getClass().getName());
        // no-op
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        final EntityManager entityManager = entityManagerRef.get();
        entityManager.clear();
        entityManager.close();
    }
}
