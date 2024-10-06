package com.zszdevelop.file.controller;
import com.zszdevelop.file.utils.ThrottledInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
@Slf4j
@RestController
@RequestMapping("/download3")
public class DownloadController3 {

    private static final String FILE_DIRECTORY = "D:\\workdir\\file\\";
    private static final long MAX_RATE = 1024 * 1024 * 20; // 每秒最大傳輸速率，20 MB/s
//    private static final long CHUNK_SIZE = 1024 * 8;  // 每次傳輸的塊大小，8 KB

    @RequestMapping(value = "/file/{fileName}", method = RequestMethod.HEAD)
    public ResponseEntity<String> getFileSize(@PathVariable("fileName") String fileName) {
       // log.info("GetFileSize from {}", fileName);
        File file = new File(FILE_DIRECTORY + fileName);
        if (file.exists() && file.isFile()) {
            long fileSize = file.length();
            log.info("GetFileSize from {} size: {}", fileName,fileSize);
            //return ResponseEntity.ok().header("content-length", String.valueOf(fileSize)).build();
            return ResponseEntity.ok()
                    .contentLength(fileSize).build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/file/{fileName}")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable("fileName") String fileName,
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException, InterruptedException {
        log.info("rangeHeader is {}", rangeHeader);
        File file = new File(FILE_DIRECTORY + fileName);
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        long fileSize = file.length();

        if (rangeHeader == null) {
            log.info("rangeHeader is null : file={}, contentLength={}", fileName, fileSize);
            return throttleFileTransfer(file, 0, fileSize,fileSize);
        }

        // 處理範圍請求
        List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(fileSize);
        long end = range.getRangeEnd(fileSize);
        long contentLength = end - start + 1;

        return throttleFileTransfer(file, start,end, contentLength);
    }

    private ResponseEntity<InputStreamResource> throttleFileTransfer(File file, long start, long endPosition,long contentLength) throws IOException, InterruptedException {
        FileInputStream fis = new FileInputStream(file);
        FileChannel fileChannel = fis.getChannel();
        fileChannel.position(start);  // 設置文件通道的開始位置
        log.info("throttleFileTransfer: file={}, start={}, end={},contentLength={}", file, start,endPosition, contentLength);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentLength(contentLength)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(new ThrottledInputStream(fis, MAX_RATE,endPosition)));
    }
}
