# ClamAV 客户端工具

## 简介

ClamAV 客户端工具是一个高可用的 Java 库，用于与 ClamAV 服务器进行文件扫描操作。该工具支持同步和异步扫描，并提供了详细的扫描结果和文件哈希值。你可以使用该工具来扫描文件和目录，确保文件的安全性。

## 特点

- **多线程支持**：支持异步扫描，提高扫描效率。
- **灵活配置**：可以通过构建器模式轻松配置连接和扫描超时时间。
- **详细扫描结果**：提供扫描结果、病毒名称、原始响应和时间戳。
- **文件哈希值**：支持计算文件的哈希值，确保文件的唯一性和完整性。

## 安装

### 1. 依赖管理

确保你的项目中包含以下依赖项。如果你使用 Maven，可以在 `pom.xml` 中添加以下依赖：

```xml
<dependencies>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.24</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```
### 2. 下载代码
克隆项目仓库到本地：
```bash
git clone https://github.com/your-username/ClamAVClient.git
```

## 使用方法
### 1. 构建 ClamAV 客户端
```java
ClamAVClient client = new ClamAVClient.Builder()
    .host("127.0.0.1")
    .port(3310)
    .connectionTimeout(Duration.ofSeconds(5))
    .scanTimeout(Duration.ofSeconds(30))
    .build();
```
### 2. 同步扫描文件
```java
File file = new File("path/to/your/file.txt");
ClamAVClient.ScanResult result = client.scan(file);
System.out.println("Scan Result: " + result);
```
### 3. 异步扫描文件
```java
File file = new File("path/to/your/file.txt");
CompletableFuture<ClamAVClient.ScanResult> future = client.scanAsync(file);
future.thenAccept(result -> System.out.println("Scan Result: " + result));
```
### 4. 扫描目录
```java
ScanDirectory directoryScanner = new ScanDirectory.Builder()
    .directory("path/to/your/directory")
    .host("127.0.0.1")
    .port(3310)
    .build();

List<ScanDirectory.ScanResult> results = directoryScanner.scan();
for (ScanDirectory.ScanResult result : results) {
    System.out.println("File: " + result.getFileName());
    System.out.println("Infected: " + result.isInfected());
    System.out.println("Virus Name: " + result.getVirusName());
    System.out.println("File Hash: " + result.getFileHash());
    System.out.println("Timestamp: " + result.getTimestamp());
}
```
### 5.异步扫描文件
```java
import com.l1Akr.utils.ClamAVClient;
import com.l1Akr.utils.ClamAVClient.ScanResult;

import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) {
        ClamAVClient client = new ClamAVClient.Builder()
            .host("127.0.0.1")
            .port(3310)
            .build();

        File file = new File("path/to/your/file.txt");
        CompletableFuture<ScanResult> future = client.scanAsync(file);
        future.thenAccept(result -> System.out.println("Scan Result: " + result));
    }
}
```
