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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import static org.openhab.binding.mymiio.internal.MyMiIOBindingConstants.BINDING_ID;

/**
 * Mapping properties from json
 *
 * @author zaoweiceng - Initial contribution
 */
public class MiIoBasicChannel {
    @SerializedName("property")
    @Expose
    private String property;
    @SerializedName("friendlyName")
    @Expose
    private String friendlyName;
    @SerializedName("channel")
    @Expose
    private String channel;
    @SerializedName("channelType")
    @Expose
    private String channelType;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("refresh")
    @Expose
    private Boolean refresh;
    @SerializedName("transformation")
    @Expose
    private String transformation;
    @SerializedName("ChannelGroup")
    @Expose
    private String channelGroup;
    @SerializedName("actions")
    @Expose
    private List<MiIoDeviceAction> miIoDeviceActions = new ArrayList<MiIoDeviceAction>();

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getFriendlyName() {
        return type == null || friendlyName.isEmpty() ? channel : friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannelType() {
        return channelType == null || channelType.isEmpty() ? BINDING_ID + ":" + channel
                : (channelType.startsWith("system") ? channelType : BINDING_ID + ":" + channelType);
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public String getType() {
        return type == null ? "" : type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getRefresh() {
        return refresh && !property.isEmpty();
    }

    public void setRefresh(Boolean refresh) {
        this.refresh = refresh;
    }

    public String getChannelGroup() {
        return channelGroup;
    }

    public void setChannelGroup(String channelGroup) {
        this.channelGroup = channelGroup;
    }

    public List<MiIoDeviceAction> getActions() {
        return miIoDeviceActions;
    }

    public void setActions(List<MiIoDeviceAction> miIoDeviceActions) {
        this.miIoDeviceActions = miIoDeviceActions;
    }

    public String getTransformation() {
        return transformation;
    }

    public void setTransformation(String transformation) {
        this.transformation = transformation;
    }

    @Override
    public String toString() {
        return "[ Channel = " + channel + ", friendlyName = " + friendlyName + ", type = " + type + ", channelType = "
                + getChannelType() + ", ChannelGroup = " + channelGroup + ", channel = " + channel + ", property = "
                + property + ", refresh = " + refresh + "]";
    }
}