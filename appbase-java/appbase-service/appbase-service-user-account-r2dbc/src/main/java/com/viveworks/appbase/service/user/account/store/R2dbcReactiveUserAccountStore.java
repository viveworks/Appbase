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

package com.viveworks.appbase.service.user.account.store;

import com.viveworks.appbase.client.service.r2dbc.accessor.reactive.ReactiveR2dbcAccessor;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AbstractStatelessAppbaseService;
import com.viveworks.appbase.service.password.PasswordService;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import reactor.core.publisher.Mono;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Natively non-blocking R2DBC-backed implementation of {@link ReactiveUserAccountStore} -- the
 * reactive JPA path's read twin, module-per-backing sibling of {@code JdbiUserAccountStore}
 * (blocking, {@code appbase-service-user-account-jdbi}). Reads go directly through the injected
 * {@link ReactiveR2dbcAccessor} -- no offload boundary, per the plan's boundary rule: native R2DBC
 * does not use the N01 offload boundary, and timeout lives in the accessor spine rather than at the
 * endpoint/method level here.
 *
 * <p>Read-only scope: only {@code read*}/{@code exists*} get reactive twins in this phase; create/
 * update/delete stay blocking-only ({@code JdbiUserAccountStore}).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ThreadSafe
public final class R2dbcReactiveUserAccountStore
        extends AbstractStatelessAppbaseService<R2dbcReactiveUserAccountStoreConfiguration>
        implements ReactiveUserAccountStore
{
    private final ReactiveR2dbcAccessor accessor;
    private final R2dbcUserAccountRowMapper rowMapper;
    private final String table;

    public R2dbcReactiveUserAccountStore(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final PasswordService passwordService,
            final ReactiveR2dbcAccessor accessor,
            final R2dbcReactiveUserAccountStoreConfiguration r2dbcReactiveUserAccountStoreConfiguration)
    {
        super(logService, metricService, mediatorService, serviceRegistry, r2dbcReactiveUserAccountStoreConfiguration);

        AppbaseUtil.notNull(passwordService, "passwordService");
        this.accessor = AppbaseUtil.notNull(accessor, "accessor");
        this.rowMapper = new R2dbcUserAccountRowMapper(passwordService);
        this.table = r2dbcReactiveUserAccountStoreConfiguration.getUserAccountTableName();
    }

    @Override
    protected void registerService() {
        serviceRegistry.registerAppbaseService(ReactiveUserAccountStore.class, this);
        serviceRegistry.registerAppbaseService(R2dbcReactiveUserAccountStore.class, this);
    }

    @Override
    public Mono<UserAccount> readByAppbaseUserIdReactive(final String appbaseUserId) {
        AppbaseUtil.notBlank(appbaseUserId, "appbaseUserId");

        final String sql = "SELECT * FROM " + table + " WHERE appbaseUserId = ?";
        return accessor.query("readByAppbaseUserIdReactive", sql, rowMapper::map, appbaseUserId).singleOrEmpty();
    }

    @Override
    public Mono<UserAccount> readByIdpUserIdReactive(final String idpUserId) {
        AppbaseUtil.notBlank(idpUserId, "idpUserId");

        final String sql = "SELECT * FROM " + table + " WHERE idpUserId = ?";
        return accessor.query("readByIdpUserIdReactive", sql, rowMapper::map, idpUserId).singleOrEmpty();
    }

    @Override
    public Mono<UserAccount> readByEmailReactive(final String email) {
        AppbaseUtil.notBlank(email, "email");

        final String sql = "SELECT * FROM " + table + " WHERE email = ?";
        return accessor.query("readByEmailReactive", sql, rowMapper::map, email).singleOrEmpty();
    }

    @Override
    public Mono<Boolean> existsByAppbaseUserIdReactive(final String appbaseUserId) {
        AppbaseUtil.notBlank(appbaseUserId, "appbaseUserId");

        final String sql = "SELECT appbaseUserId FROM " + table + " WHERE appbaseUserId = ?";
        return accessor.query("existsByAppbaseUserIdReactive", sql, (r, m) -> r.get(0, String.class), appbaseUserId)
                .hasElements();
    }

    @Override
    public Mono<Boolean> existsByIdpUserIdReactive(final String idpUserId) {
        AppbaseUtil.notBlank(idpUserId, "idpUserId");

        final String sql = "SELECT appbaseUserId FROM " + table + " WHERE idpUserId = ?";
        return accessor.query("existsByIdpUserIdReactive", sql, (r, m) -> r.get(0, String.class), idpUserId)
                .hasElements();
    }

    @Override
    public Mono<Boolean> existsByEmailReactive(final String email) {
        AppbaseUtil.notBlank(email, "email");

        final String sql = "SELECT appbaseUserId FROM " + table + " WHERE email = ?";
        return accessor.query("existsByEmailReactive", sql, (r, m) -> r.get(0, String.class), email)
                .hasElements();
    }
}
