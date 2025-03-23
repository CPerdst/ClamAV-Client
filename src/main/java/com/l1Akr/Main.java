package com.l1Akr;

import com.l1Akr.utils.ClamAVClient;
import com.l1Akr.utils.ScanDirectory;

import java.io.*;
import java.util.List;

public class Main {

    public static void syncDirectoryScan() {
        // 同步目录扫描
        ScanDirectory scanDirectory = new ScanDirectory.Builder()
                .directory("/path/to/samplesDirectory")
                .host("127.0.0.1")
                .port(3310)
                .deep(3)
                .build();

        List<ScanDirectory.ScanResult> results = scanDirectory.scan();

        for(ScanDirectory.ScanResult result : results) {
            System.out.printf("filename: %s, filehash: %s\n扫描结果: [%s]\n",
                    result.getFileName(),
                    result.getFileHash(),
                    result.isInfected() ? result.getVirusName() : "OK");
        }
    }

    public static void singleFileScanByClamAVClient() throws IOException {
        File file = new File("/path/to/samplesFile");
        try (FileInputStream fis = new FileInputStream(file);) {
            ClamAVClient clamAVClient = new ClamAVClient.Builder()
                    .host("127.0.0.1")
                    .port(3310)
                    .build();

            ClamAVClient.ScanResult scan = clamAVClient.scan(fis);
            System.out.printf("扫描结果: [%s] [%s]\n",
                    scan.isInfected() ? "不安全" : "安全",
                    scan.isInfected() ? scan.getVirusName() : "No Virus Found");
        }
    }

    public static void main(String[] args) {
        // 同步单一文件扫描
        try {
            singleFileScanByClamAVClient();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 同步目录扫描
        syncDirectoryScan();



//            ClamAVClient.ScanResult result = client.scan(stream);
//
//            System.out.println(result);
//
//            System.out.println("扫描结果: " + (result.isInfected() ?
//                    "发现病毒 [" + result.getVirusName() + "]" : "安全"));

//        while(true) {
//            ClamAVClient client = new ClamAVClient.Builder()
//                    .host("127.0.0.1")
//                    .port(3310)
//                    .connectionTimeout(Duration.ofSeconds(10))
//                    .scanTimeout(Duration.ofMinutes(1))
//                    .build();
//            try{
//                // 同步扫描文件
//                Thread.sleep(1000);
//                // 初始化客户端
//
//                File sampleFile = new File("/home/maziyang/gnome-session.log");
//                String eicar = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
//                InputStream stream = new ByteArrayInputStream(eicar.getBytes());
//                // 异步扫描
//                client.scanAsync(stream)
//                        .thenAccept(asyncResult -> {
//                            System.out.println("异步扫描完成: " + asyncResult.getRawResponse());
//                        })
//                        .exceptionally(e -> {
//                            System.err.println("异步扫描失败: " + e.getMessage());
//                            return null;
//                        });
//            } catch (ClamAVClient.ClamAVException e) {
//                System.err.println("扫描失败: " + e.getMessage());
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            } finally {
//                client.close();
//            }
//
//        }

    }
}
