package com.platform.admin.consumer.feignClient;

import com.platform.admin.consumer.hystrix.GoodsServiceHystrix;
import com.platform.common.modal.manual.ManualMessageParam;
import com.platform.common.modal.goods.MyCategory;
import com.platform.common.modal.ResultInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * Created by Huangyonghao on 2019/8/28 11:02.
 */

@FeignClient(value = "com-platform-admin-service", fallback = GoodsServiceHystrix.class)
public interface GoodsService {

    @PostMapping("/goods/clearExpireGoods")
    ResultInfo clearExpireGoods();


    @PostMapping("/goods/appendManualMessage")
    ResultInfo appendManualMessage(ManualMessageParam msg);

    @PostMapping("/goods/getMyCategorys")
    ResultInfo<List<MyCategory>> getMyCategorys();
}
