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
package com.viveworks.appbase.stacktest.service;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.stacktest.StackTestConstants;
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

import java.nio.file.Path;
import java.util.UUID;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface CustomerService {
    default Path uploadDestinationFile(final Long customerId) {
        AppbaseUtil.notNull(customerId, "customerId");

        return Path.of(
                StackTestConstants.CUSTOMER_PHOTO_UPLOAD_PATHNAME,
                String.valueOf(customerId),
                UUID.randomUUID().toString());
    }

    default Path downloadByCustomerIdDestinationFile(final Long customerId) {
        AppbaseUtil.notNull(customerId, "customerId");

        return Path.of(
                StackTestConstants.CUSTOMER_PHOTO_BY_CUSTOMER_ID_DOWNLOAD_PATHNAME,
                String.valueOf(customerId),
                UUID.randomUUID().toString());
    }

    default Path downloadByPhotoIdDestinationFile(final String photoId) {
        AppbaseUtil.notNull(photoId, "photoId");

        return Path.of(
                StackTestConstants.CUSTOMER_PHOTO_BY_PHOTO_ID_DOWNLOAD_PATHNAME,
                photoId,
                UUID.randomUUID().toString());
    }

    CreateCustomerResponse createCustomer(
            CreateCustomerRequest createCustomerRequest) throws StackTestServiceException;

    ReadCustomerResponse readCustomer(
            ReadCustomerRequest readCustomerRequest) throws StackTestServiceException;

    ReadCustomersResponse readCustomers(ReadCustomersRequest readCustomersRequest)
            throws StackTestServiceException;

    ReadCustomerOrdersResponse readCustomerOrders(
            ReadCustomerOrdersRequest readCustomerOrdersRequest) throws StackTestServiceException;

    UpdateCustomerResponse updateCustomer(
            UpdateCustomerRequest updateCustomerRequest) throws StackTestServiceException;

    DeleteCustomerResponse deleteCustomer(
            DeleteCustomerRequest deleteCustomerRequest) throws StackTestServiceException;

    CustomerStatisticsResponse customerStatistics(
            CustomerStatisticsRequest customerStatisticsRequest)
            throws StackTestServiceException;

    SearchCustomersResponse searchCustomers(
            SearchCustomersRequest searchCustomersRequest) throws StackTestServiceException;

    UploadCustomerPhotoResponse uploadCustomerPhoto(
            UploadCustomerPhotoRequest uploadCustomerPhotoRequest) throws StackTestServiceException;

    DownloadCustomerPhotoByCustomerIdResponse downloadCustomerPhotoByCustomerId(
            DownloadCustomerPhotoByCustomerIdRequest downloadCustomerPhotoByCustomerIdRequest)
            throws StackTestServiceException;

    DownloadCustomerPhotoByPhotoIdResponse downloadCustomerPhotoByPhotoId(
            DownloadCustomerPhotoByPhotoIdRequest downloadCustomerPhotoByPhotoIdRequest)
            throws StackTestServiceException;
}
