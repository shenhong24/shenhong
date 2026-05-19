package com.sky.controller.user;

import com.alibaba.fastjson.JSON;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        log.info("根据分类id查询菜品: {}", categoryId);

        //构造redis中的key，规则：dish_分类id
        String key="dish_"+categoryId;
        //查询redis中是否存在菜品数据(存的string
        List<DishVO> list =null;
        String json =(String) redisTemplate.opsForValue().get(key);
        if (json != null && !json.isEmpty()) {
            // 先解析，再判断
            list = JSON.parseArray(json, DishVO.class);
            if (list != null && !list.isEmpty()) {
                log.info("命中缓存");
                return Result.success(list);
            }
        }
        if (list!=null&&list.size()>0){
            //如果存在，直接返回，无须查询数据库（将json反序列化成list<Dishvo>）
            list = JSON.parseArray(json, DishVO.class);
            log.info("命中缓存");
            return Result.success(list);
        }



        //如果不存在，查询数据库
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品
        list = dishService.listWithFlavor(dish);
        //将查询到的数据放入 Redis (必须转为 String 存入)
        String jsonStr = JSON.toJSONString(list);
        // 建议设置过期时间，比如 30 分钟
        redisTemplate.opsForValue().set(key, jsonStr, 5, TimeUnit.MINUTES);
        log.info("存入缓存");
        return Result.success(list);
    }

}
