package com.hyperspherestudio.kryonet.Networking;

import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.ConnectionValues;
import com.esotericsoftware.kryonet.KryoSerialization;
import com.esotericsoftware.kryonet.Server;
import com.hyperspherestudio.kryonet.Logic;
import com.hyperspherestudio.kryonet.ObjectHandling.HyperKryo;
import com.hyperspherestudio.kryonet.Packets.EncryptedPacket;
import com.hyperspherestudio.kryonet.Packets.Packet;
import com.hyperspherestudio.kryonet.Packets.PacketStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
//Author Johnathan Bizzano
public class HyperServer extends Server {

    public int readBuffer, writeBuffer, startBatchAt;
    private HashMap<String, byte[]> serializationCache = new HashMap<>();
    public Output buffer;
    public Object bufferLock = new Object();
    private HyperKryo hyperKryo;

    public HyperServer(int write_buffer, int read_buffer, int startBatchAt) {
        super(write_buffer, read_buffer, new KryoSerialization(new HyperKryo()));
        hyperKryo = (HyperKryo) getKryo();
        this.writeBuffer = write_buffer;
        this.readBuffer = read_buffer;
        this.startBatchAt = startBatchAt;
        register(Packet.class);
        register(PacketStream.class);
        register(EncryptedPacket.class);
        register(byte[].class);
        register(ArrayList.class);
        buffer = new Output(write_buffer);
    }


    public void register(Class c) {
        hyperKryo.register(c);
    }

    public void clearSerializations() {
        serializationCache.clear();
    }

    public byte[] getSerialization(String s) {
        return serializationCache.get(s);
    }

    public void removeSerialization(String s) {
        serializationCache.remove(s);
    }

    public void storeTCPSerialization(String s, Object o) {
        getSerialization(o, true);
        serializationCache.put(s, buffer.getBuffer());
        clearBuffer();
    }

    public void storeUDPSerialization(String s, Object o) {
        getSerialization(o, false);
        serializationCache.put(s, buffer.getBuffer());
        clearBuffer();
    }

    public void sendSerializationTCP(String s, Connection c) throws IOException {
        sendSerializationTCP(getSerialization(s), c);
    }

    public void sendSerializationUDP(String s, Connection c) throws IOException {
        sendSerializationUDP(getSerialization(s), c);
    }

    public void sendSerializationTCP(byte[] array, Connection connection) throws IOException {
        Logic.sendViaChannelTCP(array, ConnectionValues.getSocket(connection));
    }

    public void sendSerializationUDP(byte[] array, Connection connection) throws IOException {
        Logic.sendViaChannelUDP(array, ConnectionValues.getAddress(connection), ConnectionValues.getUDPChannel(connection));
    }

    public void getSerialization(Object o, boolean TCP) {
        Logic.getSerialization(o, bufferLock, TCP, buffer, getKryo());
    }

    public void clearBuffer() {
        buffer.clear();
    }

    @Override
    public void sendToAllExceptTCP(int connectionID, Object packet) {
        try {
            if (getConnections().length >= startBatchAt) {
                getSerialization(packet, true);
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer.toBytes());
                clearBuffer();
                for (Connection connection : getConnections()) {
                    if (connection.getID() != connectionID)
                        Logic.sendViaChannelTCP(byteBuffer, ConnectionValues.getSocket(connection));
                }
            } else {
                for (Connection connection : getConnections()) {
                    if (connection.getID() != connectionID)
                        connection.sendTCP(packet);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendToAllTCP(Object packet) {
        sendToAllExceptTCP(-Integer.MAX_VALUE, packet);
    }

    @Override
    public void sendToAllExceptUDP(int connectionID, Object packet) {
        try {
            if (getConnections().length >= startBatchAt) {
                getSerialization(packet, false);
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer.toBytes());
                clearBuffer();
                for (Connection connection : getConnections()) {
                    if (connection.getID() != connectionID)
                        Logic.sendViaChannelUDP(byteBuffer, ConnectionValues.getAddress(connection), ConnectionValues.getUDPChannel(connection));
                }
            } else {
                for (Connection connection : getConnections()) {
                    if (connection.getID() != connectionID)
                        connection.sendUDP(packet);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendToAllUDP(Object packet) {
        sendToAllExceptUDP(-Integer.MAX_VALUE, packet);
    }

    public void sendPacketViaOutputStream(Object o, List<? extends Connection> list) {
        sendPacketViaOutputStream(o, PacketStream.MAXSIZE, list);
    }

    public void sendPacketViaOutputStream(Object o, int maxsize, List<? extends Connection> list) {
        Logic.sendPacketStream(list, o, getKryo(), writeBuffer, maxsize);
    }

    public void sendPacketViaOutputStream(Object o, Connection... c) {
        sendPacketViaOutputStream(o, PacketStream.MAXSIZE, c);
    }

    public void sendPacketViaOutputStream(Object o, int maxsize, Connection... c) {
        Logic.sendPacketStream(o, getKryo(), writeBuffer, maxsize, c);
    }

    public CachedPacketStream getPacketStreamCache(Object o) {
        return new CachedPacketStream(this.getKryo(), writeBuffer, PacketStream.MAXSIZE, o);
    }

    public CachedPacketStream getPacketStreamCache(Object o, int MAXSIZE) {
        return new CachedPacketStream(this.getKryo(), writeBuffer, MAXSIZE, o);
    }

    public CachedPacketStream getPacketStreamCache() {
        return new CachedPacketStream(this.getKryo(), writeBuffer);
    }

    public CachedPacketStream getPacketStreamCache(int MAXSIZE) {
        return new CachedPacketStream(this.getKryo(), writeBuffer, MAXSIZE);
    }
}

