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

package com.viveworks.appbase.core.service.mediator.message;

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.objectpool.Poolable;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a service status message.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@NotThreadSafe
public class ServiceStatusMessage extends AbstractContextualMessage implements Poolable {
    private Class<? extends AppbaseService> serviceType;
    private ServiceStatus serviceStatus;

    public ServiceStatusMessage() {
    }

    public ServiceStatusMessage(final ServiceStatusMessage serviceStatusMessage) {
        AppbaseUtil.notNull(serviceStatusMessage, "serviceStatusMessage");

        serviceType = serviceStatusMessage.serviceType;
        serviceStatus = serviceStatusMessage.serviceStatus;

        final Map<String, String> context = serviceStatusMessage.getContext();
        if (context != null) {
            addAllContext(context);
        }
    }

    /**
     * Set all fields.
     *
     * @param timestamp Timestamp of the message
     * @param serviceType {@link AppbaseService} type
     * @param serviceStatus {@link ServiceStatus}
     */
    public void setAll(
            final long timestamp,
            final Class<? extends AppbaseService> serviceType,
            final ServiceStatus serviceStatus)
    {
        setTimestamp(timestamp);
        setServiceType(serviceType);
        setServiceStatus(serviceStatus);
    }

    public Class<? extends AppbaseService> getServiceType() {
        return serviceType;
    }

    public void setServiceType(final Class<? extends AppbaseService> serviceType) {
        AppbaseUtil.notNull(serviceType, "serviceType");

        this.serviceType = serviceType;
    }

    public ServiceStatus getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(final ServiceStatus serviceStatus) {
        AppbaseUtil.notNull(serviceStatus, "serviceStatus");

        this.serviceStatus = serviceStatus;
    }

    @Override
    public boolean isValid() {
        return serviceType != null && serviceStatus != null && super.isValid();
    }

    @Override
    public boolean isClean() {
        return serviceType == null && serviceStatus == null && super.isClean();
    }


    @Override
    public void clean() {
        serviceType = null;
        serviceStatus = null;
        super.clean();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final ServiceStatusMessage that = (ServiceStatusMessage) o;
        return Objects.equals(serviceType, that.serviceType) &&
                serviceStatus == that.serviceStatus &&
                super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), serviceType, serviceStatus);
    }

    @Override
    public String toString() {
        return "ServiceStatusMessage{" +
                "serviceType=" + serviceType +
                ", serviceStatus=" + serviceStatus +
                '}' + super.toString();
    }
}
