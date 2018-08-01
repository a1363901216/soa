package org.spring.springboot.api.impl;

import org.spring.springboot.dao.StockMapper;
import org.spring.springboot.domain.Stock;
import org.spring.springboot.api.StockService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Function:
 *
 * @author crossoverJie
 *         Date: 30/04/2018 22:39
 * @since JDK 1.8
 */
//@Service("DBStockService")
@com.alibaba.dubbo.config.annotation.Service(version = "1.0.0")
public class StockServiceImpl implements StockService {

    @Autowired
    private StockMapper stockMapper;

    @Override
    public int getStockCount(int id) {
        Stock ssmStock = stockMapper.selectByPrimaryKey(id);
        return ssmStock.getCount();
    }

    @Override
    public Stock getStockById(int id) {
        return stockMapper.selectByPrimaryKey(id) ;
    }

    @Override
    public int updateStockById(Stock stock) {
        return stockMapper.updateByPrimaryKeySelective(stock) ;
    }

    @Override
    public int updateStockByOptimistic(Stock stock) {
        return stockMapper.updateByOptimistic(stock);
    }
}
