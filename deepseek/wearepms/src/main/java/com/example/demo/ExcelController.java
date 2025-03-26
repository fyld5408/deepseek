package com.example.demo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.apache.commons.compress.utils.Lists;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * created by xionghui on 2025/2/13
 **/
@RestController
public class ExcelController {

//    @GetMapping("/")
//    public String home() {
//        return "welcome.html"; // 或者直接返回视图名称，取决于你的视图解析器配置
//    }
//
//    @GetMapping("/error")
//    public String handleError() {
//        // 返回错误页面的视图名称
//        return "error.html";
//    }

    @PostMapping("/upload")
    public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            // 处理上传的Excel文件
            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);

            // 这里简单地给第一列数据加上"Processed: "前缀
            for (Row row : sheet) {
                Cell cell = row.getCell(0);
                Cell outputCell1 = row.createCell(1);
                Cell outputCell2 = row.createCell(2);

                if (cell != null) {
                    String value = cell.getStringCellValue();
                    List<String> deepseekValues = fetchDeepseekData(value);
                    outputCell1.setCellValue(deepseekValues.get(0));
//                    outputCell2.setCellValue(deepseekValues.get(1));
                }
            }

            // 将修改后的Workbook写入ByteArrayOutputStream
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.close();

            // 准备响应数据
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            String fileName = "processed_" + file.getOriginalFilename();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(in));

        } catch (IOException e) {
            return new ResponseEntity<>("Unable to process file", HttpStatus.BAD_REQUEST);
        }
    }

    private List<String> fetchDeepseekData(String content) {
        List<String> results = Lists.newArrayList();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();

        MediaType mediaType = MediaType.parse("application/json");
        String idea = "上面输入的是一段想法，请扩写成一段新闻，要求100字";
        String idea2 = "将上面输入的翻译成英文";
        String input = "{\n  \"messages\": [\n    {\n      \"content\": \"" + content + "\",\n      \"role\": \"system\"\n    },\n    {\n      \"content\": \"" + idea + "\",\n      \"role\": \"user\"\n    }\n ],\n  \"model\": \"deepseek-chat\",\n  \"frequency_penalty\": 0,\n  \"max_tokens\": 2048,\n  \"presence_penalty\": 0,\n  \"response_format\": {\n    \"type\": \"text\"\n  },\n  \"stop\": null,\n  \"stream\": false,\n  \"stream_options\": null,\n  \"temperature\": 1,\n  \"top_p\": 1,\n  \"tools\": null,\n  \"tool_choice\": \"none\",\n  \"logprobs\": false,\n  \"top_logprobs\": null\n}";

        okhttp3.RequestBody  body = okhttp3.RequestBody.create(mediaType, input);
        Request request = new Request.Builder()
                .url("https://api.deepseek.com/chat/completions")
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer sk-bc17f93a49044bf4bfff1b819f4ca1bf")
                .build();
        String res = null;
        try {
            Response response = client.newCall(request).execute();
            res = response.body().string();
            JSONObject resJson = JSON.parseObject(res);
            JSONArray choices = resJson.getJSONArray("choices");
            JSONObject choice = (JSONObject) choices.get(0);
            res = choice.getJSONObject("message").getString("content");
            results.add(res);

//            String input2 = "{\n  \"messages\": [\n    {\n      \"content\": \"" + res + "\",\n      \"role\": \"system\"\n    },\n    {\n      \"content\": \"" + idea2 + "\",\n      \"role\": \"user\"\n    }\n ],\n  \"model\": \"deepseek-chat\",\n  \"frequency_penalty\": 0,\n  \"max_tokens\": 2048,\n  \"presence_penalty\": 0,\n  \"response_format\": {\n    \"type\": \"text\"\n  },\n  \"stop\": null,\n  \"stream\": false,\n  \"stream_options\": null,\n  \"temperature\": 1,\n  \"top_p\": 1,\n  \"tools\": null,\n  \"tool_choice\": \"none\",\n  \"logprobs\": false,\n  \"top_logprobs\": null\n}";
//            body = okhttp3.RequestBody.create(mediaType, input2);
//            request = new Request.Builder()
//                    .url("https://api.deepseek.com/chat/completions")
//                    .method("POST", body)
//                    .addHeader("Content-Type", "application/json")
//                    .addHeader("Accept", "application/json")
//                    .addHeader("Authorization", "Bearer sk-bc17f93a49044bf4bfff1b819f4ca1bf")
//                    .build();
//            response = client.newCall(request).execute();
//            res = response.body().string();
//            resJson = JSON.parseObject(res);
//            choices = resJson.getJSONArray("choices");
//            choice = (JSONObject) choices.get(0);
//            res = choice.getJSONObject("message").getString("content");
//            results.add(res);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(@RequestParam("fileName") String fileName) throws IOException {
        // 由于这是一个简化的例子，我们直接返回一个空的Excel文件。
        // 实际应用中，你应该根据fileName找到对应的文件并返回。
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Result");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("测试哈哈哈");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(in));
    }



}
