package com.zszdevelop.file.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@RestController
@RequestMapping("/download2")
public class DownloadController2 {

    private static final String FILE_DIRECTORY = "D:\\workdir\\file\\";


    @RequestMapping(value = "/file/{fileName}", method = RequestMethod.HEAD)
    public ResponseEntity<String> getFileSize(@PathVariable("fileName") String fileName) {
        //log.info("GetFileSize from {}", fileName);
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
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {
        log.info("rangeHeader is {}", rangeHeader);
        File file = new File(FILE_DIRECTORY + fileName);
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        long fileSize = file.length();

        // 如果沒有範圍請求，則傳輸整個文件
        if (rangeHeader == null) {
            log.info("rangeHeader is null : file={}, contentLength={}", fileName, fileSize);
            try (FileInputStream fis = new FileInputStream(file);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                byte[] fileContent = bos.toByteArray();
                return ResponseEntity.ok()
                        .contentLength(fileSize)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(new InputStreamResource(new ByteArrayInputStream(fileContent)));
            }
        }

        // 處理範圍請求
        List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(fileSize);
        long end = range.getRangeEnd(fileSize);

        long contentLength = end - start + 1;

        try (FileInputStream fis = new FileInputStream(file))
        {
            FileChannel fileChannel = fis.getChannel();

            // 只讀取範圍內的內容
            ByteBuffer buffer = ByteBuffer.allocate((int) contentLength);
            fileChannel.position(start);
            fileChannel.read(buffer);
            buffer.flip();  // 重設緩衝區準備讀取

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .contentLength(contentLength)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(new ByteArrayInputStream(buffer.array())));
        }
    }
}
