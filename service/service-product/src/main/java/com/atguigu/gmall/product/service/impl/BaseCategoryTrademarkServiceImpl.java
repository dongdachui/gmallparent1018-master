package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.mapper.BaseCategoryTrademarkMapper;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * author:atGuiGu-mqx
 * date:2022/4/26 15:42
 * 描述：
 **/
@Service
public class BaseCategoryTrademarkServiceImpl extends ServiceImpl<BaseCategoryTrademarkMapper,BaseCategoryTrademark> implements BaseCategoryTrademarkService {

    @Autowired
    private BaseCategoryTrademarkMapper baseCategoryTrademarkMapper;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Override
    public void deleteById(Long category3Id, Long trademarkId) {
        //  删除数据的本质： update 语句. is_deleted = 1
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id",category3Id);
        baseCategoryTrademarkQueryWrapper.eq("trademark_id",trademarkId);
        baseCategoryTrademarkMapper.delete(baseCategoryTrademarkQueryWrapper);
    }

    @Override
    public void save(CategoryTrademarkVo categoryTrademarkVo) {
        //  实体类 跟vo的实体类有什么关系
        //  保存的本质：向这张表中插入数据 base_category_trademark
        //  category3Id 一个值 ， trademarkIdList 对应集合！
        //  {category3Id: 61 ,[{tmId:"2"},{tmId:"3"}]}
        //  61 2 baseCategoryTrademark   61 3 baseCategoryTrademark
        //  获取category3Id 对应的tmId 集合。
        List<Long> trademarkIdList = categoryTrademarkVo.getTrademarkIdList();
        if (!CollectionUtils.isEmpty(trademarkIdList)){
            //  循环遍历插入数据.
            //  做映射关系   tmId什么意思，哪来的 trademarkIdList 集合的泛型
            List<BaseCategoryTrademark> baseCategoryTrademarkList = trademarkIdList.stream().map(tmId -> {
                //  创建一个对象
                BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
                baseCategoryTrademark.setTrademarkId(tmId);
                baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
                //  返回对象
                return baseCategoryTrademark;
            }).collect(Collectors.toList());

            //  执行多条insert 语句.
            //            for (BaseCategoryTrademark baseCategoryTrademark : baseCategoryTrademarkList) {
            //                BaseCategoryTrademark categoryTrademark = new BaseCategoryTrademark();
            //                categoryTrademark.setTrademarkId(baseCategoryTrademark.getTrademarkId());
            //                categoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
            //
            //                baseCategoryTrademarkMapper.insert(categoryTrademark);
            //            }

            //  baseCategoryTrademarkList 这个集合就是要插入的数据对象
            //  批量插入数据： 必须要利用IService ,ServiceImpl 才能调用批量插入数据方法.
            this.saveBatch(baseCategoryTrademarkList);
        }

    }

    @Override
    public List<BaseTrademark> getCurrentTrademarkList(Long category3Id) {
        /*
        1.  根据三级分类Id 获取绑定的品牌数据.
        2.  根据tmId 过滤.
        3.

         */
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id",category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(baseCategoryTrademarkQueryWrapper);

        //  这个集合中包含了trademark_id 字段！
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)){
            //  循环遍历获取到对应的品牌Id
            List<Long> tmIdList = baseCategoryTrademarkList.stream().map(baseCategoryTrademark -> {
                return baseCategoryTrademark.getTrademarkId();
            }).collect(Collectors.toList());
            //  根据他们Id 集合要获取到品牌集合
            //  小米，苹果，华为
            //  List<BaseTrademark> baseTrademarkList1 = baseTrademarkMapper.selectBatchIds(tmIdList);
            //  TT，AA，小米，苹果，华为
            //  List<BaseTrademark> baseTrademarkList2 = baseTrademarkMapper.selectList(null);

            //   TT，AA，小米，苹果，华为 对应的tmId 与 tmIdList 进行过滤.
            //  stream: 一段可传递的代码片段， 不会改变原结果集。 最终要将计算的代码变为 集合或变为其他的，
            List<BaseTrademark> baseTrademarkList = baseTrademarkMapper.selectList(null).stream().filter(baseTrademark -> {
                return !tmIdList.contains(baseTrademark.getId());
            }).collect(Collectors.toList());
            //  返回数据;
            return baseTrademarkList;
        }
        //  三级分类Id 没有绑定任何品牌
        return baseTrademarkMapper.selectList(null);
    }

    @Override
    public List<BaseTrademark> getTrademarkList(Long category3Id) {
        /*
        参数 category3Id
            这张表 中 base_trademark 没有 category3Id 但是有 id ;
         通过category3Id 查找 这张表 base_category_trademark 对应的 trademark_id
         然后： trademark_id = base_trademark.id;
         */

        //  通过category3Id 查找 这张表 base_category_trademark 对应的 trademark_id
        //  select * from base_category_trademark where category3_id = ? and is_deleted = 0;
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id",category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(baseCategoryTrademarkQueryWrapper);

        //  trademark_id = base_trademark.id; 查询 base_trademark 集合!
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)){
            //  将这个集合 baseCategoryTrademarkList 对应的 trademark_id 品牌Id获取到！
            //            ArrayList<Long> tradeMarkIdList = new ArrayList<>();
            //            for (BaseCategoryTrademark baseCategoryTrademark : baseCategoryTrademarkList) {
            //                Long trademarkId = baseCategoryTrademark.getTrademarkId();
            //                tradeMarkIdList.add(trademarkId);
            //            }
            //  使用拉姆达表达式做映射处理.  推荐 使用这种！ 等价的效果 ---> 效率高。
            List<Long> tradeMarkIdList = baseCategoryTrademarkList.stream().map(baseCategoryTrademark -> {
                return baseCategoryTrademark.getTrademarkId();
            }).collect(Collectors.toList());

            //   根据品牌Id 集合数据来获取 base_trademark 集合
            List<BaseTrademark> baseTrademarkList = baseTrademarkMapper.selectBatchIds(tradeMarkIdList);
            //  返回品牌数据
            return baseTrademarkList;
        }
        //  默认返回空
        return null;
    }
}