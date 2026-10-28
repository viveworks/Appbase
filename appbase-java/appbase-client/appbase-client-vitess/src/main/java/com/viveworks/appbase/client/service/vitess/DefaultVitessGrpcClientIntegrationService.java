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

package com.viveworks.appbase.client.service.vitess;

import com.google.common.annotations.VisibleForTesting;
import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.vitess.configuration.CallCredentialsConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.CallerIdConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.ContextConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.CustomGrpcClientFactoryConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.DefaultVitessGrpcClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.ExecuteOptionsConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.LoadBalancerFactoryConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.NameResolverFactoryConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.RetryingInterceptorConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.StaticAuthCredentialsConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.TargetConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.TlsOptionsConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.VitessTransactionIsolation;
import com.viveworks.appbase.client.service.vitess.configuration.VitessWorkload;
import com.viveworks.appbase.client.service.vitess.configuration.VtSessionConfiguration;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import io.grpc.CallCredentials;
import io.grpc.LoadBalancer;
import io.grpc.NameResolver;
import io.vitess.client.Context;
import io.vitess.client.RpcClient;
import io.vitess.client.VTGateBlockingConnection;
import io.vitess.client.VTGateConnection;
import io.vitess.client.VTSession;
import io.vitess.client.grpc.RetryingInterceptorConfig;
import io.vitess.client.grpc.StaticAuthCredentials;
import io.vitess.client.grpc.tls.TlsOptions;
import io.vitess.proto.Query;
import io.vitess.proto.Vtrpc;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link VitessGrpcClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultVitessGrpcClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <VitessGrpcClient, ClusterIdValue, DefaultVitessGrpcClientIntegrationServiceConfiguration>
        implements VitessGrpcClientIntegrationService
{
    private final AtomicReference<VitessGrpcClient> vitessGrpcClientRef;
    private final AtomicBoolean initializedRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultVitessGrpcClientIntegrationServiceConfiguration
            defaultVitessGrpcClientIntegrationServiceConfiguration;


    public DefaultVitessGrpcClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<VitessGrpcClient, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final VitessGrpcClient vitessGrpcClient)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, VitessGrpcClient.class);

        AppbaseUtil.notNull(vitessGrpcClient, "vitessGrpcClient");

        this.vitessGrpcClientRef = new AtomicReference<>(vitessGrpcClient);
        this.initializedRef = new AtomicBoolean(false);
        this.defaultVitessGrpcClientIntegrationServiceConfiguration = null;
    }

    public DefaultVitessGrpcClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<VitessGrpcClient, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final DefaultVitessGrpcClientIntegrationServiceConfiguration
                    defaultVitessGrpcClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, VitessGrpcClient.class);

        AppbaseUtil.notNull(
                defaultVitessGrpcClientIntegrationServiceConfiguration,
                "defaultVitessGrpcClientIntegrationServiceConfiguration");

        this.vitessGrpcClientRef = new AtomicReference<>();
        this.initializedRef = new AtomicBoolean(false);
        this.defaultVitessGrpcClientIntegrationServiceConfiguration = defaultVitessGrpcClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    private TlsOptions tlsOptions(final TlsOptionsConfiguration tlsOptionsConfiguration) {
        AppbaseUtil.notNull(tlsOptionsConfiguration, "tlsOptionsConfiguration");

        final TlsOptions tlsOptions = new TlsOptions();

        final String keyAlias = tlsOptionsConfiguration.getKeyAlias();
        if (keyAlias != null) {
            tlsOptions.keyAlias(keyAlias);
        }

        final String keyPassword = tlsOptionsConfiguration.getKeyPassword();
        if (keyPassword != null) {
            tlsOptions.keyPassword(keyPassword);
        }

        final String keyStorePath = tlsOptionsConfiguration.getKeyStorePath();
        if (keyStorePath != null) {
            tlsOptions.keyStorePath(keyStorePath);
        }

        final String trustAlias = tlsOptionsConfiguration.getTrustAlias();
        if (trustAlias != null) {
            tlsOptions.trustAlias(trustAlias);
        }

        final String trustStorePath = tlsOptionsConfiguration.getTrustStorePath();
        if (trustStorePath != null) {
            tlsOptions.trustStorePath(trustStorePath);
        }

        final String trustStorePassword = tlsOptionsConfiguration.getTrustStorePassword();
        if (trustStorePassword != null) {
            tlsOptions.trustStorePassword(trustStorePassword);
        }

        return tlsOptions;
    }

    @VisibleForTesting
    static Context context(final ContextConfiguration contextConfiguration) {
        Context context = Context.getDefault();

        if (contextConfiguration == null) {
            return context;
        }

        final Duration deadlineDuration = contextConfiguration.getDeadlineDuration();
        if (deadlineDuration != null) {
            // deadlineDuration is a RELATIVE timeout (the builder default is PT10S), not an absolute
            // instant. It must be applied with withDeadlineAfter(<relative Duration>): feeding it to
            // Instant.ofEpochMilli(...) produced an absolute epoch instant (PT10S -> 1970-01-01T00:00:10Z),
            // an already-expired deadline that made every configured Vitess RPC time out immediately once
            // the (previously discarded) withDeadline(...) return began to be captured.
            context = context.withDeadlineAfter(org.joda.time.Duration.millis(deadlineDuration.toMillis()));
        }

        final CallerIdConfiguration callerIdConfiguration = contextConfiguration.getCallerIdConfiguration();
        if (callerIdConfiguration != null) {
            final String principal = callerIdConfiguration.getPrincipal();
            final String component = callerIdConfiguration.getComponent();
            final String subComponent = callerIdConfiguration.getSubComponent();

            if (principal != null || component != null || subComponent != null) {
                final Vtrpc.CallerID.Builder builder = Vtrpc.CallerID.newBuilder();

                if (principal != null) {
                    builder.setPrincipal(principal);
                }
                if (component != null) {
                    builder.setComponent(component);
                }

                if (subComponent != null) {
                    builder.setSubcomponent(subComponent);
                }

                final Vtrpc.CallerID callerID = builder.build();
                context = context.withCallerId(callerID);
            }
        }

        return context;
    }

    private RpcClientAndContext rpcClient() {
        final CustomGrpcClientFactoryConfiguration customGrpcClientFactoryConfiguration =
                defaultVitessGrpcClientIntegrationServiceConfiguration.getCustomGrpcClientFactoryConfiguration();

        final CustomGrpcClientFactory customGrpcClientFactory;
        final RetryingInterceptorConfiguration retryingInterceptorConfiguration =
                customGrpcClientFactoryConfiguration.getRetryingInterceptorConfiguration();
        final Boolean useTracing = customGrpcClientFactoryConfiguration.getUseTracing();
        if (useTracing != null) {
            if (retryingInterceptorConfiguration != null && retryingInterceptorConfiguration.isEnabled()) {
                final TimeUnitConfiguration initialBackoffConfiguration =
                        retryingInterceptorConfiguration.getInitialBackoffConfiguration();
                final TimeUnitConfiguration maxBackoffConfiguration =
                        retryingInterceptorConfiguration.getMaxBackoffConfiguration();
                final Double backoffMultiplier =
                        retryingInterceptorConfiguration.getBackoffMultiplier();

                final RetryingInterceptorConfig retryingInterceptorConfig;
                if (backoffMultiplier != null &&
                        initialBackoffConfiguration.isEnabled() &&
                        maxBackoffConfiguration.isEnabled())
                {
                    retryingInterceptorConfig = RetryingInterceptorConfig.exponentialConfig(
                            initialBackoffConfiguration.getTimeUnit().toMillis(initialBackoffConfiguration.getTime()),
                            maxBackoffConfiguration.getTimeUnit().toMillis(maxBackoffConfiguration.getTime()),
                            backoffMultiplier);
                } else {
                    retryingInterceptorConfig = RetryingInterceptorConfig.noOpConfig();
                }

                customGrpcClientFactory = new CustomGrpcClientFactory(
                        appbaseClientCore,
                        customGrpcClientFactoryConfiguration,
                        retryingInterceptorConfig,
                        useTracing);
            } else {
                customGrpcClientFactory = new CustomGrpcClientFactory(
                        appbaseClientCore, customGrpcClientFactoryConfiguration);
            }
        } else {
            customGrpcClientFactory = new CustomGrpcClientFactory(
                    appbaseClientCore, customGrpcClientFactoryConfiguration);
        }


        final GuiceInjectorService guiceInjectorService = appbaseClientCore.getAppbaseCore().getGuiceInjectorService();


        final CallCredentialsConfiguration callCredentialsConfiguration =
                customGrpcClientFactoryConfiguration.getCallCredentialsConfiguration();
        if (callCredentialsConfiguration != null) {
            final InjectedTypeConfiguration<? extends CallCredentials> callCredentialsInjectedTypeConfiguration =
                    callCredentialsConfiguration.getCallCredentialsInjectedTypeConfiguration();
            final StaticAuthCredentialsConfiguration staticAuthCredentialsConfiguration =
                    callCredentialsConfiguration.getStaticAuthCredentialsConfiguration();

            if (callCredentialsInjectedTypeConfiguration != null) {
                final CallCredentials callCredentials =
                        AppbaseUtil.injectedInstance(guiceInjectorService, callCredentialsInjectedTypeConfiguration);
                customGrpcClientFactory.setCallCredentials(callCredentials);
            } else if (staticAuthCredentialsConfiguration != null) {
                final String username = staticAuthCredentialsConfiguration.getUsername();
                final String password = staticAuthCredentialsConfiguration.getPassword();
                final StaticAuthCredentials staticAuthCredentials = new StaticAuthCredentials(username, password);
                customGrpcClientFactory.setCallCredentials(staticAuthCredentials);
            }
        }


        final LoadBalancerFactoryConfiguration loadBalancerFactoryConfiguration =
                customGrpcClientFactoryConfiguration.getLoadBalancerFactoryConfiguration();
        if (loadBalancerFactoryConfiguration != null) {
            final InjectedTypeConfiguration<? extends LoadBalancer.Factory> loadBalancerFactoryInjectedTypeConfiguration =
                    loadBalancerFactoryConfiguration.getLoadBalancerFactoryInjectedTypeConfiguration();
            final LoadBalancer.Factory factory =
                    AppbaseUtil.injectedInstance(guiceInjectorService, loadBalancerFactoryInjectedTypeConfiguration);
            customGrpcClientFactory.setLoadBalancerFactory(factory);
        }


        final NameResolverFactoryConfiguration nameResolverFactoryConfiguration =
                customGrpcClientFactoryConfiguration.getNameResolverFactoryConfiguration();
        if (nameResolverFactoryConfiguration != null) {
            final InjectedTypeConfiguration<? extends NameResolver.Factory> nameResolverInjectedTypeConfiguration =
                    nameResolverFactoryConfiguration.getNameResolverInjectedTypeConfiguration();
            final NameResolver.Factory factory =
                    AppbaseUtil.injectedInstance(guiceInjectorService, nameResolverInjectedTypeConfiguration);
            customGrpcClientFactory.setNameResolverFactory(factory);
        }


        final Context context = context(customGrpcClientFactoryConfiguration.getContextConfiguration());
        final String target = VitessGrpcClientUtil.target(customGrpcClientFactoryConfiguration.getTargetConfiguration());
        final TlsOptionsConfiguration tlsOptionsConfiguration =
                customGrpcClientFactoryConfiguration.getTlsOptionsConfiguration();

        final RpcClient rpcClient;
        if (tlsOptionsConfiguration != null) {
            final TlsOptions tlsOptions = tlsOptions(tlsOptionsConfiguration);
            rpcClient = customGrpcClientFactory.createTls(context, target, tlsOptions);
        } else {
            rpcClient = customGrpcClientFactory.create(context, target);
        }

        return new RpcClientAndContext(rpcClient, context);
    }

    private Query.ExecuteOptions executeOptions(final ExecuteOptionsConfiguration executeOptionsConfiguration) {
        final Query.ExecuteOptions.Builder executeOptionsBuilder = Query.ExecuteOptions.newBuilder();

        if (executeOptionsConfiguration != null) {
            final Boolean clientFoundRows = executeOptionsConfiguration.getClientFoundRows();
            if (clientFoundRows != null) {
                executeOptionsBuilder.setClientFoundRows(clientFoundRows);
            }

            final Boolean skipQueryPlanCache = executeOptionsConfiguration.getSkipQueryPlanCache();
            if (skipQueryPlanCache != null) {
                executeOptionsBuilder.setSkipQueryPlanCache(skipQueryPlanCache);
            }

            final Long sqlSelectLimit = executeOptionsConfiguration.getSqlSelectLimit();
            if (sqlSelectLimit != null) {
                executeOptionsBuilder.setSqlSelectLimit(sqlSelectLimit);
            }

            final VitessTransactionIsolation vitessTransactionIsolation =
                    executeOptionsConfiguration.getVitessTransactionIsolation();
            if (vitessTransactionIsolation != null) {
                executeOptionsBuilder.setTransactionIsolation(vitessTransactionIsolation.getTransactionIsolation());
            }

            final VitessWorkload vitessWorkload = executeOptionsConfiguration.getVitessWorkload();
            if (vitessWorkload != null) {
                executeOptionsBuilder.setWorkload(vitessWorkload.getWorkload());
            }
        }

        return executeOptionsBuilder.build();
    }

    @Override
    public VTSession newVtSession() {
        final VtSessionConfiguration vtSessionConfiguration = defaultVitessGrpcClientIntegrationServiceConfiguration
                .getVtSessionConfiguration();
        final TargetConfiguration targetConfiguration = defaultVitessGrpcClientIntegrationServiceConfiguration
                .getCustomGrpcClientFactoryConfiguration()
                .getTargetConfiguration();
        final String target = VitessGrpcClientUtil.target(targetConfiguration);

        final ExecuteOptionsConfiguration executeOptionsConfiguration =
                vtSessionConfiguration.getExecuteOptionsConfiguration();
        final Query.ExecuteOptions executeOptions = executeOptions(executeOptionsConfiguration);

        final VTSession vtSession = new VTSession(target, executeOptions);

        final Boolean autoCommit = vtSessionConfiguration.getAutoCommit();
        if (autoCommit != null) {
            vtSession.setAutoCommit(autoCommit);
        }

        return vtSession;
    }

    @Override
    public Context newContext() {
        final CustomGrpcClientFactoryConfiguration factoryConfiguration =
                defaultVitessGrpcClientIntegrationServiceConfiguration.getCustomGrpcClientFactoryConfiguration();
        return context(factoryConfiguration.getContextConfiguration());
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultVitessGrpcClientIntegrationServiceConfiguration,
                "defaultVitessGrpcClientIntegrationServiceConfiguration");

        final RpcClientAndContext rpcClientAndContext = rpcClient();
        final RpcClient rpcClient = rpcClientAndContext.getRpcClient();
        final VTGateConnection vtGateConnection = new VTGateConnection(rpcClient);
        final VTGateBlockingConnection vtGateBlockingConnection = new VTGateBlockingConnection(rpcClient);
        final VTSession vtSession = newVtSession();

        final VitessGrpcClient vitessGrpcClient = DefaultVitessGrpcClient
                .builder()
                .defaultContext(rpcClientAndContext.getContext())
                .vtGateConnection(vtGateConnection)
                .vtGateBlockingConnection(vtGateBlockingConnection)
                .vtSession(vtSession)
                .build();
        vitessGrpcClientRef.set(vitessGrpcClient);

        initializedRef.set(true);
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultVitessGrpcClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        final VitessGrpcClient vitessGrpcClient = getClient();

        if (vitessGrpcClient != null) {
            final VTGateConnection vtGateConnection = vitessGrpcClient.getVtGateConnection();
            if (vtGateConnection != null) {
                try {
                    vtGateConnection.close();
                } catch (IOException ioe) {
                    logger.error("Error closing {}", VTGateConnection.class.getName());
                }
            }
        }

        vitessGrpcClientRef.set(null);
        initializedRef.set(false);
    }

    @Override
    public VitessGrpcClient getClient() {
        return vitessGrpcClientRef.get();
    }

    @Override
    public boolean isHealthy() {
        final VitessGrpcClient vitessGrpcClient = getClient();

        if (vitessGrpcClient == null) {
            return false;
        }

        return initializedRef.get();
    }

    private static final class RpcClientAndContext {
        private final RpcClient rpcClient;
        private final Context context;

        private RpcClientAndContext(final RpcClient rpcClient, final Context context) {
            AppbaseUtil.notNull(rpcClient, "rpcClient");
            AppbaseUtil.notNull(context, "context");

            this.rpcClient = rpcClient;
            this.context = context;
        }

        private RpcClient getRpcClient() {
            return rpcClient;
        }

        private Context getContext() {
            return context;
        }
    }
}
