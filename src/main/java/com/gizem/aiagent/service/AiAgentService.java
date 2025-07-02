package com.gizem.aiagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AiAgentService {

    private final RestTemplate restTemplate;
    @Value("${together.api.key}")
    private String API_KEY;

    @Value("${job.search.url}")
    private String jobSearchUrl;

    private static final String TOGETHER_API_URL = "https://api.together.xyz/v1/chat/completions";

    public AiAgentService() {
        this.restTemplate = new RestTemplate();
    }

    public String askAi(String userMessage) {
        try {
            // 1. Together'a gönderilecek prompt
            String prompt = """
            Extract the job title and city from the user message.
            Return only JSON in this format:
            {
              "city": "city name",
              "title": "job title keyword, for example backend, frontend, mobile"
            }
            User: %s
        """.formatted(userMessage);

            // 2. Together API'ye istek
            Map<String, Object> body = new HashMap<>();
            body.put("model", "mistralai/Mixtral-8x7B-Instruct-v0.1");

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", prompt));
            body.put("messages", messages);
            body.put("temperature", 0.7);

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(API_KEY);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> request = new HttpEntity<>(json, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    TOGETHER_API_URL, HttpMethod.POST, request, Map.class
            );

            Map<String, Object> choices = (Map<String, Object>) ((List<?>) response.getBody().get("choices")).get(0);
            Map<String, Object> message = (Map<String, Object>) choices.get("message");
            String content = (String) message.get("content");

            // 3. Together cevabını işle
            Map<String, String> jobParams = objectMapper.readValue(content.trim(), Map.class);
            String city = jobParams.getOrDefault("city", "");
            String title = jobParams.getOrDefault("title", "");

            // 4. Job Search Servisi'ne istek
            String searchUrl = jobSearchUrl + "/api/v1/jobs/search?title=" + title + "&city=" + city;
            ResponseEntity<String> jobResults = restTemplate.getForEntity(searchUrl, String.class);

            Map<String, Object> resultMap = objectMapper.readValue(jobResults.getBody(), Map.class);
            List<Map<String, Object>> jobs = (List<Map<String, Object>>) resultMap.get("content");

            if (jobs.isEmpty()) {
                return "\nÜzgünüm 😔 Şu anda " + city + " şehrinde '" + title + "' pozisyonunda bir iş bulamadım. Yeni fırsatlar eklenince tekrar deneyebilirsin!";
            }

            StringBuilder sb = new StringBuilder("\nHarika haber!  " + city + " şehrinde '" + title + "' pozisyonu için bazı işler buldum:\n\n");

            for (Map<String, Object> job : jobs) {
                String company = (job.get("company") != null) ? job.get("company").toString() : "Belirtilmemiş";
                String jobTitle = job.get("title").toString();
                String desc = job.get("description").toString();
                String loc = job.get("city") + ", " + job.get("country");
                String updated = job.get("lastUpdated").toString().split("T")[0];
                int apps = (int) job.getOrDefault("applicationCount", 0);

                sb.append("🔸 '").append(jobTitle).append("' pozisyonu ").append(company)
                        .append(" şirketinde açık.\n")
                        .append(" \nKonum: ").append(loc).append("\n")
                        .append(" \nAçıklama: ").append(desc).append("\n")
                        .append("\n Son güncelleme: ").append(updated).append("\n")
                        .append("\n Şu ana kadar ").append(apps).append(" kişi başvurmuş.\n\n");
            }

            sb.append("\n 😊 Bol şans!  İş başvurularında yanında olmak için buradayım 😊");

            return sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Oops! Bir şeyler ters gitti 😵 AI şu anda cevap veremiyor, lütfen daha sonra tekrar dene.";
        }
    }


}