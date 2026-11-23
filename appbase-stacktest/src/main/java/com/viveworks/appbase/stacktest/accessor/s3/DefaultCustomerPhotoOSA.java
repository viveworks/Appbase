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
package com.viveworks.appbase.stacktest.accessor.s3;

import com.viveworks.appbase.client.cloud.aws.s3.accessor.AbstractObjectStoreAccessor;
import com.viveworks.appbase.client.cloud.aws.s3.accessor.ObjectStoreAccessor;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.client.cloud.aws.s3.S3ClientsService;
import com.viveworks.appbase.stacktest.StackTestConstants;

import java.io.File;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultCustomerPhotoOSA extends AbstractObjectStoreAccessor implements CustomerPhotoOSA {
    public DefaultCustomerPhotoOSA(final AppbaseClientServices clientServices) {
        super(clientServices.getCoreServices().appbaseCore(),
                clientServices.get(S3ClientsService.class),
                StackTestConstants.S3_BUCKET_NAME,
                StackTestConstants.OBJECT_STORE_READ_DESTINATION_BASE_PATHNAME);
    }

    @Override
    public String readDestinationSubDirectoryName() {
        return "customer" + File.separator + "photo";
    }

    @Override
    public String keyPrefix() {
        return "customer" + ObjectStoreAccessor.S3_BUCKET_SEPARATOR + "photo" + ObjectStoreAccessor.S3_BUCKET_SEPARATOR;
    }
}
