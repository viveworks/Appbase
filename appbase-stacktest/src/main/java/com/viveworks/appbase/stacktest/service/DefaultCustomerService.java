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
package com.viveworks.appbase.stacktest.service;

import com.google.common.annotations.VisibleForTesting;
import com.viveworks.appbase.client.service.aerospike.accessor.AerospikeAccessorException;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlClientException;
import com.viveworks.appbase.client.service.zookeeper.accessor.DistributedCounterZKA;
import com.viveworks.appbase.client.service.zookeeper.accessor.ZooKeeperAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.service.validation.ValidationServiceException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.stacktest.accessor.aerospike.CustomerASA;
import com.viveworks.appbase.stacktest.accessor.druid.DummyUser;
import com.viveworks.appbase.stacktest.accessor.druid.DummyUserDRA;
import com.viveworks.appbase.stacktest.accessor.elasticsearch.CustomerSIA;
import com.viveworks.appbase.stacktest.accessor.s3.CustomerPhotoOSA;
import com.viveworks.appbase.stacktest.accessor.vitess.CustomerJDBA;
import com.viveworks.appbase.stacktest.data.model.Customer;
import com.viveworks.appbase.stacktest.data.model.Order;
import com.viveworks.appbase.stacktest.data.pojo.customer.CreateCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.CreateCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.CustomerStatisticsRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.CustomerStatisticsResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.DeleteCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.DeleteCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.DownloadCustomerPhotoByCustomerIdRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.DownloadCustomerPhotoByCustomerIdResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.DownloadCustomerPhotoByPhotoIdRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.DownloadCustomerPhotoByPhotoIdResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomerOrdersRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomerOrdersResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomersRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomersResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.SearchCustomersRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.SearchCustomersResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.UpdateCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.UpdateCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.UploadCustomerPhotoRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.UploadCustomerPhotoResponse;
import jakarta.validation.executable.ValidateOnExecution;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultCustomerService implements CustomerService {
    private final Logger logger;
    private final ValidationService validationService;
    private final boolean manualValidation;
    private final CustomerASA customerASA;
    private final DummyUserDRA dummyUserDRA;
    private final CustomerSIA customerSIA;
    private final CustomerPhotoOSA customerPhotoOSA;
    private final CustomerJDBA customerJDBA;
    private final DistributedCounterZKA distributedCounterZKA;

    private final Timer uploadCustomerPhotoTimer;
    private final Timer downloadCustomerPhotoByCustomerIdTimer;
    private final Timer downloadCustomerPhotoByPhotoIdTimer;
    private final Timer customerStatisticsTimer;
    private final Timer searchCustomersTimer;
    private final Timer createCustomerTimer;
    private final Timer readCustomersTimer;
    private final Timer readByIdTimer;
    private final Timer readCustomerOrdersTimer;
    private final Timer updateCustomerTimer;
    private final Timer deleteByIdTimer;

    public DefaultCustomerService(
            final AppbaseClientServices clientServices,
            final boolean manualValidation,
            final CustomerASA customerASA,
            final DummyUserDRA dummyUserDRA,
            final CustomerSIA customerSIA,
            final CustomerPhotoOSA customerPhotoOSA,
            final CustomerJDBA customerJDBA,
            final DistributedCounterZKA distributedCounterZKA)
    {
        AppbaseUtil.notNull(clientServices, "clientServices");
        AppbaseUtil.notNull(customerASA, "customerASA");
        AppbaseUtil.notNull(dummyUserDRA, "dummyUserDRA");
        AppbaseUtil.notNull(customerSIA, "customerSIA");
        AppbaseUtil.notNull(customerPhotoOSA, "customerPhotoOSA");
        AppbaseUtil.notNull(customerJDBA, "customerJDBA");
        AppbaseUtil.notNull(distributedCounterZKA, "distributedCounterZKA");

        final AppbaseCore appbaseCore = clientServices.getCoreServices().appbaseCore();
        this.logger = appbaseCore.getLogService().getLogger(getClass());
        this.validationService = appbaseCore.getValidationService();
        this.manualValidation = manualValidation;
        this.customerASA = customerASA;
        this.dummyUserDRA = dummyUserDRA;
        this.customerSIA = customerSIA;
        this.customerPhotoOSA = customerPhotoOSA;
        this.customerJDBA = customerJDBA;
        this.distributedCounterZKA = distributedCounterZKA;

        final MetricService metricService = appbaseCore.getMetricService();
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.uploadCustomerPhotoTimer =
                metricRegistry.timer(metricService.name(getClass(), "uploadCustomerPhotoTimer"));
        this.downloadCustomerPhotoByCustomerIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "downloadCustomerPhotoByCustomerIdTimer"));
        this.downloadCustomerPhotoByPhotoIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "downloadCustomerPhotoByPhotoIdTimer"));
        this.customerStatisticsTimer =
                metricRegistry.timer(metricService.name(getClass(), "customerStatisticsTimer"));
        this.searchCustomersTimer =
                metricRegistry.timer(metricService.name(getClass(), "searchCustomersTimer"));
        this.createCustomerTimer =
                metricRegistry.timer(metricService.name(getClass(), "createCustomerTimer"));
        this.readCustomersTimer =
                metricRegistry.timer(metricService.name(getClass(), "readCustomersTimer"));
        this.readByIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "readByIdTimer"));
        this.readCustomerOrdersTimer =
                metricRegistry.timer(metricService.name(getClass(), "readCustomerOrdersTimer"));
        this.updateCustomerTimer =
                metricRegistry.timer(metricService.name(getClass(), "updateCustomerTimer"));
        this.deleteByIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteByIdTimer"));
    }

    @Override
    @ValidateOnExecution
    @ReadCustomersResponse.Valid
    public ReadCustomersResponse readCustomers(
            @ReadCustomersRequest.Valid
            final ReadCustomersRequest readCustomersRequest)
            throws StackTestServiceException
    {
        AppbaseUtil.notNull(readCustomersRequest, "readCustomersRequest");

        final Timer.Context timer = readCustomersTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new ReadCustomersRequest.Validator(), readCustomersRequest);
            }

            final List<Customer> customers = customerJDBA.readCustomers(readCustomersRequest.getLimit());
            final ReadCustomersResponse readCustomersResponse =
                    new ReadCustomersResponse(customers == null ? List.of() : customers,
                            readCustomersRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new ReadCustomersResponse.Validator(), readCustomersResponse);
            }

            return readCustomersResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @UploadCustomerPhotoResponse.Valid
    public UploadCustomerPhotoResponse uploadCustomerPhoto(
            @UploadCustomerPhotoRequest.Valid
            final UploadCustomerPhotoRequest uploadCustomerPhotoRequest) throws StackTestServiceException
    {
        AppbaseUtil.notNull(uploadCustomerPhotoRequest, "uploadCustomerPhotoRequest");

        Path sourcePath = null;
        final Timer.Context timer = uploadCustomerPhotoTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new UploadCustomerPhotoRequest.Validator(), uploadCustomerPhotoRequest);
            }

            final Long customerId = uploadCustomerPhotoRequest.getCustomerId();
            sourcePath = uploadCustomerPhotoRequest.getSourcePath();

            final long photoId;
            try {
                photoId = distributedCounterZKA.increment();
            } catch (ZooKeeperAccessorException e) {
                throw new StackTestServiceException(e);
            }

            final String destinationPath;
            try {
                destinationPath = customerPhotoOSA.writeObject(sourcePath, String.valueOf(photoId));
            } catch (IOException e) {
                throw new StackTestServiceException(e);
            }

            try {
                customerJDBA.updatePhotoId(String.valueOf(photoId), customerId);
            } catch (Exception ex) {
                try {
                    customerPhotoOSA.deleteObject(String.valueOf(photoId));
                } catch (IOException ioe) {
                    throw new StackTestServiceException(ioe);
                }

                throw new StackTestServiceException(ex);
            }

            try {
                customerASA.deleteCustomer(customerId);
            } catch (AerospikeAccessorException e) {
                logger.error("Error deleting from aerospike: {}", customerId, e);
            }

            final UploadCustomerPhotoResponse uploadCustomerPhotoResponse =
                    new UploadCustomerPhotoResponse(destinationPath, uploadCustomerPhotoRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(
                        new UploadCustomerPhotoResponse.Validator(), uploadCustomerPhotoResponse);
            }
            return uploadCustomerPhotoResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            if (sourcePath != null) {
                try {
                    Files.deleteIfExists(sourcePath);
                } catch (IOException ioe) {
                    logger.error("Error deleting source path: {}", sourcePath, ioe);
                }
            }
            timer.stop();
        }
    }

    @Nullable
    @ValidateOnExecution
    @DownloadCustomerPhotoByCustomerIdResponse.Valid
    @Override
    public DownloadCustomerPhotoByCustomerIdResponse downloadCustomerPhotoByCustomerId(
            @DownloadCustomerPhotoByCustomerIdRequest.Valid
            final DownloadCustomerPhotoByCustomerIdRequest downloadCustomerPhotoByCustomerIdRequest)
            throws StackTestServiceException
    {
        AppbaseUtil.notNull(downloadCustomerPhotoByCustomerIdRequest, "downloadCustomerPhotoByCustomerIdRequest");

        final Timer.Context timer = downloadCustomerPhotoByCustomerIdTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(
                        new DownloadCustomerPhotoByCustomerIdRequest.Validator(),
                        downloadCustomerPhotoByCustomerIdRequest);
            }

            final Long customerId = downloadCustomerPhotoByCustomerIdRequest.getCustomerId();
            final ReadCustomerResponse readCustomerResponse = readCustomer(
                    new ReadCustomerRequest(customerId, downloadCustomerPhotoByCustomerIdRequest.getTraceId()));
            if (readCustomerResponse == null) {
                throw new StackTestServiceException("Cannot find customer: " + customerId);
            }

            final Customer customer = readCustomerResponse.getCustomer();
            if (customer == null) {
                throw new StackTestServiceException("Customer with id " + customerId + " not found");
            }

            final String photoId = customer.getPhotoId();
            if (AppbaseUtil.isBlank(photoId)) {
                return null;
            }

            try {
                final InputStream inputStream = customerPhotoOSA.readObjectAsInputStream(photoId);
                final DownloadCustomerPhotoByCustomerIdResponse downloadCustomerPhotoByCustomerIdResponse =
                        new DownloadCustomerPhotoByCustomerIdResponse(
                                inputStream,
                                photoId,
                                "application/octet-stream",
                                downloadCustomerPhotoByCustomerIdRequest.getTraceId());
                if (manualValidation) {
                    AppbaseUtil.checkedValidation(
                            new DownloadCustomerPhotoByCustomerIdResponse.Validator(),
                            downloadCustomerPhotoByCustomerIdResponse);
                }
                return downloadCustomerPhotoByCustomerIdResponse;
            } catch (NoSuchKeyException e) {
                // This exception is AS client specific.
                // It should not bleed into the service layer, OSA must handle it.
                return new DownloadCustomerPhotoByCustomerIdResponse(null, null, null, null);
            } catch (IOException e) {
                throw new StackTestServiceException(e);
            }
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @ValidateOnExecution
    @DownloadCustomerPhotoByPhotoIdResponse.Valid
    @Override
    public DownloadCustomerPhotoByPhotoIdResponse downloadCustomerPhotoByPhotoId(
            @DownloadCustomerPhotoByPhotoIdRequest.Valid
            final DownloadCustomerPhotoByPhotoIdRequest downloadCustomerPhotoByPhotoIdRequest)
            throws StackTestServiceException
    {
        AppbaseUtil.notNull(downloadCustomerPhotoByPhotoIdRequest, "downloadCustomerPhotoByPhotoIdRequest");

        final Timer.Context timer = downloadCustomerPhotoByPhotoIdTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(
                        new DownloadCustomerPhotoByPhotoIdRequest.Validator(),
                        downloadCustomerPhotoByPhotoIdRequest);
            }

            final String photoId = downloadCustomerPhotoByPhotoIdRequest.getPhotoId();
            try {
                final InputStream inputStream = customerPhotoOSA.readObjectAsInputStream(photoId);
                final DownloadCustomerPhotoByPhotoIdResponse downloadCustomerPhotoByPhotoIdResponse =
                        new DownloadCustomerPhotoByPhotoIdResponse(
                                inputStream,
                                photoId,
                                "application/octet-stream",
                                downloadCustomerPhotoByPhotoIdRequest.getTraceId());
                if (manualValidation) {
                    AppbaseUtil.checkedValidation(
                            new DownloadCustomerPhotoByPhotoIdResponse.Validator(),
                            downloadCustomerPhotoByPhotoIdResponse);
                }
                return downloadCustomerPhotoByPhotoIdResponse;
            } catch (NoSuchKeyException e) {
                // This exception is AS client specific.
                // It should not bleed into the service layer, OSA must handle it.
                return new DownloadCustomerPhotoByPhotoIdResponse(null, null, null, null);
            } catch (IOException e) {
                throw new StackTestServiceException(e);
            }
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @CustomerStatisticsResponse.Valid
    public CustomerStatisticsResponse customerStatistics(
            @CustomerStatisticsRequest.Valid
            final CustomerStatisticsRequest customerStatisticsRequest) throws StackTestServiceException
    {
        AppbaseUtil.notNull(customerStatisticsRequest, "customerStatisticsRequest");

        final Timer.Context timer = customerStatisticsTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new CustomerStatisticsRequest.Validator(), customerStatisticsRequest);
            }

            final DummyUser dummyUser = dummyUserDRA.readDummyUser();

            final CustomerStatisticsResponse customerStatisticsResponse =
                    new CustomerStatisticsResponse(dummyUser, customerStatisticsRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new CustomerStatisticsResponse.Validator(), customerStatisticsResponse);
            }
            return customerStatisticsResponse;
        } catch (DruidSqlClientException | ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @SearchCustomersResponse.Valid
    public SearchCustomersResponse searchCustomers(
            @SearchCustomersRequest.Valid
            final SearchCustomersRequest searchCustomersRequest) throws StackTestServiceException
    {
        AppbaseUtil.notNull(searchCustomersRequest, "searchCustomersRequest");

        final Timer.Context timer = searchCustomersTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new SearchCustomersRequest.Validator(), searchCustomersRequest);
            }

            final String emailPattern = searchCustomersRequest.getEmailPattern();

            final Customer customer = new Customer();
            customer.setEmail(emailPattern);

            try {
                final List<Customer> customers = customerSIA.search(customer);

                final SearchCustomersResponse searchCustomersResponse =
                        new SearchCustomersResponse(customers, searchCustomersRequest.getTraceId());
                if (manualValidation) {
                    AppbaseUtil.checkedValidation(new SearchCustomersResponse.Validator(), searchCustomersResponse);
                }
                return searchCustomersResponse;
            } catch (IOException e) {
                throw new StackTestServiceException(e);
            }
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @CreateCustomerResponse.Valid
    public CreateCustomerResponse createCustomer(
            @CreateCustomerRequest.Valid
            final CreateCustomerRequest createCustomerRequest)
            throws StackTestServiceException
    {
        AppbaseUtil.notNull(createCustomerRequest, "createCustomerRequest");

        final Timer.Context timer = createCustomerTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new CreateCustomerRequest.Validator(), createCustomerRequest);
            }

            final String email = createCustomerRequest.getEmail();
            final String photoId = createCustomerRequest.getPhotoId();

            final Customer customer = new Customer();
            customer.setEmail(email);

            final long customerId;
            if (!AppbaseUtil.isBlank(photoId)) {
                customer.setPhotoId(photoId);
                customerId = customerJDBA.createCustomer(email, photoId);
            } else {
                customerId = customerJDBA.createCustomer(email);
            }
            customer.setCustomerId(customerId);

            try {
                customerSIA.addDocument(customer);
            } catch (IOException e) {
                logger.error("Error while adding document: {}", customer, e);
            }

            try {
                customerASA.insertCustomer(customer);
            } catch (AerospikeAccessorException e) {
                logger.error("Error inserting customer: {}", customer, e);
            }

            final CreateCustomerResponse createCustomerResponse =
                    new CreateCustomerResponse(customer, createCustomerRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new CreateCustomerResponse.Validator(), createCustomerResponse);
            }
            return createCustomerResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Nullable
    @ValidateOnExecution
    @ReadCustomerResponse.Valid
    @Override
    public ReadCustomerResponse readCustomer(
            @ReadCustomerRequest.Valid
            final ReadCustomerRequest readCustomerRequest) throws StackTestServiceException
    {
        AppbaseUtil.notNull(readCustomerRequest, "readCustomerRequest");

        final Timer.Context timer = readByIdTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new ReadCustomerRequest.Validator(), readCustomerRequest);
            }

            final Long customerId = readCustomerRequest.getCustomerId();

            try {
                final Customer customer = customerASA.readCustomer(customerId);
                if (customer != null) {
                    logger.debug("Cache hit: {}", customer);

                    return new ReadCustomerResponse(customer, readCustomerRequest.getTraceId());
                }
                logger.debug("Cache miss: {}", customer);
            } catch (AerospikeAccessorException e) {
                logger.error("Error reading customer: {}", customerId, e);
            }

            final Customer customer = customerJDBA.readById(customerId);

            if (customer != null) {
                try {
                    customerASA.insertCustomer(customer);
                } catch (AerospikeAccessorException e) {
                    logger.error("Error inserting customer: {}", customer, e);
                }
            }

            final ReadCustomerResponse readCustomerResponse =
                    new ReadCustomerResponse(customer, readCustomerRequest.getTraceId());
            if (manualValidation && customer != null) {
                AppbaseUtil.checkedValidation(new ReadCustomerResponse.Validator(), readCustomerResponse);
            }
            return readCustomerResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Nullable
    @ValidateOnExecution
    @ReadCustomerOrdersResponse.Valid
    @Override
    public ReadCustomerOrdersResponse readCustomerOrders(
            @ReadCustomerOrdersRequest.Valid
            final ReadCustomerOrdersRequest readCustomerOrdersRequest) throws StackTestServiceException
    {
        AppbaseUtil.notNull(readCustomerOrdersRequest, "readCustomerOrdersRequest");

        final Timer.Context timer = readCustomerOrdersTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new ReadCustomerOrdersRequest.Validator(), readCustomerOrdersRequest);
            }

            final List<Order> orders = customerJDBA.readCustomerOrders(readCustomerOrdersRequest.getCustomerId());
            final ReadCustomerOrdersResponse readCustomerOrdersResponse =
                    new ReadCustomerOrdersResponse(orders, readCustomerOrdersRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new ReadCustomerOrdersResponse.Validator(), readCustomerOrdersResponse);
            }

            return readCustomerOrdersResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @UpdateCustomerResponse.Valid
    public UpdateCustomerResponse updateCustomer(
            @UpdateCustomerRequest.Valid
            final UpdateCustomerRequest updateCustomerRequest)
            throws StackTestServiceException
    {
        AppbaseUtil.notNull(updateCustomerRequest, "updateCustomerRequest");

        final Timer.Context timer = updateCustomerTimer.time();
        try {
            if (manualValidation) {
                logger.info("Update customer validation: {}", updateCustomerRequest);
                AppbaseUtil.checkedValidation(new UpdateCustomerRequest.Validator(), updateCustomerRequest);
            }

            final Customer customer = updateCustomerRequest.getCustomer();
            final int updated = customerJDBA.updateCustomer(customer);

            if (updated > 0) {
                try {
                    customerSIA.updateDocument(customer);
                } catch (IOException e) {
                    logger.error("Error while updating document: {}", customer, e);
                }

                try {
                    customerASA.deleteCustomer(customer.getCustomerId());
                } catch (AerospikeAccessorException e) {
                    logger.error("Error updating customer: {}", customer, e);
                }
            }

            final UpdateCustomerResponse updateCustomerResponse =
                    new UpdateCustomerResponse(customer, (updated > 0), updateCustomerRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new UpdateCustomerResponse.Validator(), updateCustomerResponse);
            }
            return updateCustomerResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @DeleteCustomerResponse.Valid
    public DeleteCustomerResponse deleteCustomer(
            @DeleteCustomerRequest.Valid
            final DeleteCustomerRequest deleteCustomerRequest)
            throws StackTestServiceException
    {
        AppbaseUtil.notNull(deleteCustomerRequest, "deleteCustomerRequest");

        final Timer.Context timer = deleteByIdTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new DeleteCustomerRequest.Validator(), deleteCustomerRequest);
            }

            final Long customerId = deleteCustomerRequest.getCustomerId();
            final ReadCustomerRequest readCustomerRequest =
                    new ReadCustomerRequest(customerId, deleteCustomerRequest.getTraceId());
            final ReadCustomerResponse readCustomerResponse = readCustomer(readCustomerRequest);
            if (readCustomerResponse == null) {
                throw new StackTestServiceException("Customer ID " + customerId + " not found");
            }
            final Customer customer = readCustomerResponse.getCustomer();
            if (customer == null) {
                throw new StackTestServiceException("Customer with ID " + customerId + " not found");
            }

            final boolean deleted = customerJDBA.deleteById(customerId);

            if (deleted) {
                try {
                    customerSIA.deleteDocument(String.valueOf(customerId));
                } catch (IOException e) {
                    logger.error("Error while deleting document: {}", customerId, e);
                }

                final String photoId = customer.getPhotoId();
                if (!AppbaseUtil.isBlank(photoId)) {
                    try {
                        customerPhotoOSA.deleteObject(photoId);
                    } catch (IOException e) {
                        logger.error("Error deleting from object store: [customerId={}, photoId={}]",
                                customerId, photoId, e);
                    }
                }

                try {
                    customerASA.deleteCustomer(customerId);
                } catch (AerospikeAccessorException e) {
                    logger.error("Error deleting from aerospike: {}", customerId, e);
                }
            }

            final DeleteCustomerResponse deleteCustomerResponse =
                    new DeleteCustomerResponse(deleted, deleteCustomerRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new DeleteCustomerResponse.Validator(), deleteCustomerResponse);
            }
            return deleteCustomerResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @VisibleForTesting
    public CustomerPhotoOSA getCustomerPhotoOSA() {
        return customerPhotoOSA;
    }
}
