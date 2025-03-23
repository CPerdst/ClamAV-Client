package com.l1Akr.utils;

import lombok.*;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ScanDirectory {

    private final String directory;

    private final int deep;

    private final ClamAVClient clamAVClient;

    ScanDirectory(Builder builder) {
        this.directory = builder.directory;
        this.deep = builder.deep;
        this.clamAVClient = new ClamAVClient.Builder()
                .host(builder.host)
                .port(builder.port)
                .connectionTimeout(builder.connectionTimeout)
                .scanTimeout(builder.scanTimeout)
                .build();
    }

    /**
     * 同步扫描方法
     * @return
     */
    public List<ScanResult> scan() throws ScanDirectoryException {
        List<ScanResult> results = new ArrayList<>();

        File directory = new File(this.directory);

        if(directory.exists() && directory.isDirectory()) {
            // 初始化哈希工具
            HashUtil hu = new HashUtil();

            File[] files = directory.listFiles();
            if(files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        // 同步扫描，目前ClamAV只支持同步
                        ClamAVClient.ScanResult scan = clamAVClient.scan(file);
                        try {
                            ScanResult myScan = new ScanResult(scan, file.getName(), hu.getHashOfFile(file, "SHA-256"));
                            results.add(myScan);
                        } catch (ClamAVClient.ClamAVException e) {
                            throw handleException(e);
                        } catch (NoSuchAlgorithmException e) {
                            throw new ScanDirectoryException("algorithm is not support", e);
                        } catch (IOException e) {
                            throw new ScanDirectoryException("IO operation failed", e);
                        }
                    }
                }
            }
        }
        return results;
    }

    private ScanDirectoryException handleException(ClamAVClient.ClamAVException e) {
        if(e instanceof ClamAVClient.ClamAVConnectionException) {
            return new ScanDirectoryException("ClamAV connection timeout", e);
        }else if (e instanceof ClamAVClient.ClamAVTimeoutException) {
            return new ScanDirectoryException("ClamAV timeout", e);
        }else {
            return new ScanDirectoryException(e.getMessage(), e);
        }
    }

    public static class Builder {
        private String directory = "";

        private int deep = 3;

        private String host = "127.0.0.1";

        private int port = 3310;

        private Duration connectionTimeout = Duration.ofSeconds(10);

        private Duration scanTimeout = Duration.ofMinutes(1);

        public Builder directory(String path) {
            this.directory = path;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder deep(int deep) {
            this.deep = deep;
            return this;
        }

        public Builder connectionTimeout(Duration timeout) {
            this.connectionTimeout = timeout;
            return this;
        }

        public Builder scanTimeout(Duration timeout) {
            this.scanTimeout = timeout;
            return this;
        }

        public ScanDirectory build() {
            return new ScanDirectory(this);
        }
    }

    @Getter
    @Setter
    public class ScanResult extends ClamAVClient.ScanResult {

        private String fileName;

        private String fileHash;

        ScanResult(boolean infected, String virusName, String rawResponse, Instant timestamp) {
            super(infected, virusName, rawResponse, timestamp);
        }

        ScanResult(boolean infected, String virusName, String rawResponse, Instant timestamp, String fileName, String fileHash) {
            super(infected, virusName, rawResponse, timestamp);
            this.fileHash = fileHash;
            this.fileName = fileName;
        }

        ScanResult(ClamAVClient.ScanResult scan, String fileName, String fileHash) {
            super(
                    scan.isInfected(),
                    scan.getVirusName(),
                    scan.getRawResponse(),
                    scan.getTimestamp()
            );
            this.fileHash = fileHash;
            this.fileName = fileName;
        }
    }

    static class ScanDirectoryException extends RuntimeException {
        public ScanDirectoryException(String message) {
            super(message);
        }
        public ScanDirectoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
