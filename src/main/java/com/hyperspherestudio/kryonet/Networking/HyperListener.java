package com.hyperspherestudio.kryonet.Networking;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.hyperspherestudio.kryonet.Packets.PacketStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
//Author Johnathan Bizzano
public abstract class HyperListener extends Listener {

    public static ConcurrentHashMap<String, InterPacketStream> packetStreamHashMap = new ConcurrentHashMap<>();
    public Kryo kryo;

    public HyperListener(Kryo kryo){
        this.kryo = kryo;
    }

    public void disconnected(Connection c){
        Disconnected(c);
        for(Map.Entry<String, InterPacketStream> interPacketStream : packetStreamHashMap.entrySet()){
            if(!interPacketStream.getValue().c.isConnected()){
                packetStreamHashMap.remove(interPacketStream.getKey());
            }
        }
    }

    public abstract void Disconnected(Connection c);

    public void connected(Connection c){
        Connected(c);
    }

    public abstract void Connected(Connection c);

    public void received(Connection c, Object o){
        if(o instanceof PacketStream){
            addArrayToPacketStream(c, (PacketStream) o);
        } else Received(c, o);
    }

    public abstract void Received(Connection c,Object o);

    private void addArrayToPacketStream(Connection c, PacketStream packetStream){
        if(!packetStreamHashMap.containsKey(packetStream.idNum))new InterPacketStream(packetStream, c);
        InterPacketStream interPacketStream = packetStreamHashMap.get(packetStream.idNum);

        System.arraycopy(packetStream.objectData, 0, interPacketStream.array, interPacketStream.index, packetStream.objectData.length);

        interPacketStream.index += packetStream.objectData.length;
        if(interPacketStream.index == interPacketStream.array.length){
            Received(c, interPacketStream.deserialize(kryo));
            packetStreamHashMap.remove(packetStream.idNum);
        }

    }
}
