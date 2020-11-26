package com.hyperspherestudio.kryonet.Packets;
import com.esotericsoftware.kryonet.Connection;

import java.io.Serializable;
import java.util.List;
//Author Johnathan Bizzano
public class Packet implements Serializable {
    public Object o, o2;
    public int[] i;
    public boolean[] b;
    public short t = -1;

    public Packet() {

    }

    public Packet(short type) {
        set(type);
    }

    public Packet(short type, Object o) {
        set(type, o);
    }

    public Packet(short type, Object o, Object o2) {
        set(type, o, o2);
    }

    public Packet(short type, int[] array) {
        set(type, array);
    }

    public Packet(short type, int[] array, Object o) {
        this(type, array);
        this.o = o;
    }

    public Packet(short type, boolean[] array) {
        this(type);
        this.b = array;
    }

    public Packet(short type, int[] array, Object o, Object o2) {
        this(type, array, o);
        this.o2 = o2;
    }

    public Packet send(Connection c) {
        c.sendTCP(this);
        return this;
    }


    public Packet sendConnections(List<? extends Connection> connections){
        for(Connection c : connections){
            c.sendTCP(this);
        }
        return this;
    }

    public static Packet newP(){
        return new Packet();
    }

    public static void sendPacket(Connection c, short t) {
        newP().set(t).send(c);
    }

    public static void sendPacket(Connection c, short t, Object o) {
        newP().set(t, o).send(c);
    }

    public static void sendPacket(Connection c, short t, Object o, Object o2) {
        newP().set(t, o, o2).send(c);
    }

    public static void sendPacket(Connection c, short t, boolean[] b) {
        newP().set(t, b).send(c);
    }

    public static void sendPacket(Connection c, short t, Object o, Object o2, int[] array) {
        newP().set(t, o, o2, array).send(c);
    }

    public static void sendPacket(Connection c, short t, Object o, int[] array) {
        newP().set(t, o, array).send(c);
    }

    public static void sendPacket(Connection c, short t, int[] array) {
        newP().set(t, array).send(c);
    }



    public Packet set(short type, Object o, Object o2) {
        set(type, o);
        this.o2 = o2;
        return this;
    }

    public Packet set(short type, boolean[] array) {
        set(type);
        this.b = array;
        return this;
    }

    public Packet set(short type, Object o, Object o2, int[] array) {
        set(type, o, o2);
        this.i = array;
        return this;
    }

    public Packet set(short type, Object o) {
        set(type);
        this.o = o;
        return this;
    }

    public Packet set(short type, Object o, int[] array) {
        set(type, o);
        this.i = array;
        return this;
    }

    public Packet set(short type) {
        this.t = type;
        return this;
    }

    public Packet set(short type, int[] array) {
        set(type);
        this.i = array;
        return this;
    }

}
