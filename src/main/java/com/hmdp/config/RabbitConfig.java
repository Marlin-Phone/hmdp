package com.hmdp.config;

import org.springframework.amqp.core.Queue; // 必须是这个路径
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /**
     * 定义一个持久化的队列
     * 名称为 "order.queue"
     */
    @Bean
    public Queue orderQueue() {
        // 参数1：队列名称
        // 参数2：是否持久化（true表示重启RabbitMQ后队列依然存在）
        return new Queue("order.queue", true);
    }

    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}