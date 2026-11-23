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

package com.viveworks.appbase.client.service.smtp.configuration;

import com.hubspot.smtp.client.Extension;

/**
 * Wrapper for {@link Extension}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum SmtpExtension {
    AUTH(Extension.AUTH),
    CHUNKING(Extension.CHUNKING),
    DSN(Extension.DSN),
    EIGHT_BIT_MIME(Extension.EIGHT_BIT_MIME),
    ENHANCED_STATUS_CODES(Extension.ENHANCEDSTATUSCODES),
    PIPELINING(Extension.PIPELINING),
    SIZE(Extension.SIZE),
    STARTTLS(Extension.STARTTLS),
    X_CLIENT(Extension.XCLIENT),
    X_FORWARD(Extension.XFORWARD),
    SMTP_UTF8(Extension.SMTPUTF8);

    private final Extension extension;

    SmtpExtension(final Extension extension) {
        this.extension = extension;
    }

    public Extension getExtension() {
        return extension;
    }
}
