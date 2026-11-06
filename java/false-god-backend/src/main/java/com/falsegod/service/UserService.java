package com.falsegod.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.falsegod.common.PasswordUtil;
import com.falsegod.entity.User;
import com.falsegod.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 用户业务：注册 / 登录
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 按真名查询用户 */
    public User findByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
    }

    /** 按主键查询用户 */
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 注册
     *
     * @return 成功返回真名；失败返回错误信息
     */
    public RegisterResult register(String username, String rawPassword) {
        if (findByUsername(username) != null) {
            return RegisterResult.fail(2, "此真名已被神堂登记，请另取伪名");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.hash(rawPassword));
        userMapper.insert(user);
        return RegisterResult.ok(username);
    }

    /**
     * 登录。
     *
     * <p>登录成功时若发现密码是历史 MD5 格式（或 BCrypt 强度偏低），
     * 会顺手改写为 BCrypt，实现平滑迁移。
     */
    public LoginResult login(String username, String rawPassword) {
        User user = findByUsername(username);
        if (user == null) {
            return LoginResult.fail(1, "此真名未在神堂登记，请先觉醒伪神天赋");
        }
        if (!PasswordUtil.matches(rawPassword, user.getPassword())) {
            return LoginResult.fail(1, "神印有误，无法通过认证");
        }
        upgradePasswordIfNeeded(user, rawPassword);
        return LoginResult.ok(user.getId(), user.getUsername());
    }

    /** 静默升级旧哈希，失败不影响登录 */
    private void upgradePasswordIfNeeded(User user, String rawPassword) {
        if (!PasswordUtil.needsUpgrade(user.getPassword())) {
            return;
        }
        try {
            User patch = new User();
            patch.setId(user.getId());
            patch.setPassword(PasswordUtil.hash(rawPassword));
            userMapper.updateById(patch);
            log.info("已将用户 {} 的密码哈希升级为 BCrypt", user.getUsername());
        } catch (Exception e) {
            log.warn("密码哈希升级失败（不影响本次登录）：{}", e.getMessage());
        }
    }

    /** 登录结果 */
    public record LoginResult(boolean success, int code, String message, Long userId, String username) {
        static LoginResult ok(Long userId, String username) {
            return new LoginResult(true, 0, "ok", userId, username);
        }

        static LoginResult fail(int code, String message) {
            return new LoginResult(false, code, message, null, null);
        }
    }

    /** 注册结果 */
    public record RegisterResult(boolean success, int code, String message, String username) {
        static RegisterResult ok(String username) {
            return new RegisterResult(true, 0, "ok", username);
        }

        static RegisterResult fail(int code, String message) {
            return new RegisterResult(false, code, message, null);
        }
    }
}
