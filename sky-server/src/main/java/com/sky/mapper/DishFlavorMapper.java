package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /***
     * 批量插入口味数据
     * 需要使用到动态SQL，因此写入xml文件
     * @param flavors
     */
    void insertBatch(List<DishFlavor> flavors);
}
