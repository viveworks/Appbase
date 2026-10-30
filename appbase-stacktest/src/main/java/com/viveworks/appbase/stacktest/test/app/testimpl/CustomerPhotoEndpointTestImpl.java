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
package com.viveworks.appbase.stacktest.test.app.testimpl;

import com.viveworks.appbase.stacktest.test.TestUtil;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.StackTestRuntimeException;
import com.viveworks.appbase.stacktest.accessor.s3.CustomerPhotoOSA;
import com.viveworks.appbase.stacktest.data.model.Customer;
import com.viveworks.appbase.stacktest.data.pojo.customer.CreateCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.CreateCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.DeleteCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.UploadCustomerPhotoResponse;
import com.viveworks.appbase.stacktest.service.CustomerService;
import com.viveworks.appbase.stacktest.service.DefaultCustomerService;
import com.viveworks.appbase.stacktest.service.StackTestServiceException;
import com.viveworks.appbase.stacktest.service.StackTestServices;
import com.viveworks.appbase.stacktest.test.AbstractTest;
import com.viveworks.appbase.stacktest.test.app.jaxrs.CustomerPhotoJaxrsResourceTest;
import com.viveworks.appbase.stacktest.test.app.servlet.CustomerPhotoServletTest;
import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CustomerPhotoEndpointTestImpl extends AbstractTest {
    private final Class<?> testType;

    private final StackTestServices stackTestServices;

    private final String url;

    public CustomerPhotoEndpointTestImpl(
            final Class<?> testType,
            final StackTestServices stackTestServices,
            final HttpClient httpClient,
            final CookieStore cookieStore,
            final String baseUrl)
    {
        super(stackTestServices.getAppbaseServices(), httpClient, cookieStore, baseUrl);

        AppbaseUtil.notNull(testType, "testType");
        checkTestType(testType, CustomerPhotoJaxrsResourceTest.class, CustomerPhotoServletTest.class);

        this.testType = testType;
        this.stackTestServices = stackTestServices;
        if (isRestApiTest()) {
            url = baseUrl + StackTestConstants.STACK_TEST_CUSTOMER_PHOTOS_RESOURCE;
        } else {
            url = baseUrl + StackTestConstants.STACK_TEST_CUSTOMER_PHOTO_PATH;
        }
    }

    public void uploadPhotoTestImpl() throws Exception {
        final String testName = "uploadPhotoTest";
        startTest(testType, testName);

        UploadedPhoto uploadedPhoto = null;
        try {
            uploadedPhoto = uploadPhoto(testName);

            final CustomerPhotoOSA customerPhotoOSA =
                    ((DefaultCustomerService) customerService()).getCustomerPhotoOSA();
            final byte[] bytes = customerPhotoOSA.readObjectAsBytes(uploadedPhoto.getPhotoId());
            checkNotNull(bytes, testName, "bytes");
            final String fileText = new String(bytes, StandardCharsets.UTF_8);
            checkEquals(uploadedPhoto.getFileText(), fileText, testName, "fileText");
        } finally {
            cleanup(uploadedPhoto);
        }

        endTest(testType, testName);
    }

    public void downloadPhotoByCustomerIdTestImpl() throws Exception {
        final String testName = "downloadPhotoByCustomerIdTest";
        startTest(testType, testName);

        UploadedPhoto uploadedPhoto = null;
        try {
            uploadedPhoto = uploadPhoto(testName);

            String finalUrl;
            if (isRestApiTest()) {
                finalUrl = url + "/" + uploadedPhoto.getCustomerId();
            } else {
                finalUrl = url + "?customer_id=" + uploadedPhoto.getCustomerId();
            }

            final HttpEntity httpEntity = httpGet(finalUrl);
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            httpEntity.writeTo(byteArrayOutputStream);
            final String fileText = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
            checkEquals(uploadedPhoto.getFileText(), fileText, testName, "fileText");
        } finally {
            cleanup(uploadedPhoto);
        }

        endTest(testType, testName);
    }

    public void downloadPhotoByPhotoIdTestImpl() throws Exception {
        final String testName = "downloadPhotoByPhotoIdTest";
        startTest(testType, testName);

        UploadedPhoto uploadedPhoto = null;
        try {
            uploadedPhoto = uploadPhoto(testName);

            String finalUrl;
            if (isRestApiTest()) {
                finalUrl = url + "/photos/" + uploadedPhoto.getPhotoId();
            } else {
                finalUrl = url + "?photo_id=" + uploadedPhoto.getPhotoId();
            }

            final HttpEntity httpEntity = httpGet(finalUrl);
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            httpEntity.writeTo(byteArrayOutputStream);
            final String fileText = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
            checkEquals(uploadedPhoto.getFileText(), fileText, testName, "fileText");
        } finally {
            cleanup(uploadedPhoto);
        }

        endTest(testType, testName);
    }


    // ..............................
    // Helpers .....
    // ..............................

    private CustomerService customerService() {
        return stackTestServices.getCustomerService();
    }

    private boolean isRestApiTest() {
        return CustomerPhotoJaxrsResourceTest.class.isAssignableFrom(testType);
    }

    private UploadedPhoto uploadPhoto(final String testName) throws Exception {
        final String email = "test" + TestUtil.randomId() + "@test.com";
        final DefaultCustomerService customerService = (DefaultCustomerService) customerService();
        final Path tmpDir = Files.createTempDirectory(getClass().getSimpleName());
        final Path tmpFile = Files.createTempFile(tmpDir, getClass().getSimpleName(), ".txt");

        final File file = tmpFile.toFile();
        file.deleteOnExit();
        tmpDir.toFile().deleteOnExit();

        final String text = "Dummy text " + (Math.random() * 10_000_000);
        Files.writeString(tmpFile, text);

        final CreateCustomerRequest createCustomerRequest1 =
                new CreateCustomerRequest(email, null, null);
        final CreateCustomerResponse createCustomerResponse1 =
                customerService.createCustomer(createCustomerRequest1);
        checkNotNull(createCustomerResponse1, testName, "createCustomerResponse1");
        final Customer createdCustomer1 = createCustomerResponse1.getCustomer();
        checkNotNull(createdCustomer1, testName, "createdCustomer1");

        final String finalUrl;
        if (isRestApiTest()) {
            finalUrl = url + "/" + createdCustomer1.getCustomerId();
        } else {
            finalUrl = url + "?id=" + createdCustomer1.getCustomerId();
        }

        final HttpPost httpPost = new HttpPost(finalUrl);
        final HttpEntity httpEntity = MultipartEntityBuilder
                .create()
                .addBinaryBody("file", file, ContentType.MULTIPART_FORM_DATA, file.getName())
                .build();
        httpPost.setEntity(httpEntity);

        final UploadCustomerPhotoResponse uploadCustomerPhotoResponse =
                sendHttpRequest(httpPost, UploadCustomerPhotoResponse.class);
        checkNotNull(uploadCustomerPhotoResponse, testName, "uploadCustomerPhotoResponse");
        final String destinationPath = uploadCustomerPhotoResponse.getDestinationPath();
        checkNotBlank(destinationPath, testName, "destinationPath");

        final ReadCustomerRequest readCustomerRequest =
                new ReadCustomerRequest(createdCustomer1.getCustomerId(), null);
        final ReadCustomerResponse readCustomerResponse = customerService.readCustomer(readCustomerRequest);
        checkNotNull(readCustomerResponse, testName, "readCustomerResponse");
        final Customer customer = readCustomerResponse.getCustomer();
        checkNotNull(customer, testName, "customer");
        final String photoId = customer.getPhotoId();
        checkNotBlank(photoId, testName, "photoId");

        return new UploadedPhoto(createdCustomer1.getCustomerId(), photoId, text, tmpFile, tmpDir);
    }

    private void cleanup(final UploadedPhoto uploadedPhoto) throws StackTestServiceException, IOException {
        if (uploadedPhoto == null) {
            return;
        }

        final DeleteCustomerRequest deleteCustomerRequest =
                new DeleteCustomerRequest(uploadedPhoto.getCustomerId(), null);
        customerService().deleteCustomer(deleteCustomerRequest);

        final Path tmpFile = uploadedPhoto.getTemporaryFile();
        if (tmpFile != null) {
            try {
                Files.deleteIfExists(tmpFile);
            } catch (IOException e) {
                logger.error("Error deleting temporary file: {}", tmpFile.toFile().getAbsolutePath(), e);
            }
        }

        final Path tmpDir = uploadedPhoto.getTemporaryDir();
        if (tmpDir != null) {
            Files.list(tmpDir).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new StackTestRuntimeException("Failed to delete: " + path.toFile().getAbsolutePath(), e);
                }
            });
            Files.deleteIfExists(tmpDir);
        }
    }

    private static class UploadedPhoto {
        private final Long customerId;
        private final String photoId;
        private final String fileText;
        private final Path temporaryFile;
        private final Path temporaryDir;

        public UploadedPhoto(
                final Long customerId,
                final String photoId,
                final String fileText,
                final Path temporaryFile,
                final Path temporaryDir)
        {
            this.customerId = customerId;
            this.photoId = photoId;
            this.fileText = fileText;
            this.temporaryFile = temporaryFile;
            this.temporaryDir = temporaryDir;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public String getPhotoId() {
            return photoId;
        }

        public String getFileText() {
            return fileText;
        }

        public Path getTemporaryFile() {
            return temporaryFile;
        }

        public Path getTemporaryDir() {
            return temporaryDir;
        }
    }
}
