package com.hmdp.mq;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderListener {

    private final IVoucherOrderService voucherOrderService;

    @RabbitListener(queues = "order.queue")
    public void listenOrderQueue(VoucherOrder voucherOrder) {
        voucherOrderService.createVoucherOrder(voucherOrder);
    }
}
