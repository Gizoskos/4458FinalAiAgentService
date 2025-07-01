package com.gizem.aiagent.controller;

import com.gizem.aiagent.service.AiAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiAgentController {

    private final AiAgentService aiService;

    public AiAgentController(AiAgentService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/message")
    public ResponseEntity<String> ask(@RequestBody String userMessage) {
        String response = aiService.askAi(userMessage);
        return ResponseEntity.ok(response);
    }
}