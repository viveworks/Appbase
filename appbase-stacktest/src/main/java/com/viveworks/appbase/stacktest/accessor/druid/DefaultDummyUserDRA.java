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
package com.viveworks.appbase.stacktest.accessor.druid;

import com.viveworks.appbase.client.service.druid.sql.DruidSqlClientService;
import com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlParameter;
import com.viveworks.appbase.client.service.druid.sql.client.DruidResultSet;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlClientException;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlParameter;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlType;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.stacktest.StackTestConstants;
import org.eclipse.microprofile.metrics.Timer;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultDummyUserDRA implements DummyUserDRA {
    private static final int USER_ID = 3;
    private static final String QUERY =
            "SELECT * FROM " + StackTestConstants.DRUID_DATASOURCE_NAME + " WHERE userId=?";

    private final DruidSqlClientService druidSqlClientService;
    private final Timer readDummyUserTimer;

    public DefaultDummyUserDRA(final AppbaseClientServices clientServices) {
        AppbaseUtil.notNull(clientServices, "clientServices");

        this.druidSqlClientService = clientServices.get(DruidSqlClientService.class);
        final MetricService metricService = clientServices.getCoreServices().appbaseCore().getMetricService();
        this.readDummyUserTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "readDummyUserTimer"));
    }

    @Override
    public DummyUser readDummyUser() throws DruidSqlClientException {
        final Timer.Context timer = readDummyUserTimer.time();
        try {
            final DruidSqlParameter druidSqlParameter = new DefaultDruidSqlParameter(1, USER_ID, DruidSqlType.INTEGER);
            final DruidResultSet druidResultSet = druidSqlClientService
                    .getClient()
                    .executeParameterizedQuery(QUERY, List.of(druidSqlParameter));

            if (druidResultSet.next()) {
                final DummyUser dummyUser = new DummyUser();
                dummyUser.setUserId(druidResultSet.getVarchar("userId"));
                dummyUser.setName(druidResultSet.getVarchar("name"));
                dummyUser.setEmail(druidResultSet.getVarchar("email"));
                dummyUser.setCountry(druidResultSet.getVarchar("country"));

                return dummyUser;
            }
        } finally {
            timer.stop();
        }

        throw new DruidSqlClientException("Could not find user with ID=" + USER_ID);
    }
}
