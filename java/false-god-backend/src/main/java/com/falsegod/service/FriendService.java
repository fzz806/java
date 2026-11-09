package com.falsegod.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.falsegod.entity.Friend;
import com.falsegod.entity.User;
import com.falsegod.mapper.FriendMapper;
import com.falsegod.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 好友业务：结契（加好友）/ 解契（删好友）/ 好友列表
 */
@Service
public class FriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;

    public FriendService(FriendMapper friendMapper, UserMapper userMapper) {
        this.friendMapper = friendMapper;
        this.userMapper = userMapper;
    }

    /**
     * 我的好友列表（附带好友当前真名，真名以 t_user 为准）
     */
    public List<Map<String, Object>> listFriends(Long userId) {
        List<Friend> rows = friendMapper.selectList(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, userId)
                        .orderByDesc(Friend::getCreatedAt)
        );
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> ids = rows.stream().map(Friend::getFriendId).toList();
        Map<Long, User> users = new LinkedHashMap<>();
        for (User u : userMapper.selectBatchIds(ids)) {
            users.put(u.getId(), u);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Friend f : rows) {
            User u = users.get(f.getFriendId());
            if (u == null) {
                continue; // 好友账号已被删除，静默跳过
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("friendId", f.getFriendId());
            item.put("username", u.getUsername());
            item.put("remark", f.getRemark());
            item.put("addedAt", f.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    /**
     * 结契：按真名加好友，双向各插一行
     */
    @Transactional
    public AddResult addFriend(Long userId, String username) {
        User target = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (target == null) {
            return AddResult.fail(1, "神堂名册里查无此人，无法结契");
        }
        if (target.getId().equals(userId)) {
            return AddResult.fail(2, "伪神不与自身结契");
        }
        if (friendMapper.selectCount(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, userId)
                        .eq(Friend::getFriendId, target.getId())
        ) > 0) {
            return AddResult.fail(3, "此名已在你的好友名录之中");
        }

        Friend mine = new Friend();
        mine.setUserId(userId);
        mine.setFriendId(target.getId());
        friendMapper.insert(mine);

        Friend theirs = new Friend();
        theirs.setUserId(target.getId());
        theirs.setFriendId(userId);
        friendMapper.insert(theirs);

        return AddResult.ok(target.getId(), target.getUsername());
    }

    /**
     * 解契：删除双向两行
     */
    @Transactional
    public boolean removeFriend(Long userId, Long friendId) {
        if (friendMapper.selectCount(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, userId)
                        .eq(Friend::getFriendId, friendId)
        ) == 0) {
            return false;
        }
        friendMapper.delete(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId));
        friendMapper.delete(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId));
        return true;
    }

    /** 是否互为好友（发传音前校验） */
    public boolean isFriend(Long userId, Long otherId) {
        return friendMapper.selectCount(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, userId)
                        .eq(Friend::getFriendId, otherId)
        ) > 0;
    }

    /** 结契结果 */
    public record AddResult(boolean success, int code, String message, Long friendId, String username) {
        static AddResult ok(Long friendId, String username) {
            return new AddResult(true, 0, "ok", friendId, username);
        }

        static AddResult fail(int code, String message) {
            return new AddResult(false, code, message, null, null);
        }
    }
}
