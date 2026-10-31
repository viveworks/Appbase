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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.task;

import com.viveworks.appbase.core.util.AppbaseUtil;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CancelTaskRequest {
    private final String taskId;

    public CancelTaskRequest(final String taskId) {
        AppbaseUtil.notBlank(taskId, "taskId");

        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CancelTaskRequest that = (CancelTaskRequest) o;
        return Objects.equals(taskId, that.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    @Override
    public String toString() {
        return "CancelTaskRequest{" +
                "taskId='" + taskId + '\'' +
                '}';
    }
}
