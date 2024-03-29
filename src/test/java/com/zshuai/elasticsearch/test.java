package com.zshuai.elasticsearch;

import com.zshuai.elasticsearch.pojo.Item;
import com.zshuai.elasticsearch.repository.ItemRepository;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.metrics.avg.InternalAvg;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ElasticSearch.class)
public class test {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    public void testCreate(){
        // 创建索引，会根据Item类的@Document注解信息来创建
        elasticsearchTemplate.createIndex(Item.class);
        // 配置映射，会根据Item类中的id、Field等字段来自动完成映射
        elasticsearchTemplate.putMapping(Item.class);
    }

    @Test
    public void deleteIndex() {
        elasticsearchTemplate.deleteIndex("item");
    }

    @Test
    public void index(){
        Item item = new Item(1L,"小米手机", "手机", "小米",  3499.00, "http://image.leyou.com/13123.jpg");
        itemRepository.save(item);
    }
    @Test
    public void indexList() {
        List<Item> list = new ArrayList<>();
        list.add(new Item(2L, "坚果手机R1", " 手机", "锤子", 3699.00, "http://image.leyou.com/123.jpg"));
        list.add(new Item(3L, "华为META10", " 手机", "华为", 4499.00, "http://image.leyou.com/3.jpg"));
        // 接收对象集合，实现批量新增
        itemRepository.saveAll(list);
    }

    @Test
    public void testFind(){
        //查询全部，并按照价格降序排序
        Iterable<Item> items = itemRepository.findAll(Sort.by(Sort.Direction.DESC, "price"));

        items.forEach(item -> System.out.println(item.toString()));
    }

    @Test
    public void indexList2() {
        List<Item> list = new ArrayList<>();
        list.add(new Item(1L, "小米手机7", "手机", "小米", 3299.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Item(2L, "坚果手机R1", "手机", "锤子", 3699.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Item(3L, "华为META10", "手机", "华为", 4499.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Item(4L, "小米Mix2S", "手机", "小米", 4299.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Item(5L, "荣耀V10", "手机", "华为", 2799.00, "http://image.leyou.com/13123.jpg"));
        // 接收对象集合，实现批量新增
        itemRepository.saveAll(list);
    }

    @Test
    public void queryByPriceBetween(){
        List<Item> list = itemRepository.findByPriceBetween(2000.00, 3500.00);
        list.forEach(item -> System.out.println(item.toString()));
    }

    @Test
    public void testQuery(){
        //词条查询
        MatchQueryBuilder queryBuilder = QueryBuilders.matchQuery("title", "小米");

        //执行查询
        Iterable<Item> items = itemRepository.search(queryBuilder);
        items.forEach(item -> System.out.println(item.toString()));
    }

    //自定义查询
    @Test
    public void testNativeQuery(){
        //构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        //添加基本的分词查询
        queryBuilder.withQuery(QueryBuilders.matchQuery("title", "小米"));

        // 执行查询，获取结果
        Page<Item> items = itemRepository.search(queryBuilder.build());

        //打印总条数
        System.out.println(items.getTotalElements()+"总条数");
        //打印总页数
        System.out.println(items.getTotalPages()+":总页数");

        items.forEach(item ->
                        System.out.println(item.toString())
                );
    }

    @Test
    public void testNativeQueryPages(){
        // 构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 添加基本的分词查询
        queryBuilder.withQuery(QueryBuilders.termQuery("category", "手机"));

        // 初始化分页参数
        int page = 0;
        int size = 3;
        // 设置分页参数
        queryBuilder.withPageable(PageRequest.of(page, size));

        // 执行搜索，获取结果
        Page<Item> items = this.itemRepository.search(queryBuilder.build());
        // 打印总条数
        System.out.println(items.getTotalElements());
        // 打印总页数
        System.out.println(items.getTotalPages());
        // 每页大小
        System.out.println(items.getSize());
        // 当前页
        System.out.println(items.getNumber());
        items.forEach(System.out::println);
    }

    @Test
    public void testSort(){
        // 构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 添加基本的分词查询
        queryBuilder.withQuery(QueryBuilders.termQuery("category", "手机"));

        // 排序
        queryBuilder.withSort(SortBuilders.fieldSort("price").order(SortOrder.DESC));

        // 执行搜索，获取结果
        Page<Item> items = this.itemRepository.search(queryBuilder.build());
        // 打印总条数
        System.out.println(items.getTotalElements());
        items.forEach(System.out::println);
    }

    @Test
    public void testAgg(){
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //不查询任何结果
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""}, null));
        // 1.添加一个新的聚合，聚合类型为terms，聚合名称为brands，聚合字段为brand
        queryBuilder.addAggregation(
                AggregationBuilders.terms("brands").field("brand"));
        //2. 查询，需要将结果强制转换为AggregatePage类型
        AggregatedPage<Item> aggPage = (AggregatedPage<Item>) itemRepository.search(queryBuilder.build());

        //3. 解析
        //3.1 从结果集中取出名为brands的聚合
        // 因为是利用String类型字段进行trem聚合，所以结果需要强制转换为StringTerm类型
        StringTerms agg = (StringTerms) aggPage.getAggregation("brands");

        //3.2 获取桶
        List<StringTerms.Bucket> buckets = agg.getBuckets();

        //3.3 遍历桶
        for(StringTerms.Bucket bucket : buckets){
            //3.4 获取桶中的key，既品牌名称
            System.out.println(bucket.getKeyAsString());
            //3.5 获取桶中文档的数量
            System.out.println(bucket.getDocCount());
        }

    }

    @Test
    public void testSubAgg(){
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 不查询任何结果
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""}, null));
        //1. 添加一个新的聚合，聚合类型为terms。聚合名称为brands，聚合字段为brand
        queryBuilder.addAggregation(
                AggregationBuilders.terms("brands").field("brand")
                .subAggregation(AggregationBuilders.avg("priceAvg").field("price"))//在品牌聚和桶内进行嵌套聚和，求平均值
        );
        //2. 查询，需要把结果强转为AggregdatePage类型
        AggregatedPage<Item> aggPage = (AggregatedPage<Item>) itemRepository.search(queryBuilder.build());

        //3. 解析
        //3.1 从结果中取出名为brands的聚合
        // 因为是利用String类型字段进行的trems聚合，所以需要将结果强转为StringTrem类型
        StringTerms agg = (StringTerms) aggPage.getAggregation("brands") ;
        //3.2 获取桶
        List<StringTerms.Bucket> buckets = agg.getBuckets();
        //3.3 遍历
        for(StringTerms.Bucket bucket : buckets){
            //3.4 获取桶中的key 既品牌名称 以及获取桶中的文档数量
            System.out.println(bucket.getKeyAsString()+",共计:"+ bucket.getDocCount()+"台");

            //3.5 获取子聚合结果
            InternalAvg priceAvg = (InternalAvg)bucket.getAggregations().asMap().get("priceAvg");
            System.out.println("平均售价:"+priceAvg.getValue());

        }
    }


}



