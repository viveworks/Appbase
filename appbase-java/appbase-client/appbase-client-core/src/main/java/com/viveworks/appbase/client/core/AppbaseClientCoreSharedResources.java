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

package com.viveworks.appbase.client.core;

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.MutableAppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.clientkey.clusterid.MutableClusterIdValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.HostPortValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.core.util.AppbaseSharedResources;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPool;

/**
 * {@link AppbaseSharedResources} for this client core module.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface AppbaseClientCoreSharedResources extends AppbaseClientSharedResources {
    /**
     * Get shared {@link HostPortValue} object pool.
     *
     * @return {@link ManyToManyQueuedFixedObjectPool}
     */
    ManyToManyQueuedFixedObjectPool<HostPortValue> getSharedHostPortValueObjectPool();


    /**
     * Get shared {@link TextValue} object pool.
     *
     * @return {@link ManyToManyQueuedFixedObjectPool}
     */
    ManyToManyQueuedFixedObjectPool<TextValue> getSharedTextValueObjectPool();


    /**
     * Get shared {@link ClusterIdValue} object pool.
     *
     * @return {@link ManyToManyQueuedFixedObjectPool}
     */
    ManyToManyQueuedFixedObjectPool<MutableClusterIdValue> getSharedClusterIdValueObjectPool();


    /**
     * Get shared {@link MutableAppbaseClientKey} object pool.
     *
     * @return {@link ManyToManyQueuedFixedObjectPool}
     */
    ManyToManyQueuedFixedObjectPool<MutableAppbaseClientKey<? extends ComparableValue>>
    getSharedMutableAppbaseClientKeyObjectPool();


    /**
     * Get <b>uninitialized</b> {@link HostPortValue}d {@link AppbaseClientKey}. The returned {@link HostPortValue} can be
     * used as follows (it must be returned using {@link #givePooledHostPortClientKey(AppbaseClientKey)}):
     *
     * <pre>
     *     final AppbaseClientKey&lt;HostPortValue&gt; hostPortClientKey =
     *                      appbaseClientCoreSharedResources.takeUninitializedPooledHostPortClientKey();
     *     try {
     *          final HostPortValue hostPortValue = hostPortClientKey.getValue();
     *          hostPortValue.init("localhost", 1234);
     *
     *          final AppbaseClientIntegrationService&lt;C&gt; appbaseClientIntegrationService =
     *                      appbaseKeyedClientService.pooledClientIntegrationService(hostPortClientKey);
     *
     *          // Use appbaseClientIntegrationService ...
     *      } finally {
     *          appbaseClientShares.UninitializedPooledHostPortClientKey(hostPortClientKey);
     *      }
     * </pre>
     *
     * @return {@link HostPortValue}d {@link AppbaseClientKey}
     */
    AppbaseClientKey<HostPortValue> takeUninitializedPooledHostPortClientKey();


    /**
     * Return {@link HostPortValue} after use, see {@link #takeUninitializedPooledHostPortClientKey()} for more details.
     *
     * @param hostPortClientKey {@link HostPortValue}
     */
    void givePooledHostPortClientKey(AppbaseClientKey<HostPortValue> hostPortClientKey);


    /**
     * Get <b>uninitialized</b> {@link TextValue}d {@link AppbaseClientKey}. The returned {@link TextValue} can be
     * used as follows:
     *
     * <pre>
     *     final AppbaseClientKey&lt;TextValue&gt; textValueClientKey =
     *              appbaseClientCoreSharedResources.takeUninitializedPooledTextClientKey();
     *
     *     try {
     *         final TextValue textValue = textValueClientKey.getValue();
     *         textValue.init("primaryStore");
     *
     *         final AppbaseClientIntegrationService&lt;C&gt; appbaseClientIntegrationService =
     *              appbaseKeyedClientService.pooledClientIntegrationService(textValueClientKey);
     *         // Use appbaseClientIntegrationService ...
     *     } finally {
     *         appbaseClientCoreSharedResources.givePooledTextClientKey(textValueClientKey);
     *     }
     * </pre>
     *
     * @return {@link TextValue}d {@link AppbaseClientKey}
     */
    AppbaseClientKey<TextValue> takeUninitializedPooledTextClientKey();


    /**
     * Return {@link TextValue} after use, see {@link #takeUninitializedPooledTextClientKey} for more details.
     *
     * @param textClientKey {@link TextValue}
     */
    void givePooledTextClientKey(AppbaseClientKey<TextValue> textClientKey);


    /**
     * Get <b>uninitialized</b> {@link MutableClusterIdValue}d {@link AppbaseClientKey}. The returned
     * {@link MutableClusterIdValue} can be used as follows:
     *
     * <pre>
     *     final AppbaseClientKey&lt;MutableClusterIdValue&gt; mutableClusterIdValueClientKey =
     *              appbaseClientCoreSharedResources.takeUninitializedPooledClusterIdClientKey();
     *
     *     try {
     *         final MutableClusterIdValue clusterIdValue = mutableClusterIdValueClientKey.getValue();
     *         mutableClusterIdValue.init("primaryStore");
     *
     *         final AppbaseClientIntegrationService&lt;C&gt; appbaseClientIntegrationService =
     *              appbaseKeyedClientService.pooledClientIntegrationService(mutableClusterIdValueClientKey);
     *         // Use appbaseClientIntegrationService ...
     *     } finally {
     *         appbaseClientCoreSharedResources.givePooledClusterIdClientKey(mutableClusterIdValueClientKey);
     *     }
     * </pre>
     *
     * @return {@link MutableClusterIdValue}d {@link AppbaseClientKey}
     */
    AppbaseClientKey<MutableClusterIdValue> takeUninitializedPooledClusterIdClientKey();


    /**
     * Return {@link MutableClusterIdValue} after use, see {@link #takeUninitializedPooledClusterIdClientKey} for more
     * details.
     *
     * @param mutableClusterIdClientKey {@link MutableClusterIdValue}
     */
    void givePooledClusterIdClientKey(AppbaseClientKey<MutableClusterIdValue> mutableClusterIdClientKey);
}
