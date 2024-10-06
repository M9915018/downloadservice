package com.zszdevelop.file.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
@Slf4j
public class ThrottledInputStream extends FilterInputStream {

    private final long maxBytesPerSecond;

    private final long endPosition;
    private long bytesSent;


    private long BytesSentTotal;
    private long lastCheckTime;

    public ThrottledInputStream(InputStream in, long maxBytesPerSecond, long endPosition) {
        super(in);
        this.maxBytesPerSecond = maxBytesPerSecond;
        this.endPosition = endPosition;
        this.bytesSent = 0;
        this.lastCheckTime = System.nanoTime();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (BytesSentTotal >= endPosition) {
            return -1;  // 已讀取到指定位置，停止讀取
        }

        int bytesRead = super.read(b, off, len);

        if (bytesRead > 0) {
            bytesSent += bytesRead;
            throttle(bytesRead);
            BytesSentTotal += bytesRead;
        }
        return bytesRead;
    }

    private void throttle(int bytesRead) {
        long elapsedTime = (System.nanoTime() - lastCheckTime) / 1_000_000;  // 毫秒
        long expectedTime = (bytesSent * 1000) / maxBytesPerSecond;  // 預期的傳輸時間

        if (elapsedTime < expectedTime) {
            try {
                long sleepT = expectedTime - elapsedTime;
                Thread.sleep(sleepT);
                //log.info("ThrottledInputStream sleep {}: thread: {}, bytesSent :{}",  sleepT, Thread.currentThread().getName(),bytesSent);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (elapsedTime > 1000) {  // 重置計數器和時間戳
            log.info("ThrottledInputStream 重置計數器和時間戳 elapsedTime {}: thread: {}, bytesSent :{} , BytesSentTotal: {}, endPosition:{} ",  elapsedTime, Thread.currentThread().getName(),bytesSent,BytesSentTotal,endPosition);
            bytesSent = 0;
            lastCheckTime = System.nanoTime();
        }
    }
}

