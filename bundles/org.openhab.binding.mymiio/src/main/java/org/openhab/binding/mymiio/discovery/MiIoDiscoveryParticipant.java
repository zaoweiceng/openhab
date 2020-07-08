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
package org.openhab.binding.mymiio.discovery;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.mymiio.entity.Devices;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.ServiceInfo;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.openhab.binding.mymiio.internal.MyMiIOBindingConstants.*;

/**
 * Discovers Mi IO devices
 *
 * @author zaoweiceng - Initial contribution
 *
 */
@Component(service = MDNSDiscoveryParticipant.class, immediate = true)
public class MiIoDiscoveryParticipant implements MDNSDiscoveryParticipant{
    private final Logger logger = LoggerFactory.getLogger(MiIoDiscoveryParticipant.class);

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return (NONGENERIC_THING_TYPES_UIDS);
    }

    @Override
    public String getServiceType() {
        return "_miio._udp.local.";
    }

    @Override
    public @Nullable DiscoveryResult createResult(ServiceInfo service) {
        DiscoveryResult result = null;
        ThingUID uid = getThingUID(service);
        if (uid != null){
            Map<String, Object> properties = new HashMap<>(2);
            InetAddress ip = getIpAddress(service);
            if (ip == null){
                return null;
            }
            String inetAddress = ip.toString().substring(1);
            String id = uid.getId();
            String label = "Xiaomi Mi Device " + id + " (" + Long.parseUnsignedLong(id, 16) + ") " + service.getName();
            properties.put(PROPERTY_HOST_IP, inetAddress);
            properties.put(PROPERTY_DID, id);
            result = DiscoveryResultBuilder.create(uid).withProperties(properties).withRepresentationProperty(id).withLabel(label).build();
        }
        return result;
    }

    @Override
    public @Nullable ThingUID getThingUID(ServiceInfo service) {
        if (service == null){
            return null;
        }
        String id[] = service.getName().split("_miio");
        if (id.length != 2){
            return null;
        }
        int did;
        try{
            did = Integer.parseUnsignedInt(id[1]);
        }catch (Exception e){
            return null;
        }
        ThingTypeUID thingType = Devices.getType(id[0].replace("-", ",")).getThingType();
        String uidName = String.format("%08x",did);
        return new ThingUID(thingType, uidName);
    }
    private InetAddress getIpAddress(ServiceInfo service){
        InetAddress address = null;
        for (InetAddress addr : service.getInet4Addresses()){
            return addr;
        }
        for (InetAddress addr : service.getInet6Addresses()){
            return addr;
        }
        return address;
    }
}

