package com.falsegod.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.falsegod.entity.Message;
import com.falsegod.entity.User;
import com.falsegod.mapper.MessageMapper;
import com.falsegod.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 传音（点对点聊天）业务：拉取消息 / 发送消息
 */
@Service
public class ChatService {

    private static final int MAX_CONTENT = 500;
    private static final int MAX_PULL = 200;

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final FriendService friendService;

    public ChatService(MessageMapper messageMapper, UserMapper userMapper, FriendService friendService) {
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.friendService = friendService;
    }

    /**
     * 拉取我与某人之间的传音记录。
     *
     * @param afterId 只取 id 大于该值的增量消息（首次传 0 取全量，最多 200 条）
     */
    public List<Map<String, Object>> pullMessages(Long me, Long other, long afterId) {
        LambdaQueryWrapper<Message> q = new LambdaQueryWrapper<Message>()
                .and(w -> w.eq(Message::getSenderId, me).eq(Message::getReceiverId, other))
                .or(w -> w.eq(Message::getSenderId, other).eq(Message::getReceiverId, me))
                .gt(afterId > 0, Message::getId, afterId)
                .orderByAsc(Message::getId)
                .last("LIMIT " + MAX_PULL);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Message m : messageMapper.selectList(q)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("mine", m.getSenderId().equals(me));
            item.put("content", m.getContent());
            item.put("time", m.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    /**
     * 发送传音（仅好友之间）
     */
    public SendResult send(Long me, Long to, String content) {
        if (content == null || content.isBlank()) {
            return SendResult.fail(1, "传音内容不可为空");
        }
        String text = content.trim();
        if (text.length() > MAX_CONTENT) {
            return SendResult.fail(2, "传音过长，上限五百字");
        }
        if (!friendService.isFriend(me, to)) {
            return SendResult.fail(3, "非结契好友，无法传音");
        }
        User receiver = userMapper.selectById(to);
        if (receiver == null) {
            return SendResult.fail(4, "接收者已不在神堂名册");
        }

        Message m = new Message();
        m.setSenderId(me);
        m.setReceiverId(to);
        m.setContent(text);
        messageMapper.insert(m);

        return SendResult.ok(m.getId(), text);
    }

    /** 发送结果 */
    public record SendResult(boolean success, int code, String message, Long id, String content) {
        static SendResult ok(Long id, String content) {
            return new SendResult(true, 0, "ok", id, content);
        }

        static SendResult fail(int code, String message) {
            return new SendResult(false, code, message, null, null);
        }
    }
}
