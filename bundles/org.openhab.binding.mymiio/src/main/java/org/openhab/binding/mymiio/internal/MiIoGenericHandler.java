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
package org.openhab.binding.mymiio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;

import static org.openhab.binding.mymiio.internal.MyMiIOBindingConstants.CHANNEL_COMMAND;

/**
 * The {@link MiIoGenericHandler} is responsible for handling commands for devices that are not yet defined.
 * Once the device has been determined, the proper handler is loaded.
 *
 * @author zaoweiceng - Initial contribution
 */
public class MiIoGenericHandler extends MiIoHandler{
    @NonNullByDefault
    public MiIoGenericHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command == RefreshType.REFRESH){
            updateData();
            return;
        }
        if (channelUID.getId().equals(CHANNEL_COMMAND)){
            cmds.put(sendCommand(command.toString()), command.toString());
        }
    }

    @Override
    protected void updateData() {
        if (skipUpdate()){
            return;
        }
        try {
            refreshNetwork();
        }catch (Exception e){

        }
    }


}
