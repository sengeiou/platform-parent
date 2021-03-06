package com.platform.common.util.es;

import com.alibaba.fastjson.JSON;
import com.platform.common.contanst.EsConstanst;
import com.platform.common.modal.goods.GoodsInfo;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.platform.common.util.es.EsSearchUtil.queryStr;

@Slf4j
public class EsWriteUtil {

    private static final RequestOptions defaultOptions = RequestOptions.DEFAULT;
    private static final String ES_GOODS_INDEX = "goodsinfo";

    /**
     * 添加
     *
     * @param doc
     */
    public static void set(Map<String, Object> doc, String index, String idColumn) {
        Object id = doc.get(idColumn);
        if (StringUtils.isEmpty(id)) {
            throw new RuntimeException("主键值" + idColumn + "不可为空");
        }
        IndexRequest request = new IndexRequest(index);
        request.id(id.toString());
        request.source(doc).setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        RestHighLevelClient client = RestClientUtil.highClient();
        try {

            client.index(request, defaultOptions);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void set(Object object, String index) {
        if (object == null) {
            return;
        }
        Map<String, Object> map;
        map = JSON.parseObject(JSON.toJSONString(object));
        set(map, index, EsConstanst.ES_BASE_ID_COLUMN_NAME);
    }


    /**
     * 批量添加
     *
     * @param docs
     * @return
     */
    public static void addList(List<Map<String, Object>> docs, String index, String idColumn) {

        RestHighLevelClient client = RestClientUtil.highClient();
        BulkRequest request = new BulkRequest();
        docs.forEach(obj -> {
            Object id = obj.get(idColumn);
            if (StringUtils.isEmpty(id)) {
                throw new RuntimeException("主键" + idColumn + "不可为空");
            }
            IndexRequest indexBuilder = new IndexRequest(index);
            indexBuilder.id(id.toString());
            indexBuilder.source(obj);
            request.add(indexBuilder);
        });
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        try {

            BulkResponse response = client.bulk(request, defaultOptions);
            if (response.hasFailures()) {
                throw new RuntimeException(response.buildFailureMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void addList(List<Object> objects, String index) {
        if (objects == null || objects.size() == 0) {
            return;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            Object object = objects.get(i);
            if (object != null) {
                Map<String, Object> map;
                map = JSON.parseObject(JSON.toJSONString(object));
                list.add(map);
            }
        }
        if (list.size() > 0) {
            addList(list, index, EsConstanst.ES_BASE_ID_COLUMN_NAME);
        }
    }

    /**
     * 更新
     *
     * @param doc
     * @param index
     * @param idColumn
     */
    public static void update(Map<String, Object> doc, String index, String idColumn) {

        RestHighLevelClient client = RestClientUtil.highClient();
        BulkRequest request = new BulkRequest();
        Object id = doc.get(idColumn);
        if (StringUtils.isEmpty(id)) {
            throw new RuntimeException("主键不可为空");
        }
        UpdateRequest update = new UpdateRequest(index, id.toString());
        update.doc(doc);
        update.docAsUpsert(true);
        request.add(update);
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        try {
            BulkResponse bulk = client.bulk(request, defaultOptions);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void update(Object object, String index) {

        if (object == null) {
            return;
        }
        Map<String, Object> map;
        map = JSON.parseObject(JSON.toJSONString(object));
        update(map, index, EsConstanst.ES_BASE_ID_COLUMN_NAME);
    }

    /**
     * 批量更新
     *
     * @param docs
     * @param index
     * @param type
     */
    public static void updateList(List<Map<String, Object>> docs, String index, String type, String idColumn) {

        RestHighLevelClient client = RestClientUtil.highClient();
        BulkRequest request = new BulkRequest();
        docs.forEach(obj -> {
            Object id = obj.get("id");
            if (StringUtils.isEmpty(id)) {
                throw new RuntimeException("主键值不可为空");
            }
            UpdateRequest update = new UpdateRequest(index,
                    type,
                    id.toString());
            update.doc(obj);
            update.docAsUpsert(true);
            request.add(update);
        });
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        try {
            client.bulk(request, defaultOptions);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除
     *
     * @param ids
     * @param index
     * @param type
     */
    public static void delete(List<String> ids, String index, String type) {

        RestHighLevelClient client = RestClientUtil.highClient();
        BulkRequest request = new BulkRequest();
        ids.forEach(id -> {
            DeleteRequest delete = new DeleteRequest(index, type, id);
            request.add(delete);
        });
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        try {
            client.bulk(request, defaultOptions);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void deleteByQuery(String index, Map<String, Object> params, List<RangeQueryEntity> rangeQuery, String filterQuery) {
        RestHighLevelClient client = RestClientUtil.highClient();
        DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(index);
        BoolQueryBuilder booleanQueryBuilder = buildRangeQuery(params, rangeQuery, filterQuery);
        deleteByQueryRequest.setQuery(booleanQueryBuilder);
        try {
            client.deleteByQuery(deleteByQueryRequest, defaultOptions);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
        }

    }

    /**
     * updateByQuery
     *
     * @param params
     * @param rangeQuery
     * @param scriptStr
     * @param index
     */
    public static void updateByQuery(Map<String, Object> params, List<RangeQueryEntity> rangeQuery, String scriptStr, String index) {


        BoolQueryBuilder booleanQueryBuilder = buildRangeQuery(params, rangeQuery, null);
        updateByQuery(index, scriptStr, booleanQueryBuilder);

    }

    private static BoolQueryBuilder buildRangeQuery(Map<String, Object> params, List<RangeQueryEntity> rangeQuery, String filterQuery) {
        BoolQueryBuilder booleanQueryBuilder = QueryBuilders.boolQuery();
        if (params != null && !params.isEmpty()) {
            String query = queryStr(params);
            QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryStringQuery(query);
            booleanQueryBuilder.must(queryStringQueryBuilder);
        }
        if (!StringUtils.isEmpty(filterQuery)) {
            QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryStringQuery(filterQuery);
//            if (!StringUtils.isEmpty(anaylzer)) {
//                queryStringQueryBuilder.analyzer(anaylzer);
//            }
            booleanQueryBuilder.must(queryStringQueryBuilder);
        }
        if (rangeQuery != null && rangeQuery.size() > 0) {
            for (RangeQueryEntity rangeQueryEntity : rangeQuery) {
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(rangeQueryEntity.getFieldName());
                if (rangeQueryEntity.getFieldName().indexOf("Time") > -1) {
                    if (rangeQueryEntity.getMinValue() != null) {
                        rangeQueryBuilder.from(rangeQueryEntity.getMinValue());
                    }
                    if (rangeQueryEntity.getMaxValue() != null) {
                        rangeQueryBuilder.to(rangeQueryEntity.getMaxValue());
                    }
                } else {
                    if (rangeQueryEntity.getMinValue() != null) {
                        rangeQueryBuilder.gte(rangeQueryEntity.getMinValue());
                    }
                    if (rangeQueryEntity.getMaxValue() != null) {
                        rangeQueryBuilder.lte(rangeQueryEntity.getMaxValue());
                    }
                }
                booleanQueryBuilder.must(rangeQueryBuilder);
            }
        }
        return booleanQueryBuilder;
    }

    public static void updateByQuery(String indices, String scriptStr, String field, List<String> values) {
        if (values == null || values.size() == 0) {
            return;
        }
        BoolQueryBuilder booleanQueryBuilder = QueryBuilders.boolQuery();
        booleanQueryBuilder.should(new TermsQueryBuilder(field, values));
        updateByQuery(indices, scriptStr, booleanQueryBuilder);
    }

    private static void updateByQuery(String index, String scriptStr, BoolQueryBuilder booleanQueryBuilder) {
        RestHighLevelClient client = RestClientUtil.highClient();
        UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(index);
        updateByQueryRequest.setQuery(booleanQueryBuilder);
        updateByQueryRequest.setScroll(TimeValue.timeValueMinutes(30));
        updateByQueryRequest.setConflicts("proceed");
        updateByQueryRequest.setAbortOnVersionConflict(false);
        Script script = new Script(scriptStr);
        updateByQueryRequest.setScript(script);
        client.updateByQueryAsync(updateByQueryRequest, RequestOptions.DEFAULT, new ActionListener<BulkByScrollResponse>() {
            @Override
            public void onResponse(BulkByScrollResponse bulkByScrollResponse) {
                log.info("异步更新ES成功：");
            }

            @Override
            public void onFailure(Exception e) {
                log.info("异步更新ES失败：" + e.getMessage() + e.getStackTrace());
            }
        });
    }

    public static void main(String[] args) {

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("itemId", 587503710029L);
        EsWriteUtil.updateByQuery(queryMap, null, "ctx._source.commissionRate = 100", ES_GOODS_INDEX);
    }

}
