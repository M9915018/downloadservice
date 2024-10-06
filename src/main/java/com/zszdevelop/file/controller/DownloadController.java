package com.zszdevelop.file.controller;

import com.zszdevelop.file.domian.FileResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.List;
@Slf4j
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
        // 構建完整的文件路徑
        File file = new File(FILE_DIRECTORY + fileName);

        // 檢查文件是否存在
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        long fileSize = file.length();


        // 如果沒有提供 Range 標頭，返回整個文件
        if (rangeHeader == null) {
            log.info("rangeHeader is null : file={}, contentLength={}", fileName, fileSize);
            try (InputStream fileInputStream = new FileInputStream(file)) {
                return ResponseEntity.ok()
                        .contentLength(fileSize)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(new InputStreamResource(fileInputStream));
            } catch (IOException e) {
                log.error("Error while creating FileInputStream", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
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

