package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        // 缓存 key（可提取到 RedisConstants）
        String key = "cache:shop:type";
        // 1. 从 Redis 缓存中查询商铺类型
        String typeJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 若存在，直接返回
        if (StrUtil.isNotBlank(typeJson)) {
            List<ShopType> typeList = JSONUtil.toList(typeJson, ShopType.class);
            log.info("Redis缓存命中：{}", typeList);
            return Result.ok(typeList);
        }
        // 3. 不存在，查询数据库（按 sort 升序）
        List<ShopType> typeList = query().orderByAsc("sort").list();
        // 4. 数据库不存在，返回错误
        if (typeList == null || typeList.isEmpty()) {
            return Result.fail("商铺类型不存在");
        }
        // 5. 数据库存在，写入 Redis 缓存并返回
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(typeList));
        return Result.ok(typeList);
    }
}