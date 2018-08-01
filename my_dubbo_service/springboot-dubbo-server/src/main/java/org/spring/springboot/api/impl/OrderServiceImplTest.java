package org.spring.springboot.api.impl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;
@RunWith(SpringRunner.class)
@SpringBootTest
public class OrderServiceImplTest {

    @Test
    public void createWrongOrder() {
        OrderServiceImpl orderServiceImpl = new OrderServiceImpl();
        try {
            Assert.assertEquals(orderServiceImpl.createWrongOrder(1),1);
        }
        catch (Exception e){
            System.out.print("in Exception");
        }

    }
}