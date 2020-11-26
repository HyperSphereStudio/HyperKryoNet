package com.hyperspherestudio.kryonet.Networking;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.ConnectionValues;
import com.esotericsoftware.kryonet.KryoSerialization;
import com.hyperspherestudio.kryonet.Logic;
import com.hyperspherestudio.kryonet.ObjectHandling.HyperKryo;
import com.hyperspherestudio.kryonet.Packets.EncryptedPacket;
import com.hyperspherestudio.kryonet.Packets.Packet;
import com.hyperspherestudio.kryonet.Packets.PacketStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

//Author Johnathan Bizzano


public class HyperClient extends Client {
    public int readBuffer, writeBuffer;
    public Output buffer;
    public Object bufferLock = new Object();
    private final HashMap<String, byte[]> serializationCache = new HashMap<>();
    private final HyperKryo hyperKryo;

    public HyperClient(int write_buffer, int read_buffer) {
        super(write_buffer, read_buffer, new KryoSerialization(new HyperKryo()));
        hyperKryo = (HyperKryo) getKryo();
        this.readBuffer = read_buffer;
        this.writeBuffer = write_buffer;
        register(PacketStream.class);
        register(EncryptedPacket.class);
        register(byte[].class);
        register(Packet.class);
        register(ArrayList.class);
        buffer = new Output(write_buffer);

    }


    public void register(Class c){
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
        buffer.clear();
    }

    public void storeUDPSerialization(String s, Object o) {
        getSerialization(o, false);
        serializationCache.put(s, buffer.getBuffer());
        buffer.clear();
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

    public void sendPacketViaOutputStream(Object o) {
        sendPacketViaOutputStream(o, PacketStream.MAXSIZE);
    }

    public void sendPacketViaOutputStream(Object o, int maxsize) {
        Logic.sendPacketStream(o, getKryo(), writeBuffer, maxsize, this);
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

    public void reconnectLoop(long time) {
        if (!isReconnecting) {
            isReconnecting = true;
            while (!isConnected() && isReconnecting) {
                try {
                    reconnect();
                    Thread.sleep(time);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            isReconnecting = false;
        }
    }

    public void killReconnection() {
        isReconnecting = false;
    }

    public boolean isReconnecting;
}
