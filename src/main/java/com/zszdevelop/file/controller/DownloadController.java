package com.zszdevelop.file.controller;

import com.zszdevelop.file.domian.FileResult;
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

import java.io.*;
import java.util.List;

@RestController
@RequestMapping("/download")
public class DownloadController {

    // 你可以設置文件根目錄，從這裡讀取所有文件
    private static final String FILE_DIRECTORY = "D:\\workdir\\file\\";

    @GetMapping("/faq")
    public FileResult<Boolean> faq(String fileName) {
        boolean exists = true;
        FileResult<Boolean> result = FileResult.success("成功", exists);
        return result;
    }

    @GetMapping("/file/{fileName}")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable("fileName") String fileName,
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {

        // 構建完整的文件路徑
        File file = new File(FILE_DIRECTORY + fileName);

        // 檢查文件是否存在
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        long fileSize = file.length();

        // 如果沒有提供 Range 標頭，返回整個文件
        if (rangeHeader == null) {
            return ResponseEntity.ok()
                    .contentLength(fileSize)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(new FileInputStream(file)));
        }

        // 解析 Range 標頭
        List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
        HttpRange range = ranges.get(0);  // 簡單處理只支持一個 range
        long start = range.getRangeStart(fileSize);
        long end = range.getRangeEnd(fileSize);

        // 設置輸出範圍
        long contentLength = end - start + 1;
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        randomAccessFile.seek(start);

        byte[] data = new byte[(int) contentLength];
        randomAccessFile.readFully(data);
        randomAccessFile.close();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .contentLength(contentLength)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(new ByteArrayInputStream(data)));
    }
}

