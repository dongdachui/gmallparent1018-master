package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import springfox.documentation.spring.web.json.Json;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * author:atGuiGu-mqx
 * date:2022/4/23 10:44
 * 描述：
 **/
@Service
public class ManageServiceImpl implements ManageService {
    //  服务层调用mapper 层
    //  @Resource
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuPosterMapper spuPosterMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private RabbitService rabbitService;

//回显平台属性
    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {
        //  select * from base_attr_value where attr_id = attrId;
        return baseAttrValueMapper.selectList( new QueryWrapper<BaseAttrValue>().eq("attr_id",attrId));
    }

    @Override
    public IPage<SpuInfo> getSpuInfoList(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo) {

        //  构建查询条件：
        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.eq("category3_id",spuInfo.getCategory3Id());
        //  查询之后的数据按照id 进行降序排列！
        spuInfoQueryWrapper.orderByDesc("id");
        //  调用mapper 分页方法.
        return spuInfoMapper.selectPage(spuInfoPage,spuInfoQueryWrapper);
    }

    @Override
    public void onSale(Long skuId) {
        //  is_sale = 1 上架 update sku_info set is_sale = 1 where id = skuId
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoMapper.updateById(skuInfo);

        //  需要发送消息  发送的内容：根据消费者来决定的。
        this.rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuId);
    }


    @Override
    public BaseTrademark getTrademarkByTmId(Long tmId) {
        //  select * from base_trademark where id = tmId;
        BaseTrademark baseTrademark = baseTrademarkMapper.selectById(tmId);
        return baseTrademark;
    }

    @Override
    @GmallCache(prefix = "baseCategoryList:")
    public List<JSONObject> getBaseCategoryList() {
        //  先声明一个集合对象
        List<JSONObject> list = new ArrayList<>();
        //  先获取所有的分类数据集  select * from base_category_view;
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        //  如何获取到一级分类的name?  按照一级分类Id 进行分组 ，每一组对应的分类的名称 都一样。
        //  分组：Collectors.groupingBy()
        //  Long = category1Id  value = List<BaseCategoryView>
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));

        //  声明一个index 变量
        int index = 1;
        //  循环遍历这个map集合  迭代器遍历
        Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator = category1Map.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<Long, List<BaseCategoryView>> entry = iterator.next();
            Long category1Id = entry.getKey();
            List<BaseCategoryView> baseCategoryViewList1 = entry.getValue();

            //  声明一个一级分类对象 JSONObject
            JSONObject category1 = new JSONObject();
            category1.put("index",index);
            category1.put("categoryId",category1Id);
            category1.put("categoryName",baseCategoryViewList1.get(0).getCategory1Name());


            //  index变量迭代
            index++;

            //  声明一个集合来存储二级分类数据  存储当前一级分类Id 下所对应的所有二级分类数据。
            List<JSONObject> categoryChild2 = new ArrayList<>();
            //  获取到一级分类下对应的二级分类数据。
            //  key = category2Id  value = List<BaseCategoryView>;
            Map<Long, List<BaseCategoryView>> category2Map = baseCategoryViewList1.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator1 = category2Map.entrySet().iterator();
            while (iterator1.hasNext()){
                Map.Entry<Long, List<BaseCategoryView>> entry1 = iterator1.next();
                //  获取key  value
                Long category2Id = entry1.getKey();
                List<BaseCategoryView> baseCategoryViewList2 = entry1.getValue();
                //  声明一个二级分类对象 JSONObject
                JSONObject category2 = new JSONObject();
                category2.put("categoryId",category2Id);
                category2.put("categoryName",baseCategoryViewList2.get(0).getCategory2Name());
                categoryChild2.add(category2);

                //  声明一个集合来存储三级分类数据  存储当前二级分类Id 下所对应的所有三级分类数据。
                List<JSONObject> categoryChild3 = new ArrayList<>();
                //  获取三级分类数据：
                baseCategoryViewList2.forEach(baseCategoryView -> {
                    //  声明一个三级分类对象 JSONObject
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId",baseCategoryView.getCategory3Id());
                    category3.put("categoryName",baseCategoryView.getCategory3Name());
                    categoryChild3.add(category3);
                });

                //  categoryChild 存储三级分类集合。  必须等到三级分类集合有数据才能填入数据、
                    category2.put("categoryChild",categoryChild3);
            }

            //  对应的二级分类集合。 必须等到二级分类集合有数据才能填入数据、
            category1.put("categoryChild",categoryChild2);
            //  将所有的一级分类对象添加到集合中
            list.add(category1);
        }

        //  增强for 循环！
        //        for (Map.Entry<Long, List<BaseCategoryView>> entry : category1Map.entrySet()) {
        //            Long category1Id = entry.getKey();
        //            List<BaseCategoryView> baseCategoryViewList1 = entry.getValue();
        //        }
        return list;
    }

    @Override
    @GmallCache(prefix = "AttrList:")
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        //  调用mapper
        return baseAttrInfoMapper.selectAttrList(skuId);
    }

    @Override
    @GmallCache(prefix = "SpuPoster:")
    public List<SpuPoster> getSpuPosterBySpuId(Long spuId) {
        //  select * from spu_poster where spu_id = ? and is_deleted = 0;
        return spuPosterMapper.selectList(new QueryWrapper<SpuPoster>().eq("spu_id",spuId));
    }

    //  获取切换数据的Map 数据
    @GmallCache(prefix = "SkuValueIdsMap:")
    @Override
    public Map getSkuValueIdsMap(Long spuId) {
        //  声明一个map 集合
        HashMap<Object, Object> hashMap = new HashMap<>();
        //  向hashMap 中添加数据.
        //  获取数据来源：执行 sql 语句.
        List<Map> mapList = skuSaleAttrValueMapper.selectSkuValueIdsMap(spuId);
        //  判断
        if (!CollectionUtils.isEmpty(mapList)){
            mapList.forEach(map -> {
                hashMap.put(map.get("value_ids"),map.get("sku_id"));
            });
        }
        return hashMap;
    }

    @GmallCache(prefix = "SpuSaleAttrListCheckBySku:")
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        //  调用mapper
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    //  @GmallCache(prefix = "price:") 不需要走缓存！
    //  只查询的话：并发小还可以接受，如果并发高了，那么可能承受不了。
    //  第一种：lock ;   第二种：sentinel  qps  1 10
    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        String lockKey = skuId+":lock";
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            //  select price from sku_info where id = ?;
            QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
            skuInfoQueryWrapper.eq("id",skuId);
            skuInfoQueryWrapper.select("price");
            SkuInfo skuInfo = skuInfoMapper.selectOne(skuInfoQueryWrapper);
            if (skuInfo!=null){
                return skuInfo.getPrice();
            }
            return new BigDecimal(0);
        } finally {
            lock.unlock();
        }

        //  select id,spu_id,price,sku_name ... from sku_info where id =?;
        //        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        //        if (skuInfo!=null){
        //            return skuInfo.getPrice();
        //        }
        //        return new BigDecimal(0);
    }

    @Override
    @GmallCache(prefix = "CategoryView:")
    public BaseCategoryView getCategoryView(Long category3Id) {
        //  select * from base_category_view where id = 61;
        return baseCategoryViewMapper.selectById(category3Id);
    }

    @Override
    @GmallCache(prefix = "skuInfo:")
    public SkuInfo getSkuInfo(Long skuId) {
        /*
        if(true){
            return redisDate(skuId);
        }else{
            SkuInfo skuInfo = getSkuInfoDB(skuId);
            setRedisData(skuInfo);
        }
         */
        return getSkuInfoDB(skuId);

        // return getSkuInfoByRedisson(skuId);
        //  return getSkuInfoByRedis(skuId);
    }

    private SkuInfo getSkuInfoByRedisson(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            //  业务逻辑
            //  String key = "sku:skuId:info";
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;

            //  获取缓存中的数据， value = SkuInfo 这个实体类型
            skuInfo = (SkuInfo) this.redisTemplate.opsForValue().get(skuKey);
            //  判断skuInfo
            if (skuInfo==null){
                //  走数据库，放入缓存.
                //  lockKey = sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                //  获取可重入锁对象
                RLock lock = this.redissonClient.getLock(lockKey);
                //  第一种用法：
                //  lock.lock();
                //  第二种用法：
                //  lock.lock(RedisConst.SKULOCK_EXPIRE_PX1,TimeUnit.SECONDS);
                //  第三种用法
                //尝试获取锁。
                boolean result = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (result){
                    try {
                        //  判断result = true 获取到了锁. 查询数据库，并将数据放入缓存.
                        skuInfo = this.getSkuInfoDB(skuId);
                        if (skuInfo==null){
                            //  防止缓存穿透; 放入一个空值.
                            SkuInfo skuInfo1 = new SkuInfo();
                            this.redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        this.redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                        return skuInfo;
                    } finally {
                        //  解锁
                        lock.unlock();
                    }
                }else {
                    //  没有获取到锁
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }
            }else {
                //  缓存中有数据
                return skuInfo;
            }

        }catch (Exception e){
            //  记录日志。发送短信通知运维人员维护.....
            e.printStackTrace();
        }
        //  数据库兜底
        //表示从数据库中获取数据！ctrL+aLt+m提取方法快捷键
        // 如果redis宕机了，使用数据库做兜底工作！
        //宕机以后查询数据库。但是存在数据库压力问题。
        return getSkuInfoDB(skuId);
    }

    //  通过redis 实现分布式锁案例！
    private SkuInfo getSkuInfoByRedis(Long skuId) {
        SkuInfo skuInfo = null;
        try {

            //  存储skuInfo 使用哪种数据类型
            //  hash:  this.redisTemplate.opsForHash(); 存储对象 ；便于修改
            //  string: this.redisTemplate.opsForValue().get("key");
            //  商品详情特性：只是展示，没有修改因此 此处可以使用 string!
            //  给商品详情的skuInfo 起一个key 名。
            //  String key = "sku:skuId:info";
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;

            //  获取缓存中的数据， value = SkuInfo 这个实体类型
            skuInfo = (SkuInfo) this.redisTemplate.opsForValue().get(skuKey);
            if (skuInfo==null){
                //  说明缓存中没有数据。
                //  声明一个分布式锁的key
                //  lockKey = sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                //  声明一个uuid
                String uuid = UUID.randomUUID().toString();
                //  set key value ex 10 nx;
                Boolean result = this.redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                //  判断result
                if (result){
                    //  表示获取到了分布式锁。 查询数据库
                    skuInfo = this.getSkuInfoDB(skuId);
                    //  判断skuInfo  这个数据，在数据库中不存在。
                    if (skuInfo==null){
                        //  防止缓存穿透; 放入一个空值.
                        SkuInfo skuInfo1 = new SkuInfo();
                        this.redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                        return skuInfo1;
                    }
                    //  将数据库中的数据放入缓存.
                    this.redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                    //  删除缓存的key 【lua】 脚本

                    //  使用lua 脚本删除！
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

                    //  执行lua 脚本
                    DefaultRedisScript redisScript = new DefaultRedisScript();
                    redisScript.setScriptText(script);
                    redisScript.setResultType(Long.class);
                    //  第二个参数 key
                    //  第三个参数 value
                    this.redisTemplate.execute(redisScript, Arrays.asList(lockKey),uuid);
                    //  返回缓存中的数据.
                    return skuInfo;

                }else {
                    //  没有获取到锁
                    try {
                        Thread.sleep(200);
                        return getSkuInfo(skuId);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }else {
                //  直接获取缓存中的数据.
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //  如果缓存宕机了，此时数据库兜底.
        return getSkuInfoDB(skuId);
    }

    /**
     * 从数据库中skuInfo + skuImage 集合数据。
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoDB(Long skuId) {
        //  select * from sku_info where id = skuId
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo!=null){
            //  获取skuImage 集合数据
            //  select * from sku_image where sku_id = skuId;
            List<SkuImage> skuImageList = skuImageMapper.selectList(new QueryWrapper<SkuImage>().eq("sku_id", skuId));
            skuInfo.setSkuImageList(skuImageList);
        }

        //  返回数据.
        return skuInfo;
    }

    @Override
    public void cancelSale(Long skuId) {
        //  is_sale = 0 下架  update sku_info set is_sale = 0 where id = skuId
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoMapper.updateById(skuInfo);

        //  发送消息.
        this.rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_LOWER,skuId);

    }

    @Override
    public IPage<SkuInfo> getSkuInfoList(Page<SkuInfo> skuInfoPage, SkuInfo skuInfo) {
        //  创建查询条件
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.eq("category3_id",skuInfo.getCategory3Id());
        skuInfoQueryWrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(skuInfoPage,skuInfoQueryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // aop 动态代理！
    public void saveSkuInfo(SkuInfo skuInfo) {
        /*
        sku_info
        sku_image
        sku_attr_value
        sku_sale_attr_value
         */
        skuInfoMapper.insert(skuInfo);

        //  sku_image
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)){
            skuImageList.forEach(skuImage -> {
                //  注意skuId
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);

            });
        }
        //  sku_attr_value  skuId 与 平台属性值的关系
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)){
            skuAttrValueList.forEach(skuAttrValue -> {
                //  注意skuId
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            });
        }

        //  sku_sale_attr_value skuId 与 销售属性值的关系
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)){
            skuSaleAttrValueList.forEach(skuSaleAttrValue -> {
                //  skuId  spuId
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            });
        }

        //  将skuId 添加到布隆过滤器中！
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        bloomFilter.add(skuInfo.getId());
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        //  调用mapper
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        //  select * from spu_image where spu_id = spuId and is_deleted = 0;
        return spuImageMapper.selectList(new QueryWrapper<SpuImage>().eq("spu_id",spuId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSpuInfo(SpuInfo spuInfo) {
        /*
        spu_info
        spu_image
        spu_poster
        spu_sale_attr
        spu_sale_attr_value
         */
        spuInfoMapper.insert(spuInfo);

        //  spu_image
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (!CollectionUtils.isEmpty(spuImageList)){
            spuImageList.forEach(spuImage -> {
                //  设置spuId
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            });
        }

        //  spu_poster
        List<SpuPoster> spuPosterList = spuInfo.getSpuPosterList();
        if (!CollectionUtils.isEmpty(spuPosterList)){
            spuPosterList.forEach(spuPoster -> {
                spuPoster.setSpuId(spuInfo.getId());
                spuPosterMapper.insert(spuPoster);
            });
        }

        //  spu_sale_attr
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (!CollectionUtils.isEmpty(spuSaleAttrList)){
            spuSaleAttrList.forEach(spuSaleAttr -> {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);

                //  spu_sale_attr_value
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)){
                    spuSaleAttrValueList.forEach(spuSaleAttrValue -> {
                        //  赋值没有传递的数据.
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    });
                }
            });
        }


    }

    @Override
    public List<BaseSaleAttr> getSaleAttrList() {
        //  select * from base_sale_attr where is_deleted = 0;
        return baseSaleAttrMapper.selectList(null);
    }

    @Override
    public BaseAttrInfo getBaseAttrInfo(Long attrId) {
        //  获取到平台属性对象
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        //  判断
        if (baseAttrInfo==null){
            return baseAttrInfo;
        }
        //  根据平台属性Id 来获取到平台属性值集合数据.
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }


