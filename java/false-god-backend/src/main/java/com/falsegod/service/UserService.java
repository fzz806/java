package com.falsegod.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.falsegod.common.PasswordUtil;
import com.falsegod.entity.User;
import com.falsegod.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 用户业务：注册 / 登录
 */
@Service
public class UserService {

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

    /**
     * 注册
     * @return 成功返回用户；失败返回错误信息（userInfo 仅含真名，不回传密码）
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
     * 登录
     */
    public LoginResult login(String username, String rawPassword) {
        User user = findByUsername(username);
        if (user == null) {
            return LoginResult.fail(1, "此真名未在神堂登记，请先觉醒伪神天赋");
        }
        if (!PasswordUtil.matches(rawPassword, user.getPassword())) {
            return LoginResult.fail(1, "神印有误，无法通过认证");
        }
        return LoginResult.ok(username);
    }

    /** 登录结果 */
    public record LoginResult(boolean success, int code, String message, String username) {
        static LoginResult ok(String username) {
            return new LoginResult(true, 0, "ok", username);
        }

        static LoginResult fail(int code, String message) {
            return new LoginResult(false, code, message, null);
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
