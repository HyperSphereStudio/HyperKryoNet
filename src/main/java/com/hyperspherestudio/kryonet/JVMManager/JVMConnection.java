package com.hyperspherestudio.kryonet.JVMManager;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.hyperspherestudio.kryonet.Networking.HyperClient;
import com.hyperspherestudio.kryonet.Networking.HyperListener;
import com.hyperspherestudio.kryonet.Networking.HyperServer;
import com.hyperspherestudio.kryonet.ObjectHandling.HyperKryo;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
//Author Johnathan Bizzano
public class JVMConnection {
    protected final int connectionID;
    public final int defaultConnectionID;
    protected HyperServer hyperServer;
    protected HyperClient hyperSphereClient;
    protected boolean serverMode;
    public ArrayList<Integer> availableConnections = new ArrayList<>();
    public HashMap<Integer, Connection> connectionHashMap = new HashMap<>();
    public JVMListener jvmListener;
    public static int BUFFER_SIZE = 500000;
    public HashMap<String, SharedVariable> sharedVariables = new HashMap<>();
    protected Thread thread;
    protected boolean sleeping;

    public JVMConnection(String ForeignIP, JVMListener jvmListener, int defaultConnectionID, int connectionID, int connectionPort, boolean setPacketDefault) throws IOException {
        this.jvmListener = jvmListener;
        this.connectionID = connectionID;
        this.defaultConnectionID = defaultConnectionID;

        if (setPacketDefault) {
            JVMConnectionPacket.setDefaultConnection(this);
        }
        if (isTcpPortAvailable(connectionPort) && notForeignIP(ForeignIP)) {
            serverMode = true;
            hyperServer = new HyperServer(BUFFER_SIZE, BUFFER_SIZE, 2);
            try {
                hyperServer.bind(connectionPort);
                hyperServer.addListener(new Listener(this));
                hyperServer.start();
                availableConnections.add(connectionID);
            } catch (IOException e) {
                serverMode = false;
                hyperServer = null;
                loadClient("localhost", connectionPort);
            }
        } else {
            loadClient(ForeignIP == null ? "localhost" : ForeignIP, connectionPort);
        }

    }

    public HyperKryo getKryo(){
        if(serverMode){
            return (HyperKryo)hyperServer.getKryo();
        }else{
            return (HyperKryo)hyperSphereClient.getKryo();
        }
    }

    public void waitKey() {
        if (!sleeping && !serverMode) {
            if (!hyperSphereClient.isConnected()) {
                try {
                    thread = Thread.currentThread();
                    sleeping = true;
                    Thread.sleep(5000000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public void awake() {
        if (sleeping) {
            thread.interrupt();
            sleeping = false;
        }
    }

    private boolean notForeignIP(String IP) {
        return IP == null || IP.equals("localhost");
    }

    public int getServerConnectionID() {
        return availableConnections.get(0);
    }

    private void loadClient(String IP, int port) throws IOException {
        hyperSphereClient = new HyperClient(BUFFER_SIZE, BUFFER_SIZE);
        hyperSphereClient.addListener(new Listener(this));
        new Thread(hyperSphereClient).start();
        hyperSphereClient.start();
        hyperSphereClient.connect(5000, IP, port);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}
    }

    protected void register(Kryo kryo) {
        kryo.register(JVMConnectionPacket.class);
        kryo.register(JVMExistance.class);
        kryo.register(Refusal.class);
        kryo.register(JVMDisconnection.class);
        kryo.register(JVMConnections.class);
        kryo.register(JVMVariableRequest.class);
        kryo.register(ArrayList.class);
    }

    public void disconnect() {
        if (serverMode) {
            hyperServer.close();
        } else {
            hyperSphereClient.close();
        }
    }

    public static boolean isTcpPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            // setReuseAddress(false) is required only on OSX,
            // otherwise the code will not work correctly on that platform
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public void sendPacket(JVMConnectionPacket jvmConnectionPacket) {
        if (serverMode) {
            if (availableConnections.contains(jvmConnectionPacket.JVM_ID)) {
                connectionHashMap.get(jvmConnectionPacket.JVM_ID).sendTCP(jvmConnectionPacket);
            } else {
                throw new RuntimeException("Connection Not Valid");
            }
        } else {
            hyperSphereClient.sendTCP(jvmConnectionPacket);
        }
    }


    public void sendToAll(JVMConnectionPacket jvmConnectionPacket) {
        if (serverMode) {
            hyperServer.sendToAllTCP(jvmConnectionPacket);
        } else {
            hyperSphereClient.sendTCP(jvmConnectionPacket);
        }
    }

    protected void sendToAll(Connection c, JVMConnectionPacket jvmConnectionPacket) {
        if (serverMode)
            hyperServer.sendToAllExceptTCP(c.getID(), jvmConnectionPacket);
    }


    protected void sendServerPacket(Connection c, JVMConnectionPacket jvmConnectionPacket) {
        if (jvmConnectionPacket.dispersal == JVMConnectionPacket.DISPERSAL_ONE) {
            if (connectionHashMap.containsKey(jvmConnectionPacket.JVM_ID)) {
                connectionHashMap.get(jvmConnectionPacket.JVM_ID).sendTCP(jvmConnectionPacket);
            } else {
                throwRefusal(c, "ID(" + jvmConnectionPacket.JVM_ID + ") DOES NOT EXIST!");
            }
        } else {
            sendToAll(c, jvmConnectionPacket);
        }
    }

    public void throwRefusal(Connection connection, String refusal) {
        if (serverMode) {

            connection.sendTCP(new Refusal(refusal));
        } else {
            throw new RuntimeException("Cannot Throw Refusal If Not Server!");
        }
    }

    public <T> SharedVariable<T> newVariable(Consumer<T> onSetListener, String variableName) {
        return new SharedVariable<>(onSetListener, this, variableName);
    }

    public <T> SharedVariable<T> newVariable(T t, Consumer<T> onSetListener, String variableName) {
        SharedVariable<T> sharedVariable = new SharedVariable<>(onSetListener, this, variableName);
        sharedVariable.set(t);
        return sharedVariable;
    }

    public int getConnectionID(Connection connection) {
        if (serverMode) {
            for (Map.Entry<Integer, Connection> i : connectionHashMap.entrySet()) {
                if (i.getValue() == connection) {
                    return i.getKey();
                }
            }
        }
        return -1;
    }
}

class Listener extends HyperListener {
    public JVMConnection jvmConnection;

    public Listener(JVMConnection jvmConnection) {
        super(jvmConnection.serverMode ? jvmConnection.hyperServer.getKryo() : jvmConnection.hyperSphereClient.getKryo());
        this.jvmConnection = jvmConnection;
        jvmConnection.register(jvmConnection.serverMode ? jvmConnection.hyperServer.getKryo() : jvmConnection.hyperSphereClient.getKryo());
    }

    @Override
    public void Disconnected(Connection c) {
        if (jvmConnection.serverMode) {
            int JVM_ID = jvmConnection.getConnectionID(c);
            jvmConnection.availableConnections.remove((Integer) JVM_ID);
            jvmConnection.connectionHashMap.remove(JVM_ID);
            jvmConnection.hyperServer.sendToAllTCP(new JVMDisconnection(JVM_ID));
        } else {
            jvmConnection.hyperSphereClient.reconnectLoop(3000);
        }
    }

    @Override
    public void Connected(Connection c) {
        if (!jvmConnection.serverMode) {
            jvmConnection.awake();
            jvmConnection.hyperSphereClient.sendTCP(new JVMExistance(jvmConnection.connectionID));
        }
    }

    @Override
    public void Received(Connection c, Object o) {
        if (o instanceof JVMExistance) {
            if (jvmConnection.serverMode) {
                JVMExistance jvmExistance = (JVMExistance) o;
                if (!jvmConnection.connectionHashMap.containsKey(jvmExistance.JVMID)) {
                    jvmConnection.connectionHashMap.put(jvmExistance.JVMID, c);

                    if (!jvmConnection.availableConnections.contains(jvmExistance.JVMID))
                        jvmConnection.availableConnections.add(jvmExistance.JVMID);

                    jvmConnection.hyperServer.sendToAllTCP(o);
                    c.sendTCP(new JVMConnections(jvmConnection.availableConnections));
                    System.out.println(jvmConnection.availableConnections.size() + " JVM's Online! (S)");
                } else {
                    jvmConnection.throwRefusal(c, "ID(" + jvmExistance.JVMID + ") Already Exists, Please Choose Another!");
                }
            } else {
                if (!jvmConnection.availableConnections.contains(((JVMExistance) o).JVMID))
                    jvmConnection.availableConnections.add(((JVMExistance) o).JVMID);

                System.out.println(jvmConnection.availableConnections.size() + " JVM's Online! (C)");
            }
        } else if (o instanceof JVMConnectionPacket) {

            JVMConnectionPacket jvmConnectionPacket = (JVMConnectionPacket) o;
            if (jvmConnection.serverMode) {
                jvmConnectionPacket.FROM_JVM_ID = jvmConnection.getConnectionID(c);
            }

            if (jvmConnection.serverMode && jvmConnection.connectionID != jvmConnectionPacket.JVM_ID) {
                jvmConnection.sendServerPacket(c, jvmConnectionPacket);
            } else {
                if (!(jvmConnectionPacket.o instanceof JVMVariableRequest))
                    jvmConnection.jvmListener.received(jvmConnectionPacket.FROM_JVM_ID, jvmConnectionPacket.o);
                else {
                    ((JVMVariableRequest) jvmConnectionPacket.o).dealWith(c, jvmConnection);
                }
            }
        } else if (o instanceof JVMDisconnection) {
            jvmConnection.availableConnections.remove((Integer) ((JVMDisconnection) o).JVM_ID);
        } else if (o instanceof Refusal) {
            ((Refusal) o).throwRefusal();
        } else if (o instanceof JVMConnections) {
            jvmConnection.availableConnections = ((JVMConnections) o).connections;
        }
    }

}

class JVMExistance implements Serializable{

    public int JVMID = -1;

    public JVMExistance() {

    }

    public JVMExistance(int ID) {
        this.JVMID = ID;
    }


}

class Refusal implements Serializable{

    public String refusal;


    public Refusal() {

    }

    public Refusal(String refusal) {
        this.refusal = refusal;
    }

    public void throwRefusal() {
        throw new RuntimeException(refusal);
    }

}

class JVMDisconnection {

    public int JVM_ID = -1;

    public JVMDisconnection() {

    }

    public JVMDisconnection(int JVM_ID) {
        this.JVM_ID = JVM_ID;
    }

}

class JVMConnections implements Serializable{

    public ArrayList<Integer> connections;

    public JVMConnections() {

    }

    public JVMConnections(ArrayList<Integer> connections) {
        this.connections = connections;
    }

}
