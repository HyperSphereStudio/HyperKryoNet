package com.hyperspherestudio.kryonet;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.ConnectionValues;
import com.esotericsoftware.kryonet.KryoNetException;
import com.hyperspherestudio.kryonet.Packets.PacketStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
//Author Johnathan Bizzano
public class Logic {

    public static void sendViaChannelUDP(byte[] array, SocketAddress socketAddress, DatagramChannel datagramChannel) throws IOException {
        sendViaChannelUDP(ByteBuffer.wrap(array), socketAddress, datagramChannel);
    }

    public static void sendViaChannelUDP(ByteBuffer byteBuffer, SocketAddress socketAddress, DatagramChannel datagramChannel) throws IOException {
        datagramChannel.send(byteBuffer, socketAddress);
    }

    public static void sendViaChannelTCP(ByteBuffer byteBuffer, SocketChannel socketChannel) throws IOException {
        if (socketChannel != null) {
            while (byteBuffer.hasRemaining()) {
                if (socketChannel.write(byteBuffer) == 0) break;
            }
            byteBuffer.position(0);
        }
    }

    public static void sendViaChannelTCP(byte[] array, SocketChannel socketChannel) throws IOException {
        sendViaChannelTCP(ByteBuffer.wrap(array), socketChannel);
    }

    public static void sendViaChannelTCPHeaded(int bufferSize, byte[] array, SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
        int start = byteBuffer.position();
        byteBuffer.position(byteBuffer.position() + 4);
        byteBuffer.put(array);
        int end = byteBuffer.position();
        byteBuffer.position(start);
        writeLength(byteBuffer, end - 4 - start);
        byteBuffer.position(end);
        byteBuffer.flip();
        sendViaChannelTCP(byteBuffer, socketChannel);
    }

    public static String getAlphaNumericString(int n) {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz!@#$%^&*()<>,.?/':;[]{}|`~";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {

            int index
                    = (int) (AlphaNumericString.length()
                    * Math.random());
            sb.append(AlphaNumericString
                    .charAt(index));
        }
        return sb.toString();
    }

    public static void getSerialization(Object o, Object lock, boolean TCP, Output output, Kryo kryo) {
        if (TCP) {
            int start = output.position();
            try {
                output.setPosition(output.position() + 4);
                synchronized (lock) {
                    kryo.writeClassAndObject(output, o);
                }
                int end = output.position();
                output.setPosition(start);
                output.writeInt(end - 4 - start);
                output.setPosition(end);
            } catch (Throwable ex) {
                throw new KryoNetException("Error serializing object of type: " + o.getClass().getName(), ex);
            }
        } else {
            try {
                synchronized (lock) {
                    kryo.writeClassAndObject(output, o);
                }
            } catch (Exception ex) {
                throw new KryoNetException("Error serializing object of type: " + o.getClass().getName(), ex);
            }
        }
    }

    public static ByteBuffer getSerialization(Object o, int bufferSize, Object lock, boolean TCP, ByteBufferOutput output, Kryo kryo) {
        ByteBuffer writeBuffer = ByteBuffer.allocate(bufferSize);
        if (TCP) {
            int start = writeBuffer.position();
            try {
                writeBuffer.position(writeBuffer.position() + 4);
                synchronized (lock) {
                    output.setBuffer(writeBuffer);
                    kryo.writeClassAndObject(output, o);
                    output.flush();
                }
                int end = writeBuffer.position();
                writeBuffer.position(start);
                writeLength(writeBuffer, end - 4 - start);
                writeBuffer.position(end);
                writeBuffer.flip();
                return writeBuffer;
            } catch (Throwable ex) {
                throw new KryoNetException("Error serializing object of type: " + o.getClass().getName(), ex);
            }
        } else {
            try {
                synchronized (lock) {
                    output.setBuffer(writeBuffer);
                    kryo.writeClassAndObject(output, o);
                    output.flush();
                }
                writeBuffer.flip();
                return writeBuffer;
            } catch (Exception ex) {
                throw new KryoNetException("Error serializing object of type: " + o.getClass().getName(), ex);
            }
        }
    }

    public static void writeLength(ByteBuffer buffer, int length) {
        buffer.putInt(length);
    }

    public static void sendPacketStream(List<? extends Connection> list, Object o, Kryo kryo, int cutSize, int MAXSIZE) {
        Output output = new Output(Math.max(MAXSIZE, cutSize));
        if (cutSize <= 50) throw new RuntimeException("Cut Size to Small! Min Size:51");
        kryo.writeClassAndObject(output, o);
        byte[] array = output.toBytes();

        if (array.length >= cutSize - 50) {
            new Thread(() -> {
                PacketStream packetStream = new PacketStream();
                packetStream.maxSize = array.length;
                Output writeBuffer = new Output(cutSize);
                int rawTime = Math.max(1, array.length / 1500000);
                int index = 0;
                int i = 0;

                while (array.length > index) {
                    writeBuffer.clear();
                    int size = Math.min(cutSize - 50 + index, array.length);
                    int indexAdder = Math.min(cutSize - 50, array.length - index);
                    packetStream.objectData = Arrays.copyOfRange(array, index, size);

                    kryo.writeClassAndObject(writeBuffer, packetStream);

                    for (Connection connection : list) {
                        try {
                            if (connection.isConnected()) {
                                sendViaChannelTCPHeaded(cutSize, writeBuffer.toBytes(), ConnectionValues.getSocket(connection));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    index += indexAdder;

                    if (i++ == 2) {
                        try {
                            Thread.sleep(rawTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        i = 0;
                    }
                }
                packetStream.release();
            }).start();
        } else {
            try {
                for (Connection connection : list) {
                    if (connection.isConnected())
                        Logic.sendViaChannelTCPHeaded(cutSize, array, ConnectionValues.getSocket(connection));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendPacketStream(Object o, Kryo kryo, int cutSize, int MAXSIZE, Connection... c) {
        Output output = new Output(Math.max(MAXSIZE, cutSize));
        if (cutSize <= 50) throw new RuntimeException("Cut Size to Small! Min Size:51");
        kryo.writeClassAndObject(output, o);
        byte[] array = output.toBytes();

        if (array.length >= cutSize - 50) {
            new Thread(() -> {
                PacketStream packetStream = new PacketStream();
                packetStream.maxSize = array.length;
                Output writeBuffer = new Output(cutSize);
                int rawTime = Math.max(1, array.length / 1500000);
                int index = 0;
                int i = 0;

                while (array.length > index) {
                    writeBuffer.clear();
                    int size = Math.min(cutSize - 50 + index, array.length);
                    int indexAdder = Math.min(cutSize - 50, array.length - index);
                    packetStream.objectData = Arrays.copyOfRange(array, index, size);

                    kryo.writeClassAndObject(writeBuffer, packetStream);

                    for (Connection connection : c) {
                        try {
                            if (connection.isConnected()) {
                                sendViaChannelTCPHeaded(cutSize, writeBuffer.toBytes(), ConnectionValues.getSocket(connection));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    index += indexAdder;

                    if (i++ == 2) {
                        try {
                            Thread.sleep(rawTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        i = 0;
                    }
                }
                packetStream.release();
            }).start();
        } else {
            try {
                for (Connection connection : c) {
                    if (connection.isConnected())
                        Logic.sendViaChannelTCPHeaded(cutSize, array, ConnectionValues.getSocket(connection));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
