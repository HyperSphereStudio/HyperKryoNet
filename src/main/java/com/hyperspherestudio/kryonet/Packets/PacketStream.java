package com.hyperspherestudio.kryonet.Packets;
import com.hyperspherestudio.kryonet.Logic;

import java.io.Serializable;
import java.util.concurrent.CopyOnWriteArrayList;
//Author Johnathan Bizzano
public class PacketStream implements Serializable {
    public byte[] objectData;
    public int maxSize;
    public String idNum;
    public static int MAXSIZE = 5000000;
    public static CopyOnWriteArrayList<String> idList = new CopyOnWriteArrayList<>();

    public PacketStream() {
        String str = Logic.getAlphaNumericString(6);
        while (idList.contains(str)) {
            str = Logic.getAlphaNumericString(6);
        }
        idNum = str;
        idList.add(str);
    }


    public void release(){
        PacketStream.idList.remove(idNum);
    }



}
