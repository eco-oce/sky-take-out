package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    /***
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    @Override
    @Transactional   //涉及菜品表、口味表等多个表的操作，因此需要添加事务注解以保证数据一致性
    public void saveWithFlavor(DishDTO dishDTO) {
        //创建实体对象dish
        Dish dish = new Dish();

        //通过对象属性拷贝将dishDTO中的值拷贝至dish
        BeanUtils.copyProperties(dishDTO,dish);

        //向菜品表插入一条数据(DishDTO中包含了口味数据，而现在只是向菜品表插入数据，因此此处传入实体对象dish即可)
        dishMapper.insert(dish);

        //获取主键值（插入后自动回写的ID）
        Long dishId=dish.getId();

        //取出口味数据并存入集合
        List<DishFlavor> flavors = dishDTO.getFlavors();
        //判断用户是否提交了口味数据
        if (flavors!=null&&flavors.size()>0){
            //遍历集合，为每个DishFlavor的dishId属性赋值
            flavors.forEach(dishFlavor->{
                dishFlavor.setDishId(dishId);
            });
            //向口味表插入n条数据(一个菜品可能对应多个口味)
            //无需遍历集合逐条插入数据，而是通过传入集合对象的方式批量插入
            dishFlavorMapper.insertBatch(flavors);
        }
    }
}
