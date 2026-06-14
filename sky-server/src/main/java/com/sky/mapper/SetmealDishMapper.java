package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    //根据菜品id查询套餐id，它们是多对多关系，可能查出多个套餐，因此返回值为list
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

}
