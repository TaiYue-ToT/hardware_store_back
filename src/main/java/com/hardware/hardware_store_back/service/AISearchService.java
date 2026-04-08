package com.hardware.hardware_store_back.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardware.hardware_store_back.entity.HardwareItem;
import com.hardware.hardware_store_back.repository.HardwareItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AISearchService {

    @Autowired
    private HardwareItemRepository repository;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    public Map<String, Object> search(String userQuery, int limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 从数据库获取所有商品的名称，并去重
            List<HardwareItem> items = repository.findAll();
            List<String> names = items.stream()
                    .map(HardwareItem::getName)
                    .distinct()
                    .limit(30)
                    .toList();

            // 2. 极简版提示词：只返回商品名，坚决不要废话
            String systemMsg = "你是一个五金店搜索助手。请根据用户需求，从列表中挑选最多 " + limit + " 个最匹配的商品（找不到就推荐最通用的替代品）。\n" +
                    "必须严格且仅返回 JSON 数组，不准输出任何废话！\n" +
                    "格式：[{\"name\": \"商品名\"}]";
            String userMsg = "User description: \"" + userQuery + "\"\nProduct list:\n" + String.join("\n", names);

            // 3. 准备发送 HTTP 请求 (带有超时设置)
            org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(10000); // 10秒连接超时
            // 👇 【修改1】把耐心拉到绝对极限的 120 秒，坚决不让它半路挂断！
            requestFactory.setReadTimeout(120000);
            RestTemplate restTemplate = new RestTemplate(requestFactory);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // 4. 打包请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "ep-20260407221843-lc6pd");
            requestBody.put("temperature", 0.0);

            // 👇 【修改2，超级关键！】给大模型戴上“紧箍咒”，绝对不允许它生成超过 50 个词！
            // 只要它写到 50 个词，管它写没写完，强制让它停下并把数据返回！
            requestBody.put("max_tokens", 50);

            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemMsg),
                    Map.of("role", "user", "content", userMsg)
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            System.out.println("=====================================");
            System.out.println("🤖 准备向火山引擎发送请求，模型: ep-20260407221843-lc6pd");
            System.out.println("=====================================");

            // 5. 发送请求给火山引擎并接收响应
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            System.out.println("✅ 成功收到火山引擎的回复！开始解析...");

            // 6. 解析大模型返回的 JSON 数据
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            String aiContent = root.path("choices").get(0).path("message").path("content").asText();

            // 👇 新增：把大模型的原话打印出来，看看它到底说了啥废话！
            System.out.println("====== 大模型原始回复开始 ======");
            System.out.println(aiContent);
            System.out.println("====== 大模型原始回复结束 ======");

            // 终极清洗大法：寻找第一个 '[' 和最后一个 ']'，把中间的 JSON 抠出来
            int startIndex = aiContent.indexOf('[');
            int endIndex = aiContent.lastIndexOf(']');

            if (startIndex != -1 && endIndex != -1 && endIndex >= startIndex) {
                // 成功抠出 JSON 数组
                aiContent = aiContent.substring(startIndex, endIndex + 1);
            } else {
                // 如果连方括号都没有，说明大模型彻底没按要求返回，强行抛出异常让我们看到
                throw new RuntimeException("大模型没有返回 JSON 数组格式！原话是: " + aiContent);
            }

            // 现在的 aiContent 绝对是纯净的 JSON 了，放心解析
            JsonNode parsedJson = mapper.readTree(aiContent);
            // ... 前面的代码保持不变 ...
            List<String> candidates = new ArrayList<>();
            if (parsedJson.isArray()) {
                for (JsonNode node : parsedJson) {
                    candidates.add(node.path("name").asText());
                }
            }



            // 👇👇👇 升级版：无视“-数字”后缀的超级模糊匹配 👇👇👇
            List<HardwareItem> finalItems = new ArrayList<>();
            for (String name : candidates) {
                // 1. 去掉 AI 名字里可能带的空格，并“一刀斩断”横杠后面的数字
                String cleanAiName = name.trim().split("-")[0].trim();

                HardwareItem matchedItem = items.stream()
                        .filter(item -> {
                            if (item.getName() == null) return false;
                            // 2. 同样，把数据库里的名字也“斩断”横杠
                            String dbBaseName = item.getName().trim().split("-")[0].trim();

                            // 3. 只要基础名字一样，或者互相包含，就算匹配成功！
                            return dbBaseName.equalsIgnoreCase(cleanAiName) ||
                                    dbBaseName.contains(cleanAiName) ||
                                    cleanAiName.contains(dbBaseName);
                        })
                        .findFirst()
                        .orElse(null);

                if (matchedItem != null) {
                    // 匹配成功！
                    finalItems.add(matchedItem);
                } else {
                    // 🚨 终极保底
                    HardwareItem dummy = new HardwareItem();
                    dummy.setName(name.trim());
                    dummy.setLocation("未知位置 (匹配失败)");
                    dummy.setPrice(java.math.BigDecimal.ZERO);
                    dummy.setStock(0);
                    finalItems.add(dummy);
                    System.out.println("⚠️ 警告：斩尾模糊匹配也失败了 -> " + name);
                }
            }

            // 7. 打包返回给前端
            result.put("ok", true);
            result.put("candidates", finalItems);
            result.put("detail", parsedJson);

            System.out.println("🎉 解析成功！结果已返回给前端。");
            return result;

        } catch (Exception e) {
            System.out.println("❌ 糟糕！AI 搜索发生错误: " + e.getMessage());
            e.printStackTrace();
            result.put("ok", false);
            result.put("error", "AI 搜索失败: " + e.getMessage());
            return result;
        }
    }
}