package com.hyperspherestudio.kryonet.JVMManager;

import java.io.Serializable;

public class JVMConnectionPacket implements Serializable {
    public Object o;
    public int JVM_ID, FROM_JVM_ID;
    public short dispersal;
    private transient JVMConnection jvmConnection;
    public static final short DISPERSAL_ALL = 0, DISPERSAL_ONE = 1;
    public static JVMConnection defaultConnection;


    public JVMConnectionPacket() {

    }

    public JVMConnectionPacket(short dispersal, JVMConnection jvmConnection, int JVM_ID, Object o) {
        this.JVM_ID = JVM_ID;
        this.dispersal = dispersal;
        this.jvmConnection = jvmConnection;
        this.o = o;
    }

    public JVMConnectionPacket(short dispersal, JVMConnection jvmConnection, Object o) {
        this(dispersal, jvmConnection, jvmConnection.defaultConnectionID, o);
    }

    public JVMConnectionPacket(short dispersal, Object o) {
        this(dispersal, defaultConnection, o);
    }

    public JVMConnectionPacket(Object o){
        this(DISPERSAL_ONE, o);
    }

    public JVMConnectionPacket(JVMConnection jvmConnection, Object o){
        this(DISPERSAL_ONE, jvmConnection, o);
    }

    public JVMConnectionPacket(JVMConnection jvmConnection, int JVM_ID, Object o){
        this(DISPERSAL_ONE, jvmConnection, JVM_ID, o);
    }

    public JVMConnectionPacket(int JVM_ID, Object o){
        this(defaultConnection, JVM_ID, o);
    }

    public static void setDefaultConnection(JVMConnection jvmConnection) {
        defaultConnection = jvmConnection;
    }

    public void send() {
        jvmConnection.sendPacket(this);
    }

    public Object getObject() {
        return o;
    }
}
