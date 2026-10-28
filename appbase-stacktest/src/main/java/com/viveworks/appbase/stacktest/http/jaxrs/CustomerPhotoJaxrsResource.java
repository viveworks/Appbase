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
package com.viveworks.appbase.stacktest.http.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.service.StackTestServices;
import jakarta.inject.Inject;
import com.viveworks.appbase.stacktest.data.pojo.customer.DownloadCustomerPhotoByCustomerIdRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.DownloadCustomerPhotoByCustomerIdResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.DownloadCustomerPhotoByPhotoIdRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.DownloadCustomerPhotoByPhotoIdResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.UploadCustomerPhotoRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.UploadCustomerPhotoResponse;
import com.viveworks.appbase.stacktest.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Path(StackTestConstants.STACK_TEST_CUSTOMER_PHOTOS_RESOURCE)
public class CustomerPhotoJaxrsResource {
    private static final int STREAM_READ_N_BYTES = 1024;

    private final Logger logger;
    private final CustomerService customerService;

    private final Counter uploadPhotoDeleteFailedCounter;
    private final Counter downloadPhotoDeleteFailedCounter;

    @Inject
    public CustomerPhotoJaxrsResource(final StackTestServices stackTestServices) {
        final AppbaseCore appbaseCore = stackTestServices.getAppbaseServices().getCoreServices().appbaseCore();
        this.logger = appbaseCore.getLogService().getLogger(getClass());
        this.customerService = stackTestServices.getCustomerService();
        final MetricService metricService = appbaseCore.getMetricService();
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.uploadPhotoDeleteFailedCounter = metricRegistry.counter(
                MetricRegistry.name(getClass(), "uploadPhotoDeleteFailedCounter"));
        this.downloadPhotoDeleteFailedCounter = metricRegistry.counter(
                MetricRegistry.name(getClass(), "downloadPhotoDeleteFailedCounter"));
    }

    @POST
    @Path("/{customerId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upload customer photo")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = UploadCustomerPhotoResponse.class)),
            description = "Upload customer photo response",
            responseCode = "200")
    @Timed
    public Response uploadPhoto(
            @NotNull
            @FormDataParam("file")
            final InputStream uploadedInputStream,
            @NotNull
            @FormDataParam("file")
            final FormDataContentDisposition detail,
            @NotNull
            @PathParam("customerId")
            final Long customerId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        java.nio.file.Path destinationFile = null;

        // FIXME this is inefficient. The file is uploaded to a local file and then to object store.
        // It can be written directly to object store using input / output stream

        try {
            destinationFile = customerService.uploadDestinationFile(customerId);
            AppbaseServiceUtil.createTemporaryFile(destinationFile);

            try (final OutputStream outputStream = new FileOutputStream(destinationFile.toFile())) {
                int read = 0;
                final byte[] bytes = new byte[STREAM_READ_N_BYTES];
                while ((read = uploadedInputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
                outputStream.flush();
            }

            final UploadCustomerPhotoRequest uploadCustomerPhotoRequest =
                    new UploadCustomerPhotoRequest(customerId, destinationFile, AppbaseServiceUtil.traceId(traceId));
            final UploadCustomerPhotoResponse uploadCustomerPhotoResponse =
                    customerService.uploadCustomerPhoto(uploadCustomerPhotoRequest);
            return Response
                    .ok()
                    .entity(uploadCustomerPhotoResponse)
                    .build();
        } catch (Exception e) {
            logger.error("Upload photo={} error", customerId, e);
            throw new WebApplicationException(e);
        } finally {
            if (destinationFile != null) {
                try {
                    Files.deleteIfExists(destinationFile);
                } catch (IOException e) {
                    uploadPhotoDeleteFailedCounter.inc();
                    logger.error("Failed to delete {}", destinationFile, e);
                }
            }
        }
    }

    @GET
    @Path("/{customerId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Download customer photo by customer ID")
    @ApiResponse(
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM),
            description = "Download customer photo response",
            responseCode = "200")
    @Timed
    public Response downloadPhotoByCustomerId(
            @NotNull
            @PathParam("customerId")
            final Long customerId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        // FIXME this is inefficient. The file is downloaded from object store to a local file and then to client.
        // It can be written directly from object store to client using input / output stream
        try {
            final DownloadCustomerPhotoByCustomerIdRequest downloadCustomerPhotoByCustomerIdRequest =
                    new DownloadCustomerPhotoByCustomerIdRequest(customerId, AppbaseServiceUtil.traceId(traceId));

            final DownloadCustomerPhotoByCustomerIdResponse downloadCustomerPhotoByCustomerIdResponse =
                    customerService.downloadCustomerPhotoByCustomerId(downloadCustomerPhotoByCustomerIdRequest);

            if (downloadCustomerPhotoByCustomerIdResponse != null) {
                final InputStream inputStream = downloadCustomerPhotoByCustomerIdResponse.getInputStream();
                if (inputStream != null) {
                    return Response
                            .ok(inputStream, MediaType.APPLICATION_OCTET_STREAM)
                            .header(
                                    "Content-Disposition",
                                    "attachment; filename=\"" + customerId + "\"")
                            .build();
                }
            }

            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Download photo by customerId={} error", customerId, e);
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/photos/{photoId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Download customer photo by photo ID")
    @ApiResponse(
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM),
            description = "Download customer photo response",
            responseCode = "200")
    @Timed
    public Response downloadPhotoByPhotoId(
            @NotNull
            @PathParam("photoId")
            final String photoId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final DownloadCustomerPhotoByPhotoIdRequest downloadCustomerPhotoByPhotoIdRequest =
                    new DownloadCustomerPhotoByPhotoIdRequest(photoId, traceId);

            final DownloadCustomerPhotoByPhotoIdResponse downloadCustomerPhotoByPhotoIdResponse =
                    customerService.downloadCustomerPhotoByPhotoId(downloadCustomerPhotoByPhotoIdRequest);
            if (downloadCustomerPhotoByPhotoIdResponse != null) {
                final InputStream inputStream = downloadCustomerPhotoByPhotoIdResponse.getInputStream();
                if (inputStream != null) {
                    return Response
                            .ok(inputStream, MediaType.APPLICATION_OCTET_STREAM)
                            .header(
                                    "Content-Disposition",
                                    "attachment; filename=\"" + photoId + "\"")
                            .build();
                }
            }

            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Download photo by photoId={} error", photoId, e);
            throw new WebApplicationException(e);
        }
    }
}
