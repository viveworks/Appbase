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

package com.viveworks.appbase.client.service.vitess.configuration;

import io.vitess.proto.Topodata;

/**
 * Represents {@code vttablet} type and wraps {@link Topodata.TabletType}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum VitessTabletType {
    PRIMARY(Topodata.TabletType.MASTER, "primary"),

    MASTER(Topodata.TabletType.MASTER, "primary"),

    REPLICA(Topodata.TabletType.REPLICA, "replica"),

    READ_ONLY(Topodata.TabletType.RDONLY, "rdonly"),

    BACKUP(Topodata.TabletType.BACKUP, "backup"),

    RESTORE(Topodata.TabletType.RESTORE, "restore"),

    DRAINED(Topodata.TabletType.DRAINED, "drained"),

    SPARE(Topodata.TabletType.SPARE, "spare"),

    EXPERIMENTAL(Topodata.TabletType.EXPERIMENTAL, "experimental");

    private final Topodata.TabletType tabletType;

    private final String text;

    VitessTabletType(final Topodata.TabletType tabletType, final String text) {
        this.tabletType = tabletType;
        this.text = text;
    }

    public Topodata.TabletType getTabletType() {
        return tabletType;
    }

    public String getText() {
        return text;
    }
}
