package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new org.springframework.core.io.ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024*1024);
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newFixedThreadPool(1);

    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {
            while(true){
                try {
                    // 1. 获取队列中的订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 2. 创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                }
            }
        }
    }

    /**
     * 子线程
     * @param voucherOrder
     */
    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        // 1. 获取用户id
        Long userId = voucherOrder.getUserId();
        // 2. 创建锁对象
        RLock lock = redissonClient.getLock("lock:order:" + userId);
//        RLock lock1 = redissonClient.getLock("lock:order:" + userId);
//        RLock lock2 = redissonClient2.getLock("lock:order" + userId);
//        RLock lock3 = redissonClient3.getLock("lock:order" + userId);
        // 创建联锁
//        RLock lock = redissonClient.getMultiLock(lock1, lock2, lock3);
        // 3. 获取锁（带看门狗机制）
        boolean isLock = lock.tryLock();
        // 4. 判断是否获取锁成功
        if(!isLock){
            // 获取锁失败，说明是重复下单，直接返回
            log.error("不允许重复下单");
            return;
        }
        try {
            // 获取代理对象（事务）
            proxy.creatVoucherOrder(voucherOrder);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    private IVoucherOrderService proxy;

    /**
     * 主线程
     * @param voucherId
     * @return
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 获取用户id
        Long userId = UserHolder.getUser().getId();
        // 1. 执行Lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString()
        );
        // 2. 判断结果是否为0
        int r = result.intValue();
        if(r != 0){
            // 2.1 不为0，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        // 2.2 为0，代表有购买资格，把下单信息保存到阻塞队列
        // 2.2.0. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 2.2.1 订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 2.2.2 用户id
        voucherOrder.setUserId(userId);
        // 2.2.3 优惠卷id
        voucherOrder.setVoucherId(voucherId);
        // 获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        // 2.2.4 放入阻塞队列
        orderTasks.add(voucherOrder);
        // 3. 返回订单id
        return Result.ok(orderId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void creatVoucherOrder(VoucherOrder voucherOrder) {
        // 一人一单
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();

        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if(count > 0){
            log.error("用户已经购买过一次");
            return;
        }
        // 5. 扣除库存
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0) // 乐观锁，stock > 0 时才能扣减成功
                .update();
        if(!success){
            log.error("库存不足");
            return;
        }
        save(voucherOrder);
    }
}
