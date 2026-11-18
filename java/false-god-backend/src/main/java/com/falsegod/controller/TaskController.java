package com.falsegod.controller;

import com.falsegod.common.Result;
import com.falsegod.config.AuthInterceptor;
import com.falsegod.service.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 神格任务与阶位经验接口，路径与前端 src/api/task.js 保持一致：
 * <pre>
 *   GET  /api/task         -> { level, title, exp, expMax, totalExp, percent, levelMax, full,
 *                               nextTitle, realm, realmIndex,
 *                               claimable, dailyTotal, dailyClaimed,
 *                               mainTotal, mainClaimed,
 *                               tasks: [{id,code,title,desc,category,categoryLabel,action,
 *                                        progress,target,exp,status,percent}],
 *                               logs: [{time,text,exp}] }
 *   POST /api/task/claim   { id } -> { gain, levelUp, realmUp, oldLevel, newLevel, newTitle,
 *                                      panel:{...} }
 * </pre>
 *
 * <p>{@code level/title} 是神格阶位，{@code realm} 是完整修行境界（如「四印 · 圆满」），
 * 两者走同一条阶梯 {@code RealmLadder}，前端可以直接并排展示而不会打架。
 *
 * <p>位于 /api/** 下，受 AuthInterceptor 保护；当前用户取自拦截器写入的 request attribute。
 * 任务进度不接受外部直接写入，只能由真实操作（战力提升、登临）推进。
 */
@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/task")
    public Result<Map<String, Object>> panel(
            @RequestAttribute(name = AuthInterceptor.ATTR_USER_ID, required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "神印凭证无效，请重新登临");
        }
        return Result.ok(taskService.panel(userId));
    }

    @PostMapping("/task/claim")
    public Result<Map<String, Object>> claim(
            @RequestAttribute(name = AuthInterceptor.ATTR_USER_ID, required = false) Long userId,
            @RequestBody(required = false) Map<String, Object> body) {
        if (userId == null) {
            return Result.error(401, "神印凭证无效，请重新登临");
        }
        Long taskId = null;
        Object raw = body == null ? null : body.get("id");
        if (raw instanceof Number n) {
            taskId = n.longValue();
        } else if (raw instanceof String s && !s.isBlank()) {
            try {
                taskId = Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                // 交给下面统一报错
            }
        }
        TaskService.ClaimOutcome outcome = taskService.claim(userId, taskId);
        if (!outcome.isOk()) {
            return Result.error(400, outcome.getMessage());
        }
        return Result.ok(outcome.getData());
    }
}
