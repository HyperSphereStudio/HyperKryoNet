package com.esotericsoftware.kryonet;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
//Author Johnathan Bizzano
public class ConnectionValues {


    public static SocketChannel getSocket(Connection c){
        return getTCPConnection(c).socketChannel;
    }

    public static DatagramChannel getUDPChannel(Connection c){
        return c.udp.datagramChannel;
    }

    public static InetSocketAddress getAddress(Connection c){
        if (c.udpRemoteAddress == null && c.udp != null) return c.udp.connectedAddress;
        return c.udpRemoteAddress;
    }

    public static TcpConnection getTCPConnection(Connection c){
        return c.tcp;
    }

    public static void flushTCPBuffer(Connection c){
        try {
            c.tcp.writeOperation();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static ByteBuffer getBuffer(Connection c){
        return c.tcp.writeBuffer;
    }


}
