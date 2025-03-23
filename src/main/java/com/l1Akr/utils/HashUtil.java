package com.l1Akr.utils;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


// 非线程安全
public class HashUtil {

    public String getHashOfFile(File file, String algorithm) throws NoSuchAlgorithmException, IOException {
        MessageDigest instance = MessageDigest.getInstance(algorithm);
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192];
            int read;
            while((read = bis.read(buffer)) != -1) {
                instance.update(buffer, 0, read);
            }
            byte[] digest = instance.digest();
            StringBuilder sb = new StringBuilder();
            for(byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) sb.append('0');
                sb.append(hex);
            }

            return sb.toString();
        }
    }

}
