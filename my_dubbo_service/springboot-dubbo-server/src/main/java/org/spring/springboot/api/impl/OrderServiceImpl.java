package org.spring.springboot.api.impl;

//import com.alibaba.druid.sql.visitor.functions.Concat;
import com.alibaba.dubbo.config.annotation.Reference;
import com.google.gson.Gson;
import org.spring.springboot.config.MsgProducer;
import org.spring.springboot.constant.RedisKeysConstant;
import org.spring.springboot.dao.StockOrderMapper;
import org.spring.springboot.domain.Stock;
import org.spring.springboot.domain.StockOrder;
import org.spring.springboot.api.OrderService;
import org.spring.springboot.api.StockService;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.spring.springboot.api.impl.StockServiceImpl;
/**
 * Function:
 *
 * @author crossoverJie
 *         Date: 01/05/2018 14:10
 * @since JDK 1.8
 */
@Transactional(rollbackFor = Exception.class)
//@Service(value = "DBOrderService")
@com.alibaba.dubbo.config.annotation.Service(version = "1.0.0")
public class OrderServiceImpl implements OrderService {

    private Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
//
//
//    //    @Resource(name = "DBStockService")
    @Reference(version = "1.0.0",check = false)
    private StockService stockService;
//
    @Autowired
    private StockOrderMapper orderMapper;

    @Autowired
    private RedisTemplate<String,String> redisTemplate ;

    @Autowired
    private MsgProducer msgProducer ;

    @Value("${spring.kafka.template.default-topic}")
    private String kafkaTopic ;

    @Override
    public int createWrongOrder(int sid) throws Exception{
//        return 1;

        //校验库存
        Stock stock = checkStock(sid);

        //扣库存
        saleStock(stock);

        //创建订单
        int id = createOrder(stock);
        redisTemplate.delete(RedisKeysConstant.STOCK_COUNT + sid);
        redisTemplate.delete(RedisKeysConstant.STOCK_SALE + sid);
        redisTemplate.delete(RedisKeysConstant.STOCK_VERSION + sid);
        return id;
    }

    @Override
    public int createOptimisticOrder(int sid) throws Exception {

        //校验库存
        Stock stock = checkStock(sid);

        //乐观锁更新库存
        saleStockOptimistic(stock);

        //创建订单
        int id = createOrder(stock);

        return id;
    }

    @Override
    public int createOptimisticOrderUseRedis(int sid) throws Exception {
        //检验库存，从 Redis 获取
        Stock stock = checkStockByRedis(sid);

        //乐观锁更新库存 以及更新 Redis
        saleStockOptimisticByRedis(stock);

        //创建订单
        int id = createOrder(stock);
        return id ;
    }

    @Override
    public void createOptimisticOrderUseRedisAndKafka(int sid) throws Exception {

        //检验库存，从 Redis 获取
        Stock stock = checkStockByRedis(sid);

        //利用 Kafka 创建订单
        msgProducer.sendMessage(kafkaTopic,new Gson().toJson(stock,Stock.class));
        logger.info("send Kafka success");

    }

    private Stock checkStockByRedis(int sid) throws Exception {
        if (null == redisTemplate.opsForValue().get(RedisKeysConstant.STOCK_COUNT + sid)){
            Stock stock = checkStock(sid);
            redisTemplate.opsForValue().set(RedisKeysConstant.STOCK_COUNT + sid,stock.getCount().toString());
            redisTemplate.opsForValue().set(RedisKeysConstant.STOCK_SALE + sid,stock.getSale().toString());
            redisTemplate.opsForValue().set(RedisKeysConstant.STOCK_VERSION + sid,stock.getVersion().toString());
        }

        Integer count = Integer.parseInt(redisTemplate.opsForValue().get(RedisKeysConstant.STOCK_COUNT + sid));
        Integer sale = Integer.parseInt(redisTemplate.opsForValue().get(RedisKeysConstant.STOCK_SALE + sid));
        if (count.equals(sale)){
            throw new RuntimeException("库存不足 Redis count=" + count.toString()+",sale="+sale.toString());
        }
        Integer version = Integer.parseInt(redisTemplate.opsForValue().get(RedisKeysConstant.STOCK_VERSION + sid));
        Stock stock = new Stock() ;
        stock.setId(sid);
        stock.setCount(count);
        stock.setSale(sale);
        stock.setVersion(version);

        return stock;
    }

    /**
     * 乐观锁更新数据库 还要更新 Redis
     * @param stock
     */
    private void saleStockOptimisticByRedis(Stock stock) {
        int count = stockService.updateStockByOptimistic(stock);
        if (count == 0){
            throw new RuntimeException("并发更新库存失败"+stock.getCount().toString()+","+stock.getSale().toString()) ;
        }
        //自增
        redisTemplate.opsForValue().increment(RedisKeysConstant.STOCK_SALE + stock.getId(),1) ;
        redisTemplate.opsForValue().increment(RedisKeysConstant.STOCK_VERSION + stock.getId(),1) ;
    }

    private Stock checkStock(int sid) {
        Stock stock = stockService.getStockById(sid);
        if (stock.getSale().equals(stock.getCount())) {
            throw new RuntimeException("库存不足");
        }
        return stock;
    }

    private void saleStockOptimistic(Stock stock) {
        int count = stockService.updateStockByOptimistic(stock);
        if (count == 0){
            throw new RuntimeException("并发更新库存失败") ;
        }
    }


    private int createOrder(Stock stock) {
        StockOrder order = new StockOrder();
        order.setSid(stock.getId());
        order.setName(stock.getName());
        int id = orderMapper.insertSelective(order);
        return id;
    }

    private int saleStock(Stock stock) {
        stock.setSale(stock.getSale() + 1);
        return stockService.updateStockById(stock);
    }
}
