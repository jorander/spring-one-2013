/*
 * Copyright 2013 the original author or authors.
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
package com.nebhale.springone2013.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public final class Door {

    private final Integer id;

    private final Object monitor = new Object();

    private final DoorContent content;

    private DoorStatus status;

    public Door(Integer id, DoorContent content) {
        this.id = id;
        this.content = content;
        this.status = DoorStatus.CLOSED;
    }

    @JsonIgnore
    public Integer getId() {
        return id;
    }

    public DoorContent getContent() {
        synchronized (monitor) {
            return status == DoorStatus.OPEN ? content : DoorContent.UNKNOWN;
        }
    }

    public DoorStatus getStatus() {
        synchronized (monitor) {
            return status;
        }
    }

    DoorContent peekContent() {
        return content;
    }

    void setStatus(DoorStatus status) {
        synchronized (monitor) {
            this.status = status;
        }
    }
}
