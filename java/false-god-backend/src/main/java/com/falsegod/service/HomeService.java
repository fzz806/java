package com.falsegod.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.falsegod.entity.Notice;
import com.falsegod.entity.WarehouseItem;
import com.falsegod.mapper.NoticeMapper;
import com.falsegod.mapper.WarehouseItemMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页数据业务：公告 + 仓库。
 *
 * <p>返回结构与前端 HomePage.vue 约定一致：
 * <pre>{ notices: [{tag,title,time}], warehouse: [{name,count,rarity}] }</pre>
 *
 * <p>表为空时返回空数组，前端会自动回退到 data/homeData.js 的示例数据。
 */
@Service
public class HomeService {

    private final NoticeMapper noticeMapper;
    private final WarehouseItemMapper warehouseItemMapper;

    public HomeService(NoticeMapper noticeMapper, WarehouseItemMapper warehouseItemMapper) {
        this.noticeMapper = noticeMapper;
        this.warehouseItemMapper = warehouseItemMapper;
    }

    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("notices", listNotices());
        data.put("warehouse", listWarehouse());
        return data;
    }

    private List<Map<String, Object>> listNotices() {
        List<Notice> rows = noticeMapper.selectList(
                new LambdaQueryWrapper<Notice>()
                        .orderByAsc(Notice::getSortNo)
                        .orderByAsc(Notice::getId)
        );
        List<Map<String, Object>> list = new ArrayList<>(rows.size());
        for (Notice n : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tag", n.getTag());
            m.put("title", n.getTitle());
            m.put("time", n.getTimeText());
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> listWarehouse() {
        List<WarehouseItem> rows = warehouseItemMapper.selectList(
                new LambdaQueryWrapper<WarehouseItem>()
                        .orderByAsc(WarehouseItem::getSortNo)
                        .orderByAsc(WarehouseItem::getId)
        );
        List<Map<String, Object>> list = new ArrayList<>(rows.size());
        for (WarehouseItem w : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", w.getName());
            m.put("count", w.getCount());
            m.put("rarity", w.getRarity());
            list.add(m);
        }
        return list;
    }
}
