package com.hyperspherestudio.kryonet.Networking;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.ConnectionValues;
import com.hyperspherestudio.kryonet.Logic;
import com.hyperspherestudio.kryonet.Packets.PacketStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class CachedPacketStream {
    public ByteBuffer[] byteBuffers;
    public Object singlePacket;
    public int maxSize, cutSize;
    private Output output;
    private Object o = new Object();
    private PacketStream packetStream = new PacketStream();
    private final Kryo kryo;

    public CachedPacketStream(Kryo kryo, int cutSize, int maxSize, Object o) {
        this(kryo, cutSize, maxSize);
        setData(o);
    }

    public CachedPacketStream(Kryo kryo, int cutSize, Object o) {
        this(kryo, cutSize, PacketStream.MAXSIZE, o);
    }

    public CachedPacketStream(Kryo kryo, int cutSize) {
        this(kryo, cutSize, PacketStream.MAXSIZE, 3);
    }

    public CachedPacketStream(Kryo kryo, int cutSize, int MAXSIZE) {
        output = new Output(Math.max(MAXSIZE, cutSize));
        this.cutSize = cutSize;
        this.kryo = kryo;
    }

    public void setData(Object o) {
        singlePacket = null;
        if (cutSize <= 50) throw new RuntimeException("Cut Size to Small! Min Size:51");
        kryo.writeClassAndObject(output, o);
        byte[] array = output.toBytes();
        maxSize = array.length;
        packetStream.maxSize = maxSize;
        byteBuffers = null;

        if (array.length >= cutSize - 50) {
            int index = 0;

            ArrayList<ByteBuffer> arrayList = new ArrayList<>();
            Output writeBuffer = new Output(cutSize);

            while (array.length > index) {
                int size = Math.min(cutSize - 50 + index, array.length);
                int indexAdder = Math.min(cutSize - 50, array.length - index);
                packetStream.objectData = Arrays.copyOfRange(array, index, size);
                Logic.getSerialization(packetStream, this.o, true, writeBuffer, kryo);
                arrayList.add(ByteBuffer.wrap(writeBuffer.toBytes()));
                writeBuffer.clear();
                index += indexAdder;
            }
            byteBuffers = arrayList.toArray(new ByteBuffer[0]);
        } else {
            singlePacket = o;
        }

        output.clear();
    }

    public void sendConnection(Connection c, boolean flushOnFinish) {
        if (singlePacket != null) {
            c.sendTCP(singlePacket);
            if (flushOnFinish) flush();
        } else {
            new Thread(() -> {
                try {
                    synchronized (c) {
                        sendConnectionRaw(c, flushOnFinish);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public void sendConnectionRaw(Connection c, boolean flush) throws InterruptedException {
        int rawTime = Math.max(1, maxSize / 1500000);
        int i = 0;

        for (ByteBuffer byteBuffer : byteBuffers) {
            if (c.isConnected()) {
                try {
                    Logic.sendViaChannelTCP(byteBuffer, ConnectionValues.getSocket(c));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else break;

            if (i++ == 2) {
                Thread.sleep(rawTime);
                i = 0;
            }
        }

        Thread.sleep(rawTime);

        if (flush) flush();
    }

    public void flush() {
        byteBuffers = null;
        maxSize = 0;
        output.clear();
        singlePacket = null;
        packetStream.release();
        System.gc();
    }

    public int getBytes() {
        if (byteBuffers != null) {
            return maxSize;
        }

        return 0;
    }


}
