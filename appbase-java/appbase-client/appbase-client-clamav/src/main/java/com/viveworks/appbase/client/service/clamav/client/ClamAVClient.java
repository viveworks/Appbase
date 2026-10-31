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

package com.viveworks.appbase.client.service.clamav.client;

import com.viveworks.appbase.client.service.clamav.client.handler.ClamAVPingResultHandler;
import com.viveworks.appbase.client.service.clamav.client.handler.ClamAVScanResultHandler;
import com.viveworks.appbase.core.service.Reloadable;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Represents a {@code ClamAV} client.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface ClamAVClient extends Reloadable {
    /**
     * Ping {@code ClamAV}.
     *
     * @param clamAVPingResultHandler {@link ClamAVPingResultHandler}
     * @throws ClamAVClientException if there were errors before executing {@code ping} command
     */
    void ping(ClamAVPingResultHandler clamAVPingResultHandler) throws ClamAVClientException;


    /**
     * Scan a {@link Path} for viruses using {@code ClamAV}.
     *
     * @param path {@link Path}
     * @param clamAVScanResultHandler {@link ClamAVScanResultHandler}
     * @throws ClamAVClientException if there were errors before executing {@code scan} command
     */
    void scan(Path path, ClamAVScanResultHandler clamAVScanResultHandler) throws ClamAVClientException;


    /**
     * Scan {@link InputStream} for viruses using {@code ClamAV}.
     *
     * @param inputStream {@link InputStream}
     * @param closeInputStream Close {@code inputStream} after reading
     * @param clamAVScanResultHandler {@link ClamAVScanResultHandler}
     * @throws ClamAVClientException if there were errors before executing {@code scan} command
     */
    void scan(
            InputStream inputStream,
            boolean closeInputStream,
            ClamAVScanResultHandler clamAVScanResultHandler) throws ClamAVClientException;
}
