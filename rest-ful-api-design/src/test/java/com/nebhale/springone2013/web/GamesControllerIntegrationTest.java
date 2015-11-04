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

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.io.UnsupportedEncodingException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.nebhale.springone2013.Application;
import org.springframework.test.web.servlet.ResultActions;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {Application.class})
public final class GamesControllerIntegrationTest {

    @Autowired
    private volatile WebApplicationContext webApplicationContext;

    private volatile MockMvc mockMvc;

    @Before
    public void before() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();
    }

    @Test
    public void playAGame() throws Exception {
        String gameLocation = createNewGame();

        String doorsLocation = getLinkedLocation(gameLocation, "doors");
        selectDoor(doorsLocation, 1).andExpect(status().isOk());

        if ("CLOSED".equals(getDoorStatus(doorsLocation, 0))) {
            openDoor(doorsLocation, 0)
                    .andExpect(status().isOk());
        } else {
            openDoor(doorsLocation, 2)
                    .andExpect(status().isOk());
        }

        String gameStatus = getGameStatus(gameLocation);
        assertTrue("WON".equals(gameStatus) || "LOST".equals(gameStatus));
    }

    private String createNewGame() throws Exception {
        String gameLocation = this.mockMvc.perform(post("/games"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return gameLocation;
    }

    private ResultActions selectDoor(String doorsLocation, final int doorId) throws Exception {
        return this.mockMvc.perform(put(getDoorLocation(doorsLocation, doorId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getBytes("{ \"status\": \"SELECTED\"}")));
    }

    private ResultActions openDoor(String doorsLocation, final int doorId) throws Exception {
        return this.mockMvc.perform(put(getDoorLocation(doorsLocation, doorId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getBytes("{ \"status\": \"OPEN\"}")));
    }

    private String getLinkedLocation(String location, String rel) throws Exception {
        String json = this.mockMvc.perform(get(location)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, String.format("$.links[?(@.rel==%s)].href[0]", rel));
    }

    private String getDoorLocation(String location, int index) throws Exception {
        String json = this.mockMvc.perform(get(location)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, String.format("$.content[%d].links[?(@.rel==self)].href[0]", index));
    }

    private String getDoorStatus(String location, int index) throws Exception {
        String json = this.mockMvc.perform(get(location)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, String.format("$.content[%d].status", index));
    }

    private String getGameStatus(String location) throws Exception {
        String json = this.mockMvc.perform(get(location)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.status");
    }

    private byte[] getBytes(String s) throws UnsupportedEncodingException {
        return s.getBytes("UTF8");
    }

    @Test
    public void cannotAccessNonExistingGame() throws Exception {
        mockMvc.perform(get("/games/-1")).andExpect(status().isNotFound());
    }

    @Test
    public void cannotSelectTwoDoors() throws Exception {
        final String gameLocation = createNewGame();
        final String doorsLocation = getLinkedLocation(gameLocation, "doors");
        selectDoor(doorsLocation, 1);
        selectDoor(doorsLocation, 2).andExpect(status().isConflict());
    }

    @Test
    public void cannotOpenADoorAtBeginningOfGame() throws Exception {
        final String gameLocation = createNewGame();
        final String doorsLocation = getLinkedLocation(gameLocation, "doors");
        openDoor(doorsLocation, 0).andExpect(status().isConflict());
    }
}
