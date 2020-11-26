package com.hyperspherestudio.kryonet.Packets;

import com.hyperspherestudio.kryonet.FileCrypto;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.NoSuchAlgorithmException;
//Author Johnathan Bizzano
public class EncryptedPacket implements Serializable{

    public byte[] data;

    public EncryptedPacket(){

    }

    public void encrypt(byte[] key, Object o) throws NoSuchAlgorithmException, NoSuchPaddingException, IOException {
        data = new FileCrypto(key).encrypt(serialize(o));
    }


    public Object decrypt(byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, IOException, ClassNotFoundException {
        return deserialize(new FileCrypto(key).decrypt(data));
    }


    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }
    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }
}
