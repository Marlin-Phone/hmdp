package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1. 查询优惠卷信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2. 判断是否秒杀开始
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime beginTime = voucher.getBeginTime();
        LocalDateTime endTime = voucher.getEndTime();
        if(beginTime.isAfter(now)){
            return Result.fail("秒杀未开始");
        }
        // 3. 判断是否秒杀结束
        if(endTime.isBefore(now)){
            return Result.fail("秒杀已结束");
        }
        // 4. 判断库存是否充足
        if(voucher.getStock() <= 0){
            return Result.fail("库存不足");
        }
        // 5. 扣除库存
//        voucher.setStock(voucher.getStock() - 1);
//        voucher.setUpdateTime(LocalDateTime.now());
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).update();
        if(!success){
            return Result.fail("库存不足");
        }
        // 6. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 6.1 订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 6.2 用户id
        Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        // 6.3 优惠卷id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        // 7. 返回订单id
        return Result.ok(orderId);
    }
}
