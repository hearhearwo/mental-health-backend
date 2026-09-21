package com.example.mentalhealth.controller;

import com.example.mentalhealth.common.Result;
import com.example.mentalhealth.common.UserContext;
import com.example.mentalhealth.dto.StatusResponse;
import com.example.mentalhealth.service.StatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final StatusService statusService;

    public ProfileController(StatusService statusService) {
        this.statusService = statusService;
    }

    /** 心理状态：综合等级 + 5 维度 + 7/30 天趋势，全部从 emotion_log 聚合 */
    @GetMapping("/status")
    public Result<StatusResponse> status() {
        return Result.ok(statusService.getStatus(UserContext.require()));
    }
}
