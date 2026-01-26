package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        // 实现缓存穿透
//        Shop shop = queryWithPassThrough(id);

        // 实现缓存击穿
        Shop shop = queryWithMutex(id);
        if (shop == null) {
            return Result.fail("商铺不存在！");
        }

        // 3. 返回商铺信息
        return Result.ok(shop);
    }

    // 互斥锁解决缓存击穿
    public Shop queryWithMutex(Long id) {
        String key = CACHE_SHOP_KEY + id;
        // 1. 从Redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断商铺是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3. 存在，直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 判断命中的是否是空值
        if (shopJson != null) {
            return null;
        }
        // 4. 实现缓存重建
        // 4.1 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 4.2 判断是否获取成功
            if (!isLock) {
                // 4.3 失败，休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            shopJson = stringRedisTemplate.opsForValue().get(key);
            if(StrUtil.isNotBlank(shopJson)){
                return JSONUtil.toBean(shopJson,Shop.class);
            }

            // 4.4 成功，根据id查询数据库
            shop = getById(id);
            // 模拟重建延时
            Thread.sleep(200);
            // 5. 不存在，返回错误
            if (shop == null) {
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL + RandomUtil.randomInt(1, 4), TimeUnit.MINUTES);
                return null;
            }
            // 6. 存在，写入Redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL + RandomUtil.randomInt(0, 30), TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 7. 释放锁
            unlock(lockKey);
        }
        // 8. 返回
        return shop;
    }

    public Shop queryWithPassThrough(Long id) {
        String key = CACHE_SHOP_KEY + id;
        // 1. 从Redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断商铺是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3. 存在，直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 判断命中的是否是空值
        if (shopJson != null) {
            return null;
        }
        // 4. 不存在，根据id查询数据库
        Shop shop = getById(id);
        // 5. 不存在，返回错误
        if (shop == null) {
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL + RandomUtil.randomInt(1, 4), TimeUnit.MINUTES);
            return null;
        }
        // 6. 存在，写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL + RandomUtil.randomInt(0, 30), TimeUnit.MINUTES);
        // 7. 返回
        return shop;
    }

    /**
     * 获取锁
     * @param key
     * @return
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     * @param key
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result update(Shop shop) {
        Long shopId = shop.getId();
        if (shopId == null) {
            return Result.fail("店铺 id 不能为空");
        }
        // 1. 更新数据库
        boolean success = updateById(shop);
        if (!success) {
            return Result.fail("更新失败");
        }
        // 2. 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        // 3. 返回结果
        return Result.ok(shop);
    }
}