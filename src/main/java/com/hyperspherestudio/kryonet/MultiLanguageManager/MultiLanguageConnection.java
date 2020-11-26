package com.hyperspherestudio.kryonet.MultiLanguageManager;
import com.hyperspherestudio.kryonet.JVMManager.JVMConnection;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
//Author Johnathan Bizzano
public class MultiLanguageConnection {

    public ServerSocket server;
    public Socket client;
    public boolean serverMode, running = true;
    public String ip;
    public int id;
    public ArrayList<Socket> connections = new ArrayList<>();

    public MultiLanguageConnection(boolean makeServer, String foreignIP, int port, int id) {
        this.ip = foreignIP;
        if (JVMConnection.isTcpPortAvailable(port) && makeServer) {
            this.id = id;
        } else {
            runClient(port);
        }
    }

    public void runServer(int port) {
        try {
            server = new ServerSocket(port);
            serverMode = true;
            while (running) {
                connections.add(server.accept());

            }
        } catch (IOException e) {
            runClient(port);
            server = null;
        }
    }

    public void runClient(int port) {

    }
}