//    @Transactional(rollbackFor = Exception.class)  添加事物,括号内的意思是回滚,当发生异常时，回滚,要么两个都成功，要么两个都失败。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //  保存数据  id 是null  ，修改时 id 不为空！
        /*
            base_attr_info  平台属性表
            base_attr_value 平台属性值表
         */
        //  保存 ,修改 base_attr_info
        //如果前端传过来有ID，说明要进行修改操作。如果没有ID则为添加
        if (baseAttrInfo.getId()==null){
            //  插入完成之后， @TableId(type = IdType.AUTO) 能够获取到注解自增的值。
            baseAttrInfoMapper.insert(baseAttrInfo);
        }else {
            //  修改：id 不为空！base_attr_info
            baseAttrInfoMapper.updateById(baseAttrInfo);
            //  删除平台属性值集合数据. base_attr_value
            //  删除的本质：update base_attr_value set is_deleted = 1 where attr_id = 1;  逻辑删除.
            QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
            baseAttrValueQueryWrapper.eq("attr_id",baseAttrInfo.getId());
            baseAttrValueMapper.delete(baseAttrValueQueryWrapper);
        }

        // 保存 ,修改 base_attr_value ,先删除，再新增！
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        //  循环遍历插入数据
        if (!CollectionUtils.isEmpty(attrValueList)){
            //  消费型函数接口：有参无返回！
            attrValueList.forEach((baseAttrValue) -> {
                //  attr_id = base_attr_info.id;
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                //  将数据插入到baseAttrValue
                baseAttrValueMapper.insert(baseAttrValue);
            });
        }
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        //  推荐使用xml 形式
        return baseAttrInfoMapper.selectAttrInfoList(category1Id,category2Id,category3Id);
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        //  select * from base_category3 where base_category3.category2_id = 1;
        return baseCategory3Mapper.selectList(new QueryWrapper<BaseCategory3>().eq("category2_id",category2Id));
    }

    //  编写实现
    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        //  select * from base_category2 where base_category2.category1_id = 1;
        return baseCategory2Mapper.selectList(new QueryWrapper<BaseCategory2>().eq("category1_id",category1Id));
    }

    @Override
    public List<BaseCategory1> getCategory1() {
        // select * from base_category1;
        List<BaseCategory1> baseCategory1List = baseCategory1Mapper.selectList(null);

        return baseCategory1List;
    }
}