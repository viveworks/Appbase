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

package com.viveworks.appbase.core.service.jmx;

import com.viveworks.appbase.core.jmx.PermissionDeniedException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.PropertyCastException;
import com.viveworks.appbase.core.service.log.LogService;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AUD-08 (CWE-276, owner decision DQ-1, 2026-10-01): regression coverage proving every mutating
 * {@link AppbaseServiceMXBean} operation (reload, shutdown) is DENIED by default at each of the
 * three independent permission tiers — global ({@code GLOBAL_ALL_OPERATIONS}), per-bean
 * ({@code ALL_OPERATIONS}), and the operation-specific permission — and that explicit configuration
 * still permits it.
 *
 * <p>{@link AbstractAppbaseServiceMXBean#checkPermission} short-circuits as soon as any one tier
 * denies, so a naive "it throws when nothing is configured" test cannot tell a correctly-deny-by-
 * default tier from one whose default silently reverted to permit: the other two tiers would still
 * deny and the test would still pass. Each tier below is therefore exercised in ISOLATION — the
 * other two tiers explicitly permitted via configuration, only the tier under test left unconfigured
 * — so a mutation to that one tier's default is independently detectable.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AbstractAppbaseServiceMXBeanPermissionTest {

    /** Concrete configuration type so the YAML overload can deserialize a minimal document. */
    private static final class TestConfiguration implements AppbaseConfiguration {
    }

    /** Minimal concrete {@link AbstractAppbaseServiceMXBean}; the base class has no abstract members. */
    private static final class FixtureMXBean
            extends AbstractAppbaseServiceMXBean<AppbaseService, TestConfiguration>
    {
        FixtureMXBean(final LogService logService, final AppbaseService appbaseService,
                final ConfigurationService configurationService)
        {
            super(logService, appbaseService, TestConfiguration.class, configurationService);
        }
    }

    private AppbaseService appbaseService;

    private FixtureMXBean newBean(final ConfigurationService configurationService) {
        final LogService logService = mock(LogService.class);
        when(logService.getLogger(any())).thenReturn(LoggerFactory.getLogger(FixtureMXBean.class));
        appbaseService = mock(AppbaseService.class);
        when(appbaseService.getConfiguration()).thenReturn(mock(AppbaseConfiguration.class));
        return new FixtureMXBean(logService, appbaseService, configurationService);
    }

    /** Every property key, unless overridden below, answers "not configured" (null). */
    private ConfigurationService unconfigured() throws PropertyCastException {
        final ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.getBoolean(anyString())).thenReturn(null);
        return configurationService;
    }

    /**
     * {@link com.viveworks.appbase.core.service.configuration.util.ConfigurationUtil#config} reads
     * the environment-variable key, then the property key; a non-null property value overrides a
     * non-null environment value, but a {@code null} property (as every other key stays here, via
     * {@link #unconfigured}) never overwrites an already-permitted environment value. Stubbing the
     * property key alone is therefore sufficient to permit a tier in this fixture.
     */
    private void permit(final ConfigurationService configurationService, final String propertyKey)
            throws PropertyCastException
    {
        when(configurationService.getBoolean(eq(propertyKey))).thenReturn(Boolean.TRUE);
    }

    /**
     * Stubs the environment-variable key alone, leaving the paired property key at
     * {@link #unconfigured}'s {@code null} — proves a tier can be permitted through the
     * environment-variable channel by itself, not just via the property channel {@link #permit} uses.
     */
    private void permitViaEnv(final ConfigurationService configurationService, final String environmentKey)
            throws PropertyCastException
    {
        when(configurationService.getBoolean(eq(environmentKey))).thenReturn(Boolean.TRUE);
    }

    // ---- shutdown: each tier in isolation ---------------------------------------------------------

    @Test
    void shutdown_nothingConfigured_isDeniedAtEveryTier() throws Exception {
        final FixtureMXBean bean = newBean(unconfigured());
        assertThrows(PermissionDeniedException.class, bean::shutdown);
    }

    @Test
    void shutdown_globalTierUnconfigured_otherTiersPermitted_isDenied() throws Exception {
        final ConfigurationService configurationService = unconfigured();
        final FixtureMXBean bean = newBean(configurationService);
        permit(configurationService, bean.jmxBeanAllOperationsPermissionProperty());
        permit(configurationService, AbstractAppbaseServiceMXBean.SHUTDOWN_OPERATION_PERMISSION_PROPERTY);
        // AbstractAppbaseServiceMXBean.GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY left unconfigured.
        assertThrows(PermissionDeniedException.class, bean::shutdown);
    }

    @Test
    void shutdown_beanAllOperationsTierUnconfigured_otherTiersPermitted_isDenied() throws Exception {
        final ConfigurationService configurationService = unconfigured();
        final FixtureMXBean bean = newBean(configurationService);
        permit(configurationService, AbstractAppbaseServiceMXBean.GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY);
        permit(configurationService, AbstractAppbaseServiceMXBean.SHUTDOWN_OPERATION_PERMISSION_PROPERTY);
        // bean.jmxBeanAllOperationsPermissionProperty() left unconfigured.
        assertThrows(PermissionDeniedException.class, bean::shutdown);
    }

    @Test
    void shutdown_operationSpecificTierUnconfigured_otherTiersPermitted_isDenied() throws Exception {
        final ConfigurationService configurationService = unconfigured();
        final FixtureMXBean bean = newBean(configurationService);
        permit(configurationService, AbstractAppbaseServiceMXBean.GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY);
        permit(configurationService, bean.jmxBeanAllOperationsPermissionProperty());
        // SHUTDOWN_OPERATION_PERMISSION_PROPERTY left unconfigured.
        assertThrows(PermissionDeniedException.class, bean::shutdown);
    }

    @Test
    void shutdown_everyTierExplicitlyPermitted_succeeds() throws Exception {
        final ConfigurationService configurationService = unconfigured();
        final FixtureMXBean bean = newBean(configurationService);
        permit(configurationService, AbstractAppbaseServiceMXBean.GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY);
        permit(configurationService, bean.jmxBeanAllOperationsPermissionProperty());
        permit(configurationService, AbstractAppbaseServiceMXBean.SHUTDOWN_OPERATION_PERMISSION_PROPERTY);
        assertDoesNotThrow(bean::shutdown);
    }

    /**
     * AUD-08's environment-fallback fix ({@code ConfigurationUtil.config}) is only proven at the
     * standalone unit level by {@code ConfigurationUtilTest}; this closes that gap at the bean's own
     * dispatch path, so a future mis-wiring of which {@code *_ENV} constant feeds a given tier inside
     * {@link AbstractAppbaseServiceMXBean#checkPermission} would be caught here.
     */
    @Test
    void shutdown_everyTierPermittedViaEnvironmentVariableAlone_succeeds() throws Exception {
        final ConfigurationService configurationService = unconfigured();
        final FixtureMXBean bean = newBean(configurationService);
        permitViaEnv(configurationService, AbstractAppbaseServiceMXBean.GLOBAL_ALL_OPERATIONS_PERMISSION_ENV);
        permitViaEnv(configurationService, bean.jmxBeanAllOperationsPermissionEnv());
        permitViaEnv(configurationService, AbstractAppbaseServiceMXBean.SHUTDOWN_OPERATION_PERMISSION_ENV);
        assertDoesNotThrow(bean::shutdown);
    }

    // ---- reload(): each tier in isolation ---------------------------------------------------------

    @Test
    void reload_nothingConfigured_isDeniedAtEveryTier() throws Exception {
        final FixtureMXBean bean = newBean(unconfigured());
        assertThrows(PermissionDeniedException.class, () -> bean.reload());
    }

    @Test
    void reload_globalTierUnconfigured_otherTiersPermitted_isDenied() throws Exception {
        final ConfigurationService configurationService = unconfigured();
        final FixtureMXBean bean = newBean(configurationService);
        permit(configurationService, bean.jmxBeanAllOperationsPermissionProperty());
        permit(configurationService, AbstractAppbaseServiceMXBean.RELOAD_OPERATIONS_PERMISSION_PROPERTY);
        assertThrows(PermissionDeniedException.class, () -> bean.reload());
    }

    @Test
    void reload_beanAllOperationsTierUnconfigured_otherTiersPermitted_isDenied() throws Exception {
        final ConfigurationService configurationService = unconfigured();
        final FixtureMXBean bean = newBean(configurationService);
        permit(configurationService, AbstractAppbaseServiceMXBean.GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY);
        permit(configurationService, AbstractAppbaseServiceMXBean.RELOAD_OPERATIONS_PERMISSION_PROPERTY);
        assertThrows(PermissionDeniedException.class, () -> bean.reload());
    }

    @Test
    void reload_operationSpecificTierUnconfigured_otherTiersPermitted_isDenied() throws Exception {
        final ConfigurationService configurationService = unconfigured();
        final FixtureMXBean bean = newBean(configurationService);
        permit(configurationService, AbstractAppbaseServiceMXBean.GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY);
        permit(configurationService, bean.jmxBeanAllOperationsPermissionProperty());
        assertThrows(PermissionDeniedException.class, () -> bean.reload());
    }

    @Test
    void reload_everyTierExplicitlyPermitted_succeeds() throws Exception {
        final ConfigurationService configurationService = unconfigured();
        final FixtureMXBean bean = newBean(configurationService);
        permit(configurationService, AbstractAppbaseServiceMXBean.GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY);
        permit(configurationService, bean.jmxBeanAllOperationsPermissionProperty());
        permit(configurationService, AbstractAppbaseServiceMXBean.RELOAD_OPERATIONS_PERMISSION_PROPERTY);
        assertDoesNotThrow(() -> bean.reload());
    }

    // ---- reload(String): the YAML overload must enforce the same permission boundary ---------------

    @Test
    void reloadWithYaml_nothingConfigured_isDenied() throws Exception {
        final FixtureMXBean bean = newBean(unconfigured());
        assertThrows(PermissionDeniedException.class, () -> bean.reload("{}"));
    }

    @Test
    void reloadWithYaml_everyTierExplicitlyPermitted_succeeds() throws Exception {
        final ConfigurationService configurationService = unconfigured();
        final FixtureMXBean bean = newBean(configurationService);
        permit(configurationService, AbstractAppbaseServiceMXBean.GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY);
        permit(configurationService, bean.jmxBeanAllOperationsPermissionProperty());
        permit(configurationService, AbstractAppbaseServiceMXBean.RELOAD_OPERATIONS_PERMISSION_PROPERTY);
        assertDoesNotThrow(() -> bean.reload("{}"));
    }

    // ---- read-only operation: unaffected by AUD-08 -----------------------------------------------

    /**
     * internal work item 2554's own scope clarification requires a test proving read-only operations stay permitted
     * when nothing is configured: {@link AbstractAppbaseServiceMXBean#getStatus()} never calls
     * {@link AbstractAppbaseServiceMXBean#checkPermission}, so none of the AUD-08 deny-by-default
     * tiers apply to it.
     */
    @Test
    void getStatus_nothingConfigured_stillSucceeds() throws Exception {
        final FixtureMXBean bean = newBean(unconfigured());
        when(appbaseService.getStatus()).thenReturn(ServiceStatus.STARTED);
        assertEquals(ServiceStatus.STARTED, assertDoesNotThrow(bean::getStatus));
    }
}
