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
package com.viveworks.appbase.stacktest.http.servlet;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.web.endpoint.servlet.FileDownloadInfo;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.data.pojo.customer.DownloadCustomerPhotoByCustomerIdRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.DownloadCustomerPhotoByCustomerIdResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.DownloadCustomerPhotoByPhotoIdRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.DownloadCustomerPhotoByPhotoIdResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.UploadCustomerPhotoRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.UploadCustomerPhotoResponse;
import com.viveworks.appbase.stacktest.service.CustomerService;
import com.viveworks.appbase.stacktest.service.StackTestServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 10, // 10 MB
        maxFileSize = 1024 * 1024 * 20, // 20 MB
        maxRequestSize = 1024 * 1024 * 100) // 100 MB
public class CustomerPhotoServlet extends AbstractStackTestServlet {
    private static final long serialVersionUID = -9183020430770076925L;

    private CustomerService customerService;


    @Override
    public void init() {
        super.init();

        customerService = stackTestServices().getCustomerService();
    }

    @Override
    protected void doGet(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");

        doFileDownload(httpServletRequest, httpServletResponse);
    }

    @Override
    protected void doPost(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");

        doFileUpload(httpServletRequest, httpServletResponse);
    }

    @Nullable
    @Override
    protected FileDownloadInfo fileDownloadInfo(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse,
            final String traceId)
            throws ServletException, IOException
    {
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");
        AppbaseUtil.notBlank(traceId, "traceId");

        final String customerId = AppbaseServiceUtil.parameter(StackTestConstants.CUSTOMER_ID_PARAM, httpServletRequest);
        if (!AppbaseUtil.isBlank(customerId)) {
            final DownloadCustomerPhotoByCustomerIdRequest downloadCustomerPhotoByCustomerIdRequest
                    = new DownloadCustomerPhotoByCustomerIdRequest(Long.parseLong(customerId), traceId);
            try {
                final DownloadCustomerPhotoByCustomerIdResponse downloadCustomerPhotoByCustomerIdResponse =
                        customerService.downloadCustomerPhotoByCustomerId(downloadCustomerPhotoByCustomerIdRequest);
                if (downloadCustomerPhotoByCustomerIdResponse != null) {
                    return new FileDownloadInfo(
                            downloadCustomerPhotoByCustomerIdResponse.getFileName(),
                            downloadCustomerPhotoByCustomerIdResponse.getMimeType(),
                            downloadCustomerPhotoByCustomerIdResponse.getTraceId(),
                            downloadCustomerPhotoByCustomerIdResponse.getInputStream());
                }
            } catch (StackTestServiceException e) {
                throw new ServletException(e);
            }
        }

        final String photoId = AppbaseServiceUtil.parameter(StackTestConstants.PHOTO_ID_PARAM, httpServletRequest);
        if (!AppbaseUtil.isBlank(photoId)) {
            final DownloadCustomerPhotoByPhotoIdRequest downloadCustomerPhotoByPhotoIdRequest =
                    new DownloadCustomerPhotoByPhotoIdRequest(photoId, traceId);
            try {
                final DownloadCustomerPhotoByPhotoIdResponse downloadCustomerPhotoByPhotoIdResponse =
                        customerService.downloadCustomerPhotoByPhotoId(downloadCustomerPhotoByPhotoIdRequest);
                if (downloadCustomerPhotoByPhotoIdResponse != null) {
                    return new FileDownloadInfo(
                            downloadCustomerPhotoByPhotoIdResponse.getFileName(),
                            downloadCustomerPhotoByPhotoIdResponse.getMimeType(),
                            downloadCustomerPhotoByPhotoIdResponse.getTraceId(),
                            downloadCustomerPhotoByPhotoIdResponse.getInputStream());
                }
            } catch (StackTestServiceException e) {
                throw new ServletException(e);
            }
        }

        return null;
    }

    protected Path uploadDestinationFile(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        AppbaseUtil.notBlank(traceId, "traceId");
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");

        final Long customerId = longParameter(StackTestConstants.ID_PARAM, httpServletRequest, httpServletResponse);
        return customerService.uploadDestinationFile(customerId);
    }

    @SuppressWarnings("unchecked")
    protected Class<UploadCustomerPhotoResponse> fileUploadResponseType() {
        return UploadCustomerPhotoResponse.class;
    }

    @SuppressWarnings("unchecked")
    protected UploadCustomerPhotoResponse postWriteUpload(
            final String traceId,
            final Path uploadDestinationFile,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        AppbaseUtil.notBlank(traceId, "traceId");
        AppbaseUtil.notNull(uploadDestinationFile, "uploadDestinationFile");
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");
        AppbaseUtil.notNull(httpServletResponse, "httpServletResponse");

        final Long customerId = longParameter(StackTestConstants.ID_PARAM, httpServletRequest, httpServletResponse);
        final UploadCustomerPhotoRequest uploadCustomerPhotoRequest =
                new UploadCustomerPhotoRequest(customerId, uploadDestinationFile, traceId);
        try {
            return customerService.uploadCustomerPhoto(uploadCustomerPhotoRequest);
        } catch (StackTestServiceException e) {
            throw new AppbaseRuntimeException(e);
        }
    }
}
