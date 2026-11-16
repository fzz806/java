package com.falsegod.controller;

import com.falsegod.common.Result;
import com.falsegod.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 首页聚合接口，路径与前端 src/api/content.js 的 fetchHome 保持一致：
 *   GET /api/home -> { notices: [{tag,title,time}], warehouse: [{name,count,rarity}] }
 *
 * <p>位于 /api/** 下，受 AuthInterceptor 保护。
 */
@RestController
@RequestMapping("/api")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/home")
    public Result<Map<String, Object>> home() {
        return Result.ok(homeService.overview());
    }
}
