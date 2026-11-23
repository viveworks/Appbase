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
package com.viveworks.appbase.service.supportmessage;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.id.DefaultIdService;
import com.viveworks.appbase.service.id.DefaultIdServiceConfiguration;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.supportmessage.data.ReadSupportMessagesRequest;
import com.viveworks.appbase.service.supportmessage.data.ReadSupportMessagesResponse;
import com.viveworks.appbase.service.supportmessage.store.SupportMessageStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * L4-03 delta (CWE-400): the owner has set the support-message read maximum to 500, and that
 * maximum must hold at the <em>service</em> boundary -- not only on the JAX-RS resource's
 * {@code @Max} annotation, which the servlet face ({@code ReadSupportMessagesServlet}, which
 * parses {@code limit} with no bound at all) and any direct service caller bypass entirely.
 *
 * <p>Unlike {@link DefaultSupportMessageServiceIT}, this class is fully offline: the store is
 * a recording stub, so no JDBI handle, vtgate, or live server is involved. It also pins that
 * the bounded {@code limit} is what actually reaches {@link SupportMessageStore#readPage} --
 * the store boundary the maximum exists to protect.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class SupportMessageReadLimitCapTest {
    private static AppbaseCore appbaseCore;
    private static DefaultSupportMessageService supportMessageService;
    private static RecordingSupportMessageStore recordingStore;

    @BeforeAll
    static void setUp() {
        System.setProperty(
                ClientCoreTestHelper.CORE_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-core.yaml");
        System.setProperty(
                ClientCoreTestHelper.CLIENT_CORE_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-client-core.yaml");

        appbaseCore = ClientCoreTestHelper.appbaseCore();
        recordingStore = new RecordingSupportMessageStore();
        final IdService idService = new DefaultIdService(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                DefaultIdServiceConfiguration.builder().build());
        supportMessageService = new DefaultSupportMessageService(
                recordingStore,
                idService,
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                new DefaultSupportMessageServiceConfiguration());
    }

    @AfterAll
    static void tearDown() {
        if (supportMessageService != null) {
            supportMessageService.shutdown();
        }

        if (appbaseCore != null) {
            appbaseCore.shutdown();
        }
    }

    /**
     * The owner-set maximum is 500. Pinned as an explicit value (not a self-referencing
     * constant read-back) so a drift back to the old 1000 -- or any other value -- fails here.
     */
    @Test
    void maximumReadLimitIsTheOwnerSetMaximum() {
        Assertions.assertEquals(500, AppbaseServiceConstants.MAX_SUPPORT_MESSAGE_READ_LIMIT,
                "L4-03 delta: the owner has set the support-message read maximum to 500");
    }

    /**
     * One above the maximum must be refused by the service itself: the servlet face and direct
     * service callers never pass through the JAX-RS bean validation that carries {@code @Max}.
     */
    @Test
    void readSupportMessagesRejectsLimitAboveMaximum() {
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> supportMessageService.readSupportMessages(
                        new ReadSupportMessagesRequest(0, 501, null)),
                "the service boundary must refuse limit=501 itself, not rely on the JAX-RS @Max");
    }

    /**
     * At the maximum the request must succeed and the <em>bounded limit</em> must be what the
     * store's {@code readPage} receives -- the page size the maximum exists to bound. (This
     * also guards against the limit being dropped or substituted on the way to the store.)
     */
    @Test
    void readSupportMessagesAtMaximumForwardsTheBoundToTheStore() throws Exception {
        final ReadSupportMessagesResponse response = supportMessageService.readSupportMessages(
                new ReadSupportMessagesRequest(0, 500, null));

        Assertions.assertNotNull(response);
        Assertions.assertEquals(0, recordingStore.lastOffset);
        Assertions.assertEquals(500, recordingStore.lastLimit,
                "readPage must receive the bounded limit, not a second copy of the offset");
    }

    /**
     * Offline recording stub: only {@code readPage} is exercised, and only to observe the
     * (offset, limit) pair the service actually forwards.
     */
    private static final class RecordingSupportMessageStore implements SupportMessageStore {
        private int lastOffset = -1;
        private int lastLimit = -1;

        @Override
        public Collection<SupportMessage> readPage(
                final int offset, final int limit) {
            this.lastOffset = offset;
            this.lastLimit = limit;
            return Collections.emptyList();
        }

        @Override
        public int createSupportMessage(
                final SupportMessage supportMessage) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<SupportMessage> readByMessageId(
                final String messageId) {
            return Optional.empty();
        }

        @Override
        public boolean existsByMessageId(final String messageId) {
            return false;
        }

        @Override
        public int updateMessage(final String messageId, final String message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateStatus(final String messageId, final String status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean deleteByMessageId(final String messageId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void shutdown() {
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public <C extends AppbaseConfiguration> void reload(final C configuration)
                throws ServiceReloadException {
        }

        @Override
        public ServiceStatus getStatus() {
            return ServiceStatus.STARTED;
        }

        @Override
        public <C extends AppbaseConfiguration> C getConfiguration() {
            return null;
        }
    }
}
