package com.l1Akr.utils;

import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

/**
 * 高可用ClamAV客户端工具类
 */
public class ClamAVClient implements AutoCloseable {
    private final String host;
    private final int port;
    private final Duration connectionTimeout;
    private final Duration scanTimeout;
    private final ExecutorService asyncExecutor;

    private ClamAVClient(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.connectionTimeout = builder.connectionTimeout;
        this.scanTimeout = builder.scanTimeout;
        this.asyncExecutor = Executors.newFixedThreadPool(builder.threadPoolSize);
    }

    /**
     * 同步扫描文件
     */
    public ScanResult scan(File file) throws ClamAVException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return scan(fis);
        } catch (IOException e) {
            throw new ClamAVException("File read error", e);
        }
    }

    /**
     * 同步扫描输入流（自动关闭流）
     */
    public ScanResult scan(InputStream inputStream) throws ClamAVException {
        try (Socket socket = createSocket();
             OutputStream os = socket.getOutputStream();
             InputStream is = socket.getInputStream()) {

            sendInstreamCommand(os, inputStream);
            return parseResponse(readResponse(is));

        } catch (IOException e) {
            throw handleIOException(e);
        }
    }

    /**
     * 异步扫描文件
     */
    public CompletableFuture<ScanResult> scanAsync(File file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return scan(file);
            } catch (ClamAVException e) {
                throw new CompletionException(e);
            }
        }, asyncExecutor);
    }

    /**
     * 异步扫描文件
     */
    public CompletableFuture<ScanResult> scanAsync(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return scan(inputStream);
            } catch (ClamAVException e) {
                throw new CompletionException(e);
            }
        }, asyncExecutor);
    }

    private Socket createSocket() throws IOException {
        Socket socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(host, port),
                (int) connectionTimeout.toMillis());
        socket.setSoTimeout((int) scanTimeout.toMillis());
        return socket;
    }

    private void sendInstreamCommand(OutputStream os, InputStream is)
            throws IOException {

        os.write("zINSTREAM\0".getBytes());
        os.flush();

        byte[] buffer = new byte[2048];
        int read;
        while ((read = is.read(buffer)) != -1) {
            ByteBuffer chunkSize = ByteBuffer.allocate(4)
                    .order(ByteOrder.BIG_ENDIAN)
                    .putInt(read);
            os.write(chunkSize.array());
            os.write(buffer, 0, read);
        }
        os.write(new byte[]{0, 0, 0, 0});
        os.flush();
    }

    private String readResponse(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
            if (baos.toString().contains("\0")) break;
        }
        return baos.toString().trim().replace("\0", "");
    }

    private ScanResult parseResponse(String response) {
        Instant timestamp = Instant.now();

        if (response.endsWith("OK")) {
            return new ScanResult(false, null, response, timestamp);
        } else if (response.endsWith("FOUND")) {
            String virus = response.split(": ")[1].replace(" FOUND", "");
            return new ScanResult(true, virus, response, timestamp);
        } else if (response.endsWith("ERROR")) {
            throw new ClamAVException("Scan error: " + response);
        }
        throw new ClamAVException("Unknown response: " + response);
    }

    private ClamAVException handleIOException(IOException e) {
        if (e instanceof java.net.ConnectException) {
            return new ClamAVConnectionException("Connection failed to " + host + ":" + port, e);
        } else if (e instanceof java.net.SocketTimeoutException) {
            return new ClamAVTimeoutException("Scan timeout after " + scanTimeout, e);
        }
        return new ClamAVException("IO operation failed", e);
    }

    @Override
    public void close() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static class Builder {
        private String host = "localhost";
        private int port = 3310;
        private Duration connectionTimeout = Duration.ofSeconds(5);
        private Duration scanTimeout = Duration.ofSeconds(30);
        private int threadPoolSize = 4;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
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

        public Builder threadPoolSize(int size) {
            this.threadPoolSize = size;
            return this;
        }

        public ClamAVClient build() {
            return new ClamAVClient(this);
        }
    }

    /******************** 扫描结果封装 ********************/
    @Getter
    @Setter
    public static class ScanResult {
        private final boolean infected;
        private final String virusName;
        private final String rawResponse;
        private final Instant timestamp;

        ScanResult(boolean infected, String virusName,
                   String rawResponse, Instant timestamp) {
            this.infected = infected;
            this.virusName = virusName;
            this.rawResponse = rawResponse;
            this.timestamp = timestamp;
        }

        // Getter方法省略...
    }

    /******************** 自定义异常体系 ********************/
    public static class ClamAVException extends RuntimeException {
        public ClamAVException(String message) { super(message); }
        public ClamAVException(String message, Throwable cause) { super(message, cause); }
    }

    public static class ClamAVConnectionException extends ClamAVException {
        public ClamAVConnectionException(String message, Throwable cause) { super(message, cause); }
    }

    public static class ClamAVTimeoutException extends ClamAVException {
        public ClamAVTimeoutException(String message, Throwable cause) { super(message, cause); }
    }
}