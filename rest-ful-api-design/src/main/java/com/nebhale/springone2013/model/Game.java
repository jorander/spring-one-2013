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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public final class Game {

    private final Integer id;

    private final Map<Integer, Door> doors;

    private final Object monitor = new Object();

    private volatile GameStatus status;

    public Game(Integer id, Set<Door> doors) {
        this.id = id;

        this.doors = new HashMap<>();
        doors.stream().forEach((door) -> {
            this.doors.put(door.getId(), door);
        });

        this.status = GameStatus.AWAITING_INITIAL_SELECTION;
    }

    public void select(Integer doorId) throws IllegalTransitionException, DoorDoesNotExistException {
        synchronized (monitor) {
            if (status != GameStatus.AWAITING_INITIAL_SELECTION) {
                throw new IllegalTransitionException(id, status, GameStatus.AWAITING_FINAL_SELECTION);
            }

            getDoor(doorId).setStatus(DoorStatus.SELECTED);

            openHintDoor();

            status = GameStatus.AWAITING_FINAL_SELECTION;
        }
    }

    public void open(Integer doorId) throws IllegalTransitionException, DoorDoesNotExistException {
        synchronized (monitor) {
            if (status != GameStatus.AWAITING_FINAL_SELECTION) {
                throw new IllegalTransitionException(id, status, GameStatus.WON);
            }

            final Door door = getDoor(doorId);
            if (DoorStatus.OPEN == door.getStatus()) {
                throw new IllegalTransitionException(id, doorId, door.getStatus(), DoorStatus.OPEN);
            }

            door.setStatus(DoorStatus.OPEN);

            status = DoorContent.BICYCLE == door.getContent() ? GameStatus.WON : GameStatus.LOST;
        }
    }

    @JsonIgnore
    public Integer getId() {
        return id;
    }

    public Door getDoor(Integer doorId) throws DoorDoesNotExistException {
        if (doors.containsKey(doorId)) {
            return doors.get(doorId);
        }

        throw new DoorDoesNotExistException(id, doorId);
    }

    @JsonIgnore
    public Set<Door> getDoors() {
        return new HashSet<>(doors.values());
    }

    public GameStatus getStatus() {
        synchronized (monitor) {
            return status;
        }
    }

    private void openHintDoor() {
        getDoors().stream()
                .filter(door -> DoorStatus.CLOSED == door.getStatus() && DoorContent.SMALL_FURRY_ANIMAL == door.peekContent())
                .findFirst().get()
                .setStatus(DoorStatus.OPEN);
    }
}
