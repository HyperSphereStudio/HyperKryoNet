package com.hyperspherestudio.kryonet.Networking;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryonet.Connection;
import com.hyperspherestudio.kryonet.Networking.HyperListener;
import com.hyperspherestudio.kryonet.Packets.PacketStream;
//Author Johnathan Bizzano
public class InterPacketStream {
    public byte[] array;
    public int index;
    public Connection c;

    public InterPacketStream(PacketStream packetStream, Connection c) {
        array = new byte[packetStream.maxSize];
        index = 0;
        this.c = c;
        HyperListener.packetStreamHashMap.put(packetStream.idNum, this);
    }

    public Object deserialize(Kryo kryo) {
        return kryo.readClassAndObject(new Input(array));
    }
}

