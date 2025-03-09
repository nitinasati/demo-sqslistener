package com.learning.demo_sqslistener.controller;

import com.amazonaws.services.sqs.model.SendMessageResult;
import com.learning.demo_sqslistener.service.SQSService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final SQSService sqsService;

    public MessageController(SQSService sqsService) {
        this.sqsService = sqsService;
    }

    @PostMapping
    public ResponseEntity<SendMessageResult> sendMessage(@RequestBody String message) {
        SendMessageResult result = sqsService.sendMessage(message);
        return ResponseEntity.ok(result);
    }
} 