package com.hyperspherestudio.kryonet.MultiLanguageManager;
//Author Johnathan Bizzano
public interface MultiLanguageListener {

    void Received(Connection c, Object o);

    void Connected(Connection c);

    void Disconnected(Connection c);
}
