package com.hyperspherestudio.kryonet.MultiLanguageManager;
//Author Johnathan Bizzano
public class Message {

    public int code;
    public Object o;
    public static final int MESSAGE_REFUSAL = 0, MESSAGE_CONNECTIONS = 1;

    public Message() {

    }

    public Message(int code, Object o) {
        this(code);
        this.o = o;
    }

    public Message(int code) {
        this.code = code;
    }
}
