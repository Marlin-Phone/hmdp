package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private ShopServiceImpl shopService;
    @Autowired
    private ShopServiceImpl shopServiceImpl;

    @Test
    void testSaveShop() {
        shopServiceImpl.saveShop2Redis(1L, 10L);
    }

}
