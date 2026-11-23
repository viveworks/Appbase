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

package com.viveworks.appbase.client.service.vitess.accessor;

import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.vitess.VitessGrpcClient;
import com.viveworks.appbase.client.service.vitess.VitessGrpcClientIntegrationService;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.vitess.client.Context;
import io.vitess.client.VTGateBlockingConnection;
import io.vitess.client.VTSession;
import io.vitess.client.cursor.Cursor;
import io.vitess.client.cursor.CursorWithError;
import io.vitess.proto.Query;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.joda.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Tests for {@link VitessGrpcAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class VitessGrpcAccessorTest {

    /**
     * Bundles a mocked {@link AppbaseCore} with the metric fixtures it was built from, so tests
     * can both drive the accessor and verify the shared spine's interactions (e.g. timer
     * recording). Mirrors {@code ReactiveVitessGrpcAccessorTest.CoreFixtures}.
     */
    private static final class CoreFixtures {
        final AppbaseCore appbaseCore;
        final Timer timer;
        final Timer.Context timerContext;
        final MetricRegistry metricRegistry;
        final Tracer tracer;
        final Span span;
        final Mediator mediator;

        private CoreFixtures(final AppbaseCore appbaseCore, final Timer timer, final Timer.Context timerContext,
                              final MetricRegistry metricRegistry, final Tracer tracer, final Span span,
                              final Mediator mediator) {
            this.appbaseCore = appbaseCore;
            this.timer = timer;
            this.timerContext = timerContext;
            this.metricRegistry = metricRegistry;
            this.tracer = tracer;
            this.span = span;
            this.mediator = mediator;
        }

        static CoreFixtures create() {
            final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

            final LogService logService = Mockito.mock(LogService.class);
            Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
            Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                    .thenReturn(LoggerFactory.getLogger(VitessGrpcAccessorTest.class));

            final MetricService metricService = Mockito.mock(MetricService.class, Mockito.CALLS_REAL_METHODS);
            final MetricRegistry metricRegistry = Mockito.mock(MetricRegistry.class);
            final Timer timer = Mockito.mock(Timer.class);
            final Timer.Context timerContext = Mockito.mock(Timer.Context.class);
            final Counter errorCounter = Mockito.mock(Counter.class);
            Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);
            Mockito.doReturn(metricRegistry).when(metricService).getMetricRegistry();
            Mockito.when(metricRegistry.timer(Mockito.anyString())).thenReturn(timer);
            Mockito.when(metricRegistry.counter(Mockito.anyString())).thenReturn(errorCounter);
            Mockito.when(timer.time()).thenReturn(timerContext);

            final TracerService tracerService = Mockito.mock(TracerService.class);
            final Tracer tracer = Mockito.mock(Tracer.class);
            final Tracer.SpanBuilder spanBuilder = Mockito.mock(Tracer.SpanBuilder.class);
            final Span span = Mockito.mock(Span.class);
            Mockito.when(appbaseCore.getTracerService()).thenReturn(tracerService);
            Mockito.when(tracerService.getTracer()).thenReturn(tracer);
            Mockito.when(tracer.buildSpan(Mockito.anyString())).thenReturn(spanBuilder);
            Mockito.when(spanBuilder.start()).thenReturn(span);

            final MediatorService mediatorService = Mockito.mock(MediatorService.class);
            final Mediator mediator = Mockito.mock(Mediator.class);
            Mockito.when(appbaseCore.getMediatorService()).thenReturn(mediatorService);
            Mockito.when(mediatorService.getMediator()).thenReturn(mediator);
            Mockito.when(mediator.getMessageFromPool()).thenReturn(new Message());

            return new CoreFixtures(appbaseCore, timer, timerContext, metricRegistry, tracer, span, mediator);
        }
    }

    private static VitessGrpcClientIntegrationService mockIntegrationService(
            final VitessGrpcClient vitessGrpcClient, final VTSession vtSession) {
        final Context context = vitessGrpcClient.getDefaultContext();
        final VitessGrpcClientIntegrationService integrationService =
                Mockito.mock(VitessGrpcClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(vitessGrpcClient);
        Mockito.when(integrationService.newVtSession()).thenReturn(vtSession);
        Mockito.when(integrationService.newContext()).thenReturn(context);
        return integrationService;
    }

    private static VitessGrpcClient mockVitessGrpcClient(
            final VTGateBlockingConnection vtGateBlockingConnection, final Context context) {
        final VitessGrpcClient vitessGrpcClient = Mockito.mock(VitessGrpcClient.class);
        Mockito.when(vitessGrpcClient.getVtGateBlockingConnection()).thenReturn(vtGateBlockingConnection);
        Mockito.when(vitessGrpcClient.getDefaultContext()).thenReturn(context);
        return vitessGrpcClient;
    }

    private static VitessGrpcAccessor accessor(
            final CoreFixtures core,
            final DefaultReactiveOperationConfiguration configuration,
            final Supplier<VitessGrpcClientIntegrationService> borrow) {
        return new VitessGrpcAccessor(core.appbaseCore, configuration, borrow);
    }

    private static DefaultReactiveOperationConfiguration defaultConfig() {
        return DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build();
    }

    private static VTSession vtSession() {
        return new VTSession("target", Query.ExecuteOptions.getDefaultInstance());
    }

    @Test
    void executeReturnsCursor() throws Exception {
        final Cursor cursor = Mockito.mock(Cursor.class);
        final VTGateBlockingConnection conn = Mockito.mock(VTGateBlockingConnection.class);
        Mockito.when(conn.execute(
                        Mockito.any(Context.class), Mockito.eq("select 1"), Mockito.anyMap(), Mockito.any(VTSession.class)))
                .thenReturn(cursor);
        final VitessGrpcClientIntegrationService integrationService =
                mockIntegrationService(mockVitessGrpcClient(conn, Context.getDefault()), vtSession());
        final VitessGrpcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        final Cursor result = accessor.execute("exec", "select 1", Collections.emptyMap());

        Assertions.assertSame(cursor, result);
    }

    @Test
    void executeThrowSurfacesVitessAccessorException() throws SQLException {
        final VTGateBlockingConnection conn = Mockito.mock(VTGateBlockingConnection.class);
        Mockito.when(conn.execute(
                        Mockito.any(Context.class), Mockito.anyString(), Mockito.anyMap(), Mockito.any(VTSession.class)))
                .thenThrow(new SQLException("y"));
        final VitessGrpcClientIntegrationService integrationService =
                mockIntegrationService(mockVitessGrpcClient(conn, Context.getDefault()), vtSession());
        final VitessGrpcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        Assertions.assertThrows(VitessAccessorException.class,
                () -> accessor.execute("exec", "select 1", Collections.emptyMap()));
    }

    @Test
    void integrationServiceReturnedOnSuccessAndError() throws Exception {
        final Cursor cursor = Mockito.mock(Cursor.class);
        final VTGateBlockingConnection conn = Mockito.mock(VTGateBlockingConnection.class);
        Mockito.when(conn.execute(
                        Mockito.any(Context.class), Mockito.eq("ok"), Mockito.anyMap(), Mockito.any(VTSession.class)))
                .thenReturn(cursor);
        Mockito.when(conn.execute(
                        Mockito.any(Context.class), Mockito.eq("err"), Mockito.anyMap(), Mockito.any(VTSession.class)))
                .thenThrow(new SQLException("x"));
        final VitessGrpcClientIntegrationService integrationService =
                mockIntegrationService(mockVitessGrpcClient(conn, Context.getDefault()), vtSession());
        final VitessGrpcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        accessor.execute("exec", "ok", Collections.emptyMap());
        Assertions.assertThrows(VitessAccessorException.class,
                () -> accessor.execute("exec", "err", Collections.emptyMap()));

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }

    @Test
    void streamExecuteReturnsCursor() throws Exception {
        // The returned Cursor is now a BorrowReleasingCursor wrapping the driver's cursor (see
        // syncStreamExecuteHoldsBorrowUntilCursorClosed below for the borrow-lifetime contract),
        // so identity no longer holds -- assert delegation instead.
        final Cursor cursor = Mockito.mock(Cursor.class);
        Mockito.when(cursor.getRowsAffected()).thenReturn(7L);
        final VTGateBlockingConnection conn = Mockito.mock(VTGateBlockingConnection.class);
        Mockito.when(conn.streamExecute(
                        Mockito.any(Context.class), Mockito.eq("select stream"), Mockito.anyMap(), Mockito.any(VTSession.class)))
                .thenReturn(cursor);
        final VitessGrpcClientIntegrationService integrationService =
                mockIntegrationService(mockVitessGrpcClient(conn, Context.getDefault()), vtSession());
        final VitessGrpcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        final Cursor result = accessor.streamExecute("stream", "select stream", Collections.emptyMap());

        Assertions.assertNotSame(cursor, result);
        Assertions.assertEquals(7L, result.getRowsAffected());
    }

    @Test
    void syncStreamExecuteHoldsBorrowUntilCursorClosed() throws Exception {
        final Cursor mockCursor = Mockito.mock(Cursor.class);
        final VTGateBlockingConnection conn = Mockito.mock(VTGateBlockingConnection.class);
        Mockito.when(conn.streamExecute(
                        Mockito.any(Context.class), Mockito.eq("SELECT 1"), Mockito.anyMap(), Mockito.any(VTSession.class)))
                .thenReturn(mockCursor);
        final VitessGrpcClientIntegrationService integrationService =
                mockIntegrationService(mockVitessGrpcClient(conn, Context.getDefault()), vtSession());
        final VitessGrpcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        final Cursor returned = accessor.streamExecute("stream", "SELECT 1", Collections.emptyMap());

        Mockito.verify(integrationService, Mockito.never()).finalizeOperation();

        returned.close();

        Mockito.verify(integrationService).finalizeOperation();
        Mockito.verify(mockCursor).close();
    }

    @Test
    void syncStreamExecuteFinalizesImmediatelyOnObtainError() throws Exception {
        final VTGateBlockingConnection conn = Mockito.mock(VTGateBlockingConnection.class);
        Mockito.when(conn.streamExecute(
                        Mockito.any(Context.class), Mockito.anyString(), Mockito.anyMap(), Mockito.any(VTSession.class)))
                .thenThrow(new SQLException("boom"));
        final VitessGrpcClientIntegrationService integrationService =
                mockIntegrationService(mockVitessGrpcClient(conn, Context.getDefault()), vtSession());
        final VitessGrpcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        Assertions.assertThrows(VitessAccessorException.class,
                () -> accessor.streamExecute("stream", "SELECT 1", Collections.emptyMap()));

        Mockito.verify(integrationService).finalizeOperation();
    }

    @Test
    void executeBatchReturnsCursorsWithErrors() throws Exception {
        @SuppressWarnings("unchecked")
        final List<CursorWithError> batchResult = Mockito.mock(List.class);
        final VTGateBlockingConnection conn = Mockito.mock(VTGateBlockingConnection.class);
        Mockito.when(conn.executeBatch(
                        Mockito.any(Context.class), Mockito.anyList(), Mockito.anyList(), Mockito.any(VTSession.class)))
                .thenReturn(batchResult);
        final VitessGrpcClientIntegrationService integrationService =
                mockIntegrationService(mockVitessGrpcClient(conn, Context.getDefault()), vtSession());
        final VitessGrpcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);
        final Map<String, Object> bindVars = Collections.emptyMap();
        final List<Map<String, ?>> bindVarsList = new ArrayList<>();
        bindVarsList.add(bindVars);

        final List<CursorWithError> result =
                accessor.executeBatch("batch", Collections.singletonList("select 1"), bindVarsList);

        Assertions.assertSame(batchResult, result);
    }

    @Test
    void explicitExecuteUsesCallerSuppliedContextAndSession() throws Exception {
        final Cursor cursor = Mockito.mock(Cursor.class);
        final Context callerContext = Context.getDefault().withDeadlineAfter(Duration.standardSeconds(30));
        final VTSession callerSession = new VTSession("explicit", Query.ExecuteOptions.getDefaultInstance());
        final VTGateBlockingConnection conn = Mockito.mock(VTGateBlockingConnection.class);
        Mockito.when(conn.execute(
                        Mockito.same(callerContext), Mockito.eq("select 2"), Mockito.anyMap(), Mockito.same(callerSession)))
                .thenReturn(cursor);
        // A bridge-provided session that must NOT be the one passed to the driver call.
        final VTSession bridgeSession = new VTSession("bridge", Query.ExecuteOptions.getDefaultInstance());
        final VitessGrpcClientIntegrationService integrationService =
                mockIntegrationService(mockVitessGrpcClient(conn, Context.getDefault()), bridgeSession);
        final VitessGrpcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        final Cursor result =
                accessor.execute("exec", callerContext, callerSession, "select 2", Collections.emptyMap());

        Assertions.assertSame(cursor, result);
    }

    @Test
    void commandDelegatesToSuppliedCommand() throws Exception {
        final Cursor cursor = Mockito.mock(Cursor.class);
        final VTGateBlockingConnection conn = Mockito.mock(VTGateBlockingConnection.class);
        Mockito.when(conn.execute(
                        Mockito.any(Context.class), Mockito.eq("select command"), Mockito.anyMap(), Mockito.any(VTSession.class)))
                .thenReturn(cursor);
        final VitessGrpcClientIntegrationService integrationService =
                mockIntegrationService(mockVitessGrpcClient(conn, Context.getDefault()), vtSession());
        final VitessGrpcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        final VitessCommand<Cursor> command =
                (c, ctx, session) -> c.execute(ctx, "select command", Collections.emptyMap(), session);

        final Cursor result = accessor.command("command", command);

        Assertions.assertSame(cursor, result);
    }

    @Test
    void timerRecordedForSuccessfulOperation() throws Exception {
        final Cursor cursor = Mockito.mock(Cursor.class);
        final VTGateBlockingConnection conn = Mockito.mock(VTGateBlockingConnection.class);
        Mockito.when(conn.execute(
                        Mockito.any(Context.class), Mockito.anyString(), Mockito.anyMap(), Mockito.any(VTSession.class)))
                .thenReturn(cursor);
        final VitessGrpcClientIntegrationService integrationService =
                mockIntegrationService(mockVitessGrpcClient(conn, Context.getDefault()), vtSession());
        final CoreFixtures core = CoreFixtures.create();
        final VitessGrpcAccessor accessor = accessor(core, defaultConfig(), () -> integrationService);

        accessor.execute("exec", "select 1", Collections.emptyMap());

        Mockito.verify(core.metricRegistry).timer(Mockito.anyString());
        Mockito.verify(core.timer).time();
        Mockito.verify(core.timerContext).stop();
    }
}
