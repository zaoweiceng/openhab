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

import static org.openhab.binding.mymiio.internal.MyMiIOBindingConstants.*;

import com.google.gson.*;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.cache.ExpiringCache;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.mymiio.entity.CommandParameterType;
import org.openhab.binding.mymiio.entity.MiIoCommand;
import org.openhab.binding.mymiio.entity.json.MiIoBasicChannel;
import org.openhab.binding.mymiio.entity.json.MiIoBasicDevice;
import org.openhab.binding.mymiio.entity.json.MiIoDeviceAction;
import org.openhab.binding.mymiio.entity.json.MiIoSendCommand;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The {@link MyMiIOHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author zaoweiceng - Initial contribution
 */
public class MyMiIOHandler extends MiIoHandler {

    private final Logger logger = LoggerFactory.getLogger(MyMiIOHandler.class);
    private boolean hasChannelStructure;
    private final ExpiringCache<Boolean> updateDateCache = new ExpiringCache<Boolean>(CACHE_EXPIRY, ()->{
        scheduler.schedule(this::updateData, 0, TimeUnit.SECONDS);
        return true;
    });
    List<MiIoBasicChannel> refreshList = new ArrayList<MiIoBasicChannel>();
    MiIoBasicDevice miIoDevice;
    private Map<String, MiIoDeviceAction> actions;
    @NonNullByDefault
    public MyMiIOHandler(Thing thing) {
        super(thing);
    }
    @Override
    public void initialize(){
        super.initialize();
        hasChannelStructure = false;
        isIdentified = false;
        refreshList = new ArrayList<MiIoBasicChannel>();
    }
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command == RefreshType.REFRESH){
            if (updateDateCache.isExpired()){
                updateDateCache.getValue();
            }
            return;
        }
        if (channelUID.getId().equals(CHANNEL_COMMAND)){
            cmds.put(sendCommand(command.toString()), command.toString());
        }
        if (actions != null){
            if (actions.containsKey(channelUID.getId())){
                String preCommandPara1 = actions.get(channelUID.getId()).getPreCommandParameter1();
                preCommandPara1 = ((preCommandPara1 != null && !preCommandPara1.isEmpty()) ?  preCommandPara1 + "," : "");
                String para1 = actions.get(channelUID.getId()).getParameter1();
                String para2 = actions.get(channelUID.getId()).getParameter2();
                String para3 = actions.get(channelUID.getId()).getParameter3();
                String para = "" + (para1 != null ? "," : "") +
                        (para2 != null ? "," : "") +
                        (para3 != null ? "," : "");
                String cmd = actions.get(channelUID.getId()).getCommand();
                CommandParameterType parameterType = actions.get(channelUID.getId()).getparameterType();
                if (parameterType == CommandParameterType.EMPTY){
                    cmd = cmd + "[]";
                }else if (command instanceof OnOffType){
                    if (parameterType == CommandParameterType.ONOFF){
                        cmd = cmd + "[" + preCommandPara1 + "\"" + command.toString().toLowerCase() + "\"" + para + "]";
                    }else if (parameterType == CommandParameterType.ONOFFPARA){
                        cmd = cmd.replace("*", command.toString().toLowerCase()) + "[]";
                    }else if (parameterType == CommandParameterType.ONOFFBOOL) {
                        boolean boolCommand = command == OnOffType.ON;
                        cmd = cmd + "[" + preCommandPara1 + "\"" + boolCommand + "\"" + para + "]";
                    } else {
                        cmd = cmd + "[]";
                    }
                }else if (command instanceof StringType){
                    if (parameterType == CommandParameterType.STRING) {
                        cmd = cmd + "[" + preCommandPara1 + "\"" + command.toString() + "\"" + para + "]";
                    } else if (parameterType == CommandParameterType.CUSTOMSTRING) {
                        cmd = cmd + "[" + preCommandPara1 + "\"" + command.toString() + para + "]";
                    }
                }else if (command instanceof DecimalType) {
                    cmd = cmd + "[" + preCommandPara1 + command.toString().toLowerCase() + para + "]";
                }
                sendCommand(cmd);
            }
        }
        updateDateCache.invalidateValue();
        updateData();
    }
    @Override
    protected void updateData() {
        try {
            if (!hasConnection() || skipUpdate()){
                return;
            }
            miioCom.startReciver();
            miioCom.sendPing(configuration.host);
            checkChannelStructure();
            if (!isIdentified){
                miioCom.queueCommand(MiIoCommand.MIIO_INFO);
            }
            if (miIoDevice != null){
                refreshProperties(miIoDevice);
                refreshNetwork();
            }
        }catch (Exception e){
            logger.debug("error : {}", e.getMessage());
        }
    }
    private boolean refreshProperties(MiIoBasicDevice device){
        MiIoCommand command = MiIoCommand.getCommand(device.getDeviceMapping().getPropertyMethod());
        int maxPro = device.getDeviceMapping().getMaxProperties();
        JsonArray getPropString = new JsonArray();
        for (MiIoBasicChannel miIoBasicChannel : refreshList){
            getPropString.add(miIoBasicChannel.getProperty());
            if (getPropString.size()>=maxPro){
                sendRefreshProperties(command, getPropString);
                getPropString = new JsonArray();
            }
        }
        if (getPropString.size()>0){
            sendRefreshProperties(command, getPropString);
        }
        return true;
    }
    private void sendRefreshProperties(MiIoCommand command, JsonArray getPropString){
        try {
            miioCom.queueCommand(command, getPropString.toString());
        } catch ( IOException e) {
            logger.debug("Send refresh failed {}", e.getMessage());
        }
    }
    private void checkChannelStructure(){
        if (!hasChannelStructure){
            if (configuration.model != null && !configuration.model.isEmpty()){
                hasChannelStructure = buildChannelStructure(configuration.model);
            }
        }
        if (hasChannelStructure){
            refreshList = new ArrayList<MiIoBasicChannel>();
            for (MiIoBasicChannel miIoBasicChannel : miIoDevice.getDeviceMapping().getChannels()){
                if (miIoBasicChannel.getRefresh()){
                    refreshList.add(miIoBasicChannel);
                }
            }
        }
    }
    private boolean buildChannelStructure(String deviceName){
        URL fn = findDatabaseEntry(deviceName);
        if (fn == null) {
            logger.info("Database entry for model '{}' cannot be found.", deviceName);
            return false;
        }
        try {
            JsonObject deviceMapping = Utils.convertFileToJSON(fn);
            Gson gson = new GsonBuilder().serializeNulls().create();
            miIoDevice = gson.fromJson(deviceMapping, MiIoBasicDevice.class);
            ThingBuilder thingBuilder = editThing();
            int channelsAdded = 0;
            actions = new HashMap<String, MiIoDeviceAction>();
            for (MiIoBasicChannel miChannel : miIoDevice.getDeviceMapping().getChannels()) {
                logger.debug("properties {}", miChannel);
                for (MiIoDeviceAction action : miChannel.getActions()) {
                    actions.put(miChannel.getChannel(), action);
                }
                if (miChannel.getType() != null) {
                    channelsAdded += addChannel(thingBuilder, miChannel.getChannel(), miChannel.getChannelType(),
                            miChannel.getType(), miChannel.getFriendlyName()) ? 1 : 0;
                }
            }
            if (channelsAdded > 0){
                updateThing(thingBuilder.build());
            }
            return true;
        }catch (Exception e){
            logger.info("error in read database : {} on : {}", deviceName, e.getMessage());
        }
        return false;
    }
    private boolean addChannel(ThingBuilder thingBuilder, String channel, String channelType, String datatype, String friendlyName){
        if (channel == null || channel.isEmpty() || datatype == null || datatype.isEmpty()){
            return false;
        }
        ChannelUID channelUID = new ChannelUID(getThing().getUID(), channel);
        ChannelTypeUID channelTypeUID = new ChannelTypeUID(channelType);
        if (getThing().getChannel(channel) != null){
            thingBuilder.withoutChannel(new ChannelUID(getThing().getUID(), channel));
        }
        Channel newChannel = ChannelBuilder.create(channelUID, datatype).withType(channelTypeUID).withLabel(friendlyName).build();
        thingBuilder.withChannel(newChannel);
        return true;
    }
    private URL findDatabaseEntry(String deviceName){
        URL fn;
        try {
            Bundle bundle = FrameworkUtil.getBundle(getClass());
            fn = bundle.getEntry(DATABASE_PATH + deviceName + ".json");
            return fn;
        }catch (Exception e){
            logger.info("error database : {}", e.getMessage());
        }
        return null;
    }
    private MiIoBasicChannel getChannel(String parameter){
        for (MiIoBasicChannel basicChannel : refreshList){
            if (basicChannel.getProperty().equals(parameter)){
                return basicChannel;
            }
        }
        return null;
    }
    private void updatePropsFromJsonArray(MiIoSendCommand response){
        JsonArray res = response.getResult().getAsJsonArray();
        JsonArray para = parser.parse(response.getCommandString()).getAsJsonObject().get("params").getAsJsonArray();
        if (res.size() != para.size()){
            logger.debug("Unexpected size different. Request size {},  response size {}. (Req: {}, Resp:{})",
                    para.size(), res.size(), para, res);
        }
        for (int i = 0; i < para.size(); i++) {
            String param = para.get(i).getAsString();
            JsonElement val = res.get(i);
            if (val.isJsonNull()) {
                logger.debug("Property '{}' returned null (is it supported?).", param);
                continue;
            }
            MiIoBasicChannel basicChannel = getChannel(param);
            updateChannel(basicChannel, param, val);
        }
    }
    private void updatePropsFromJsonObject(MiIoSendCommand response) {
        JsonObject res = response.getResult().getAsJsonObject();
        for (Object k : res.keySet()) {
            String param = (String) k;
            JsonElement val = res.get(param);
            if (val.isJsonNull()) {
                logger.debug("Property '{}' returned null (is it supported?).", param);
                continue;
            }
            MiIoBasicChannel basicChannel = getChannel(param);
            updateChannel(basicChannel, param, val);
        }
    }

    private void updateChannel(MiIoBasicChannel basicChannel, String param, JsonElement val){
        if (basicChannel != null) {
            if (basicChannel.getTransformation() != null) {
                JsonElement transformed = Conversions.execute(basicChannel.getTransformation(), val);
                val = transformed;
            }
            try {
                if (basicChannel.getType().equals("Number")) {
                    updateState(basicChannel.getChannel(), new DecimalType(val.getAsBigDecimal()));
                }
                if (basicChannel.getType().equals("String")) {
                    updateState(basicChannel.getChannel(), new StringType(val.getAsString()));
                }
                if (basicChannel.getType().equals("Switch")) {
                    updateState(basicChannel.getChannel(),
                            val.getAsString().toLowerCase().equals("on")
                                    || val.getAsString().toLowerCase().equals("true") ? OnOffType.ON
                                    : OnOffType.OFF);
                }
            } catch (Exception e) {
                logger.debug("Error updating {} property {} with '{}' : {}: {}", getThing().getUID(),
                        basicChannel.getChannel(), val, e.getClass().getCanonicalName(), e.getMessage());
                logger.trace("Property update error detail:", e);
            }
        } else {
            logger.debug("Channel not found for {}", param);
        }
    }

    @Override
    public void messageReceived(MiIoSendCommand response){
        super.messageReceived(response);
        if (response.isError()){
            return;
        }
        try{
            switch (response.getCommand()){
                case MIIO_INFO:
                    break;
                case GET_VALUE:
                case GET_PROPERTY:
                    if (response.getResult().isJsonArray()){
                        updatePropsFromJsonArray(response);
                    }else if (response.getResult().isJsonObject()){
                        updatePropsFromJsonObject(response);
                    }
                    break;
                default:
                    break;
            }
        }catch (Exception e){
            logger.info("Error while handing message {}", response.getResponse(), e);
        }
    }

}
