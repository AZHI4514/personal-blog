package com.azhi.controller;

import com.azhi.pojo.Result;
import com.azhi.service.ClapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clap")
@RequiredArgsConstructor
public class ClapController {

    private final ClapService clapService;

    @PostMapping
    public Result<Void> clap() {
        clapService.clap();
        return Result.success();   // 成功，无数据返回
    }
}
