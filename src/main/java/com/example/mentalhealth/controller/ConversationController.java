package com.example.mentalhealth.controller;

import com.example.mentalhealth.common.Result;
import com.example.mentalhealth.common.UserContext;
import com.example.mentalhealth.dto.ConversationResponse;
import com.example.mentalhealth.dto.MessageResponse;
import com.example.mentalhealth.dto.SendMessageRequest;
import com.example.mentalhealth.dto.SendMessageResponse;
import com.example.mentalhealth.service.ConversationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public Result<List<ConversationResponse>> list() {
        return Result.ok(conversationService.list(UserContext.require()));
    }

    @PostMapping
    public Result<ConversationResponse> create() {
        return Result.ok(conversationService.create(UserContext.require()));
    }

    @GetMapping("/{id}/messages")
    public Result<List<MessageResponse>> messages(@PathVariable("id") Long id) {
        return Result.ok(conversationService.messages(UserContext.require(), id));
    }

    /** 发送消息，同步返回 AI 回复（前端此时处于 loading 状态） */
    @PostMapping("/{id}/messages")
    public Result<SendMessageResponse> send(@PathVariable("id") Long id,
                                            @RequestBody SendMessageRequest request) {
        return Result.ok(conversationService.send(UserContext.require(), id, request.getContent()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        conversationService.delete(UserContext.require(), id);
        return Result.ok();
    }
}
