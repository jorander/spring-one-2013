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

package com.nebhale.springone2013.web;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.nebhale.springone2013.model.Door;
import com.nebhale.springone2013.model.DoorDoesNotExistException;
import com.nebhale.springone2013.model.DoorStatus;
import com.nebhale.springone2013.model.Game;
import com.nebhale.springone2013.model.GameDoesNotExistException;
import com.nebhale.springone2013.model.IllegalTransitionException;
import com.nebhale.springone2013.repository.GameRepository;

@Controller
@RequestMapping("/games")
final class GamesController {

    private static final String STATUS_KEY = "status";

    private final GameRepository gameRepository;

    private final GameResourceAssembler gameResourceAssembler;

    private final DoorsResourceAssembler doorsResourceAssembler;

    @Autowired
    public GamesController(GameRepository gameRepository, GameResourceAssembler gameResourceAssembler, DoorsResourceAssembler doorsResourceAssembler) {
        this.gameRepository = gameRepository;
        this.gameResourceAssembler = gameResourceAssembler;
        this.doorsResourceAssembler = doorsResourceAssembler;
    }

    @RequestMapping(method = RequestMethod.POST, value = "")
    public ResponseEntity<Void> createGame() {
        final Game game = gameRepository.create();

        final HttpHeaders headers = new HttpHeaders();
        headers.setLocation(linkTo(GamesController.class).slash(game.getId()).toUri());

        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{gameId}", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_XML_VALUE })
    @ResponseBody
    public Resource<Game> showGame(@PathVariable Integer gameId) throws GameDoesNotExistException {
        final Game game = gameRepository.retrieve(gameId);
        return gameResourceAssembler.toResource(game);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{gameId}")
    @ResponseBody
    public void destroyGame(@PathVariable Integer gameId) throws GameDoesNotExistException {
        gameRepository.remove(gameId);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{gameId}/doors", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_XML_VALUE})
    @ResponseBody
    public Resources<Resource<Door>> showDoors(@PathVariable Integer gameId) throws GameDoesNotExistException {
        final Game game = gameRepository.retrieve(gameId);
        return doorsResourceAssembler.toResource(game);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{gameId}/doors/{doorId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = {
        MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_XML_VALUE})
    @ResponseBody
    public void modifyDoor(@PathVariable Integer gameId, @PathVariable Integer doorId, @RequestBody Map<String, String> body)
            throws MissingKeyException, GameDoesNotExistException, IllegalTransitionException, DoorDoesNotExistException {
        final DoorStatus status = getStatus(body);
        final Game game = gameRepository.retrieve(gameId);

        if (DoorStatus.SELECTED == status) {
            game.select(doorId);
        } else if (DoorStatus.OPEN == status) {
            game.open(doorId);
        } else {
            throw new IllegalTransitionException(gameId, doorId, status);
        }
    }

    @ExceptionHandler({ GameDoesNotExistException.class, DoorDoesNotExistException.class })
    public ResponseEntity<String> handleNotFounds(Exception e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({ IllegalArgumentException.class, MissingKeyException.class })
    public ResponseEntity<String> handleBadRequests(Exception e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalTransitionException.class)
    public ResponseEntity<String> handleConflicts(Exception e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
    }

    private DoorStatus getStatus(Map<String, String> body) throws MissingKeyException {
        if (body.containsKey(STATUS_KEY)) {
            final String value = body.get(STATUS_KEY);

            try {
                return DoorStatus.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format("'%s' is an illegal value for key '%s'", value, STATUS_KEY), e);
            }
        }

        throw new MissingKeyException(STATUS_KEY);
    }
}
