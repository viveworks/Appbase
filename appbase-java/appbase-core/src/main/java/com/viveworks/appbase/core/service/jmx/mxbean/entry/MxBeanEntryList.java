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

package com.viveworks.appbase.core.service.jmx.mxbean.entry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;
import java.util.Objects;

/**
 * List of {@link com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MxBeanEntryList {
    @JsonSerialize(contentAs = MXBeanEntry.class)
    @JsonDeserialize(contentAs = MXBeanEntry.class)
    private List<MXBeanEntry> mxBeanEntries;

    public MxBeanEntryList() {
    }

    @JsonCreator
    public MxBeanEntryList(
            @JsonProperty(value = "mxBeanEntries")
            final List<MXBeanEntry> mxBeanEntries)
    {
        this.mxBeanEntries = mxBeanEntries;
    }

    public List<MXBeanEntry> getMxBeanEntries() {
        return mxBeanEntries;
    }

    public void setMxBeanEntries(final List<MXBeanEntry> mxBeanEntries) {
        this.mxBeanEntries = mxBeanEntries;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MxBeanEntryList that = (MxBeanEntryList) o;
        return Objects.equals(mxBeanEntries, that.mxBeanEntries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mxBeanEntries);
    }

    @Override
    public String toString() {
        return "MxBeanEntryList{" +
                "mxBeanEntries=" + mxBeanEntries +
                '}';
    }
}
