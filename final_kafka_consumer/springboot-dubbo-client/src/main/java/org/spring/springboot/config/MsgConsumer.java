package org.spring.springboot.config;

import com.alibaba.dubbo.config.annotation.Reference;
import com.google.gson.Gson;
import org.spring.springboot.api.OrderService;
import org.spring.springboot.api.StockService;
import org.spring.springboot.domain.Stock;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 描述:消息消费者
 *
 * @author yanpenglei
 * @create 2017-10-16 18:33
 **/
@Component
public class MsgConsumer {

    @Reference(version = "1.0.0")
    private StockService stockService;

    @Reference(version = "1.0.0")
    private OrderService orderService;

    @KafkaListener(topics = {"ORDER-TOPIC1"})
    public void processMessage(String content) {

        System.out.println("消息被消费"+content);
        Stock stock = new Gson().fromJson(content,Stock.class);
        try {
            orderService.createOptimisticOrder(stock.getId());
        }
        catch (Exception e){
            System.out.println(e);
        }


    }

}