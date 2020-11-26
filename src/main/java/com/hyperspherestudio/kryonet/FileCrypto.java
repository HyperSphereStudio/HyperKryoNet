package com.hyperspherestudio.kryonet;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
//Author Johnathan Bizzano
public class FileCrypto {

    public SecretKey secretKey;
    public Cipher cipher;

    public FileCrypto(byte[] key) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.secretKey = new SecretKeySpec(key, "AES");
        this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    }


    public byte[] decrypt(byte[] input) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(new byte[16]));
            return cipher.doFinal(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public byte[] encrypt(byte[] input) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(new byte[16]));
            return cipher.doFinal(input);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}

