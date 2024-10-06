package com.zszdevelop.file.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.util.List;

@RestController
@RequestMapping("/download2")
public class DownloadController2 {

    private static final String FILE_DIRECTORY = "D:\\workdir\\file\\";

    @GetMapping("/file/{fileName}")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable("fileName") String fileName,
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {

        File file = new File(FILE_DIRECTORY + fileName);
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        long fileSize = file.length();

        // 如果沒有範圍請求，則傳輸整個文件
        if (rangeHeader == null) {
            try (FileInputStream fis = new FileInputStream(file);
                 FileChannel fileChannel = fis.getChannel()) {

                return ResponseEntity.ok()
                        .contentLength(fileSize)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(new InputStreamResource(fis));
            }
        }

        // 處理範圍請求
        List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(fileSize);
        long end = range.getRangeEnd(fileSize);

        long contentLength = end - start + 1;

        try (FileInputStream fis = new FileInputStream(file);
             FileChannel fileChannel = fis.getChannel()) {

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
