package com.hyperspherestudio.kryonet.MultiLanguageManager;

import java.net.Socket;
//Author Johnathan Bizzano
public class Connection implements Runnable {

    public Socket socket;
    public MultiLanguageListener hyperListener;
    
    public Connection(MultiLanguageListener hyperListener, Socket socket){
        this.hyperListener = hyperListener;
        this.socket = socket;
        new Thread(this).start();
    }

    public void send(){
        
    }
    
    public Object get(){
        return null;
    }
    
    @Override
    public void run(){
    }
}