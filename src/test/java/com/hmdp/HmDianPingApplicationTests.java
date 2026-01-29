package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private ShopServiceImpl shopServiceImpl;
    @Resource
    CacheClient cacheClient;

    /**
     * 缓存击穿中逻辑过期的预热（店铺id）
     */
    @Test
    void testSaveShop() {
        Object shop = shopServiceImpl.getById(1L);
        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 1, shop, 30L, TimeUnit.SECONDS);
    }

}
