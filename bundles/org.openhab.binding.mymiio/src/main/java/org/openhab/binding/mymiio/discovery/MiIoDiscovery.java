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

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.net.NetUtil;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.mymiio.internal.Message;
import org.openhab.binding.mymiio.internal.Utils;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.mymiio.internal.MyMiIOBindingConstants.*;

/**
 * Discovers Mi IO devices
 *
 * @author zaoweiceng - Initial contribution
 *
 */
@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.miio")
public class MiIoDiscovery extends AbstractDiscoveryService {

    private static final long SEARCH_INTERVAL = 600;
    private static final int BUFFER_LENGTH = 1024;
    private static final int DISCOVERY_TIME = 10;

    private ScheduledFuture<?> miIoDiscoveryJob;
    protected DatagramSocket clientSocket;
    private Thread socketReceiveThread;
    Set<String> responceIps = new HashSet<String>();
    private final Logger logger = LoggerFactory.getLogger(MiIoDiscovery.class);


    public MiIoDiscovery() {
        super(DISCOVERY_TIME);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes(){
        return SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    protected void startBackgroundDiscovery(){
        if (miIoDiscoveryJob == null || miIoDiscoveryJob.isCancelled()){
            miIoDiscoveryJob = scheduler.scheduleWithFixedDelay(()->discover(), 0, SEARCH_INTERVAL, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery(){
        if (miIoDiscoveryJob != null && !miIoDiscoveryJob.isCancelled()){
            miIoDiscoveryJob.cancel(true);
            miIoDiscoveryJob = null;
        }
    }

    @Override
    protected void deactivate(){
        stopReceiverTread();
        if (clientSocket != null){
            clientSocket.close();
            clientSocket = null;
        }
        super.deactivate();
    }

    private void discover(){
        startReceiverThread();
        responceIps = new HashSet<String >();
        HashSet<String> broadcastAddress = new HashSet<String>();
        broadcastAddress.add("224.0.0.1");
        broadcastAddress.add("224.0.0.50");
        broadcastAddress.addAll(NetUtil.getAllBroadcastAddresses());
        for (String address : broadcastAddress){
            sendDiscoveryRequest(address);
        }
    }

    private void sendDiscoveryRequest(String ipAddress){
        try {
            byte[] sendData = DISCOVER_STRING;
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(ipAddress), PORT);
            getSocket().send(sendPacket);
        }catch (Exception e){
            logger.info("{}", e.getMessage());
        }
    }

    private synchronized void startReceiverThread(){
        stopReceiverTread();
        socketReceiveThread = new ReceiverThread();
        socketReceiveThread.start();
    }

    private synchronized void stopReceiverTread(){
        if (socketReceiveThread != null){
            closeSocket();
            socketReceiveThread = null;
        }
    }

    private void closeSocket(){
        if (clientSocket == null){
            return;
        }
        clientSocket.close();
        clientSocket = null;
    }
    @Override
    protected void startScan() {
        getSocket();
        discover();
    }

    private class ReceiverThread extends Thread{
        @Override
        public void run(){
            receiveData(getSocket());
        }
        private void receiveData(DatagramSocket socket){
            DatagramPacket receivePacket = new DatagramPacket(new byte[BUFFER_LENGTH], BUFFER_LENGTH);
            try {
                while (true){
                    socket.receive(receivePacket);
                    String hostAddress = receivePacket.getAddress().getHostAddress();
                    byte[] messageBuf = Arrays.copyOfRange(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getOffset() + receivePacket.getLength());
                    if (!responceIps.contains(hostAddress)){
                        scheduler.schedule(()->{
                            try {
                                discovered(hostAddress, messageBuf);
                            }catch (Exception e){
                                logger.info("{}", e.getMessage());
                            }
                        }, 0, TimeUnit.SECONDS);
                    }
                    responceIps.add(hostAddress);
                }
            }catch (Exception e){
                logger.info("{}", e.getMessage());
            }
        }
    }

    private void discovered(String ip ,byte[] respose){
        Message msg = new Message(respose);
        String token = Utils.getHex(msg.getChecksum());
        String id = Utils.getHex(msg.getDeviceId());
        String label = "Xiaomi Mi Device " + id + " (" + Long.parseUnsignedLong(id, 16) + ")";
        ThingUID uid = new ThingUID(THING_TYPE_MIIO, id);
        if (IGNORED_TOKENS.contains(token)){
            thingDiscovered(DiscoveryResultBuilder.create(uid).withProperty(PROPERTY_HOST_IP, ip).withProperty(PROPERTY_DID, id).withRepresentationProperty(id).withLabel(label).build());
        }else{
            thingDiscovered(DiscoveryResultBuilder.create(uid).withProperty(PROPERTY_HOST_IP, ip)
                    .withProperty(PROPERTY_DID, id).withProperty(PROPERTY_TOKEN, token).withRepresentationProperty(id)
                    .withLabel(label + " with token").build());
        }
    }

    public synchronized DatagramSocket getSocket(){
        if (clientSocket != null && clientSocket.isBound()){
            return clientSocket;
        }
        try{
            DatagramSocket socket = new DatagramSocket();
            socket.setReuseAddress(true);
            socket.setBroadcast(true);
            this.clientSocket = socket;
            return socket;
        }catch (Exception e){

        }
        return null;
    }

}
