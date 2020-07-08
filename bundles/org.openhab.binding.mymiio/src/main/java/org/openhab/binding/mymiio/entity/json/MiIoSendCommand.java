/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mymiio.entity.json;

import com.google.gson.JsonObject;
import com.sun.istack.Nullable;
import org.openhab.binding.mymiio.entity.MiIoCommand;

/**
 * The {@link MiIoSendCommand} is responsible for creating Xiaomi messages.
 *
 * @author zaoweiceng - Initial contribution
 */
public class MiIoSendCommand {
    private final int id;
    private final MiIoCommand command;
    private final String commandString;
    private  JsonObject response;

    public void setResponse(JsonObject response){
        this.response = response;
    }

    public MiIoSendCommand(int id, MiIoCommand command, String commandString) {
        this.id = id;
        this.command = command;
        this.commandString = commandString;
    }

    public int getId() {
        return id;
    }

    public MiIoCommand getCommand() {
        return command;
    }

    public String getCommandString() {
        return commandString;
    }

    public JsonObject getResponse() {
        return response;
    }

    public boolean isError(){
        if (response != null){
            return response.has("error");
        }
        return true;
    }

    public JsonObject getResult(){
        if (response != null && response.has("result")){
            return (JsonObject)response.get("result");
        }
        return new JsonObject();
    }

}