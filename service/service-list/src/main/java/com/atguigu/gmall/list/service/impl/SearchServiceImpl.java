package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * author:atGuiGu-mqx
 * date:2022/5/6 14:18
 * 描述：
 **/
@Service
public class  SearchServiceImpl implements SearchService {

    //  需要调用数据库么? 不需要！ 需要注入service-product!
    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    //  引入es的高级客户端
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public void onSale(Long skuId) {
        //  可以优化为多线程查询.
        //  需要将Goods 赋值，然后将其保存到es 中!
        Goods goods = new Goods();
        //  赋值操作
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);

        goods.setId(skuInfo.getId());
        goods.setTitle(skuInfo.getSkuName());
        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        //  价格应该给谁？
        //  goods.setPrice(skuInfo.getPrice().doubleValue()); // 不建议 skuInfo 有可能是在缓存中的，缓存中的价格有可能不是最新的价格.
        goods.setPrice(productFeignClient.getSkuPrice(skuId).doubleValue()); // 实时的价格
        goods.setCreateTime(new Date());

        //  获取品牌数据
        BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
        goods.setTmId(trademark.getId());
        goods.setTmName(trademark.getTmName());
        goods.setTmLogoUrl(trademark.getLogoUrl());

        //  获取分类数据
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        goods.setCategory3Id(categoryView.getCategory3Id());
        goods.setCategory2Id(categoryView.getCategory2Id());
        goods.setCategory1Id(categoryView.getCategory1Id());

        goods.setCategory3Name(categoryView.getCategory3Name());
        goods.setCategory2Name(categoryView.getCategory2Name());
        goods.setCategory1Name(categoryView.getCategory1Name());

        //  热度排名默认值。不用管： 后续hostScore 会随着访问量的增加自动实现累加.

        //  赋值平台属性，平台属性值
        //  返回来的是平台属性：平台属性值集合。
        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
        //  SearchAttr 这个对象中 attrId attrName attrValue
        //  为什么 是一个集合List<SearchAttr> attrs ?
        //  保存skuId = 24;  对应有几个平台属性 以及 对应的平台属性值！
        //  拉姆达表达式、
        List<SearchAttr> searchAttrList = attrList.stream().map(baseAttrInfo -> {
            //  声明对象并赋值
            SearchAttr searchAttr = new SearchAttr();
            searchAttr.setAttrId(baseAttrInfo.getId());
            searchAttr.setAttrName(baseAttrInfo.getAttrName());
            searchAttr.setAttrValue(baseAttrInfo.getAttrValueList().get(0).getValueName());
            return searchAttr;
        }).collect(Collectors.toList());

        ArrayList<SearchAttr> searchAttrs = new ArrayList<>();
        //  循环遍历.
        attrList.forEach(baseAttrInfo -> {
            SearchAttr searchAttr = new SearchAttr();
            searchAttr.setAttrId(baseAttrInfo.getId());
            searchAttr.setAttrName(baseAttrInfo.getAttrName());
            searchAttr.setAttrValue(baseAttrInfo.getAttrValueList().get(0).getValueName());
            searchAttrs.add(searchAttr);
        });

        //  赋值平台属性集合
        goods.setAttrs(searchAttrList);
        //  谁能做保存？ 自定义一个接口。
        goodsRepository.save(goods);
    }

    @Override
    public void onSale(List<Long> skuIds) {
        //
    }

    @Override
    public SearchResponseVo search(SearchParam searchParam) throws IOException {
        //  操作es 的客户端. restHighLevelClient
        /*
        1.  先生成dsl 语句.
        2.  执行这个dsl语句.
        3.  封装返回结果集
         */
        //  SearchRequest searchRequest = new SearchRequest();
        /*
        GET /goods/_search
        {
          "query": {
            "match_all": {}
          }
        }
         */
        //  SearchRequest 查询请求对象 {装有dsl 语句}
        //  SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        //  构建SearchRequest 对象
        SearchRequest searchRequest = this.queryDsl(searchParam);
        //   执行这个dsl语句.
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //  封装返回结果集
        SearchResponseVo searchResponseVo = this.parseSearchResponse(searchResponse);
        /*
            返回这对象的本质：给这个对象的属性赋值.
            List<SearchResponseTmVo> trademarkList;
            List<SearchResponseAttrVo> attrsList = new ArrayList<>();
            List<Goods> goodsList = new ArrayList<>();
            Long total;//总记录数

            这四个属性赋值 在 parseSearchResponse() 方法中
            -----
            后三个在当前这个search方法中实现.
            Integer pageSize;//每页显示的内容
            Integer pageNo;//当前页面
            Long totalPages;
         */
        searchResponseVo.setPageSize(searchParam.getPageSize());
        searchResponseVo.setPageNo(searchParam.getPageNo());
        //  分页公式： 10 3 4   || 9 3 3
        //  Long totalPage = searchResponseVo.getTotal()%searchParam.getPageSize()==0?searchResponseVo.getTotal()/searchParam.getPageSize():searchResponseVo.getTotal()/searchParam.getPageSize()+1;
        Long totalPage = (searchResponseVo.getTotal() + searchParam.getPageSize() - 1)/searchParam.getPageSize();
        searchResponseVo.setTotalPages(totalPage);
        //  返回对象.
        return searchResponseVo;
    }

    /**
     * 数据转换！
     * @param searchResponse
     * @return
     */
    private SearchResponseVo parseSearchResponse(SearchResponse searchResponse) {
        /*
            返回这对象的本质：给这个对象的属性赋值.
            List<SearchResponseTmVo> trademarkList;
            List<SearchResponseAttrVo> attrsList = new ArrayList<>();
            List<Goods> goodsList = new ArrayList<>();
            Long total;//总记录数
            这四个属性赋值 在 parseSearchResponse() 方法中
         */
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //  获取hits
        SearchHits hits = searchResponse.getHits();
        //  获取到总记录数
        searchResponseVo.setTotal(hits.getTotalHits().value);
        //  声明一个集合来存储goods
        List<Goods> goodsList = new ArrayList<>();

        //  获取Goods 集合
        SearchHit[] subHits = hits.getHits();
        for (SearchHit subHit : subHits) {
            //  获取 _source节点数据.
            String sourceAsString = subHit.getSourceAsString();
            //  这个json 字符串是 Goods
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            //  有个特殊情况： _source节点下的title 不是高亮！
            if (subHit.getHighlightFields().get("title")!=null){
                //  说明用户是通过全文检索来获取数据的
                Text title = subHit.getHighlightFields().get("title").getFragments()[0];
                goods.setTitle(title.toString());
            }
            goodsList.add(goods);
        }
        searchResponseVo.setGoodsList(goodsList);

        //  品牌数据！在聚合中获取. 将聚合变为一个map 集合！
        //  key = attrAgg  value =  Aggregation  |  key = tmIdAgg  value =  Aggregation
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        //  因为我们要获取到 tmId,tmName,tmLogoUrl return  SearchResponseTmVo 在把这个对象变为集合。
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map((bucket) -> {
            //  声明一个对象
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            //  获取到品牌Id
            String tmId = ((Terms.Bucket) bucket).getKeyAsString();
            searchResponseTmVo.setTmId(Long.parseLong(tmId));
            //  赋值tmName
            ParsedStringTerms tmNameAgg = ((Terms.Bucket) bucket).getAggregations().get("tmNameAgg");
            searchResponseTmVo.setTmName(tmNameAgg.getBuckets().get(0).getKeyAsString());

            //  赋值tmLogoUrl
            ParsedStringTerms tmLogoUrlAgg = ((Terms.Bucket) bucket).getAggregations().get("tmLogoUrlAgg");
            searchResponseTmVo.setTmLogoUrl(tmLogoUrlAgg.getBuckets().get(0).getKeyAsString());
            //  返回品牌。
            return searchResponseTmVo;
        }).collect(Collectors.toList());
        searchResponseVo.setTrademarkList(trademarkList);

        //  获取平台属性： 从聚合中获取. 数据类型是nested.
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        //  获取平台属性对象集合.
        List<SearchResponseAttrVo> attrsList = attrIdAgg.getBuckets().stream().map((bucket) -> {
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
            //  平台属性Id
            String attrId = ((Terms.Bucket) bucket).getKeyAsString();
            searchResponseAttrVo.setAttrId(Long.parseLong(attrId));

            //  平台属性名：attrName 类型转换是为了获取桶集合！
            ParsedStringTerms attrNameAgg = ((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseAttrVo.setAttrName(attrName);

            //  平台属性值：List<String> attrValueList
            ParsedStringTerms attrValueAgg = ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");

            //  第一种：
            //            List<String> valueList = new ArrayList<>();
            //            for (Terms.Bucket attrValueAggBucket : attrValueAgg.getBuckets()) {
            //                String attrValue = attrValueAggBucket.getKeyAsString();
            //                valueList.add(attrValue);
            //            }
            //  第二种：拉姆达表达式.
            List<String> valueList = attrValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());

            //  设置平台属性值.
            searchResponseAttrVo.setAttrValueList(valueList);
            return searchResponseAttrVo;
        }).collect(Collectors.toList());

        searchResponseVo.setAttrsList(attrsList);
        //  返回。
        return searchResponseVo;
    }

    /**
     * 动态生成dsl 语句。
     * @param searchParam
     * @return
     */
    private SearchRequest queryDsl(SearchParam searchParam) {
        //  {} 查询器：
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //  {bool}
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //  判断 是否是根据关键词检索
        if (!StringUtils.isEmpty(searchParam.getKeyword())){
            //  {bool must match}
            boolQueryBuilder.must(QueryBuilders.matchQuery("title",searchParam.getKeyword()).operator(Operator.AND));

            //  高亮： 无检索-不高亮！
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("title");
            highlightBuilder.preTags("<span style=color:red>");
            highlightBuilder.postTags("</span>");
            //  设置好的高亮对象放入查询器中.
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        //  判断用户是否根据分类Id 进行检索！
        //  term 相当于单值匹配  category3Id = ?   terms 相当于多值匹配  category3Id in (?,?,?);
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())){
            //  {bool filter term}
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id",searchParam.getCategory3Id()));
        }

        if (!StringUtils.isEmpty(searchParam.getCategory2Id())){
            //  {bool filter term}
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id",searchParam.getCategory2Id()));
        }

        if (!StringUtils.isEmpty(searchParam.getCategory1Id())){
            //  {bool filter term}
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id",searchParam.getCategory1Id()));
        }

        //  根据品牌Id 进行过滤
        //  &trademark=1:小米
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)){
            //  使用：进行分割
            String[] split = trademark.split(":");
            //  判断数组长度>0 或数组长度 == 2
            if (split!=null && split.length==2){
                // split[0];
                //  {bool filter term }
                boolQueryBuilder.filter(QueryBuilders.termQuery("tmId",split[0]));
            }
        }
        //  根据平台属性值 进行过滤
        //  props=23:12G:运行内存&props=106:安卓手机:手机一级
        String[] props = searchParam.getProps();
        if (props!=null && props.length>0){
            //  循环遍历
            for (String prop : props) {
                // prop=23:12G:运行内存
                String[] split = prop.split(":");
                //  中间层的bool
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                //  最内层
                BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",split[0]));
                subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue",split[1]));
                //  bool -->must-->nested
                boolQuery.must(QueryBuilders.nestedQuery("attrs",subBoolQuery, ScoreMode.None));

                //  配置最外层的bool
                boolQueryBuilder.filter(boolQuery);
            }
        }
        //  分页
        //  (pageNo-1)*pageSize
        int from = (searchParam.getPageNo()-1)*searchParam.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(searchParam.getPageSize());


        //  {query: bool}
        searchSourceBuilder.query(boolQueryBuilder);

        //  设置一个排序：
        //  1：表示综合排序hotScore  order=1:asc order=1:desc  2:表示按照价格price进行排序的 order=2:asc  order=2:desc
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)){
            //  声明一个字段来接受排序规则的字段
            String field = "";
            //  先分割字符串
            String[] split = order.split(":");
            if (split!=null && split.length==2){
                switch (split[0]){
                    case "1":
                        field="hotScore";
                        break;
                    case "2":
                        field="price";
                        break;
                }
                //  小三元
                searchSourceBuilder.sort(field,"asc".equals(split[1])? SortOrder.ASC:SortOrder.DESC);
            }else {
                searchSourceBuilder.sort("hotScore",SortOrder.DESC);
            }
        }

        //  聚合：
        //  聚合品牌
        searchSourceBuilder.aggregation(AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"))
        );

        //  聚合平台属性
        //  数据类型为nested
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","attrs")
            .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                       .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                       .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))
            )
        );

        //  查询数据的时候？ GET /goods/_search
        //  声明一个SearchRequest 对象
        SearchRequest searchRequest = new SearchRequest("goods");

        //  将构建的dsl 语句放入了source 中.
        searchRequest.source(searchSourceBuilder);

        //  设置获取哪些字段数据. 在Goods 中只要 "id", "defaultImg","title","price" 字段。 品牌，平台属性等数据从聚合中获取！
        searchSourceBuilder.fetchSource(new String[] {"id", "defaultImg","title","price"},null);

        //  打印dsl 语句.
        System.out.println("dsl:\t"+searchSourceBuilder.toString());
        //  返回对象.
        return searchRequest;
    }

    @Override
    public void cancelSale(Long skuId) {
        //  下架：
        goodsRepository.deleteById(skuId);
    }

    //  更新热度排名
    @Override
    public void incrHotScore(Long skuId) {
        //  借助redis ，需要导入工具类。
        //  分析使用的数据类型 ，如何实现自增！ 每次自增1;  string incr / incrby   Zset zincrby hotscore 1 sku:1
        //  定义一个key
        String key = "hotScore";
        Double count = this.redisTemplate.opsForZSet().incrementScore(key, "skuId:" + skuId, 1);
        //  判断是否需要更新es
        if (count%10==0){
            //此时更新一下es的热度排名
            Optional<Goods> optional = this.goodsRepository.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(count.longValue());
            this.goodsRepository.save(goods);
        }
    }

    @Override
    public void cancelSale(List<Long> skuIds) {

    }
}