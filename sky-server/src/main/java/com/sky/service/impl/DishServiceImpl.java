package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
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
    @Autowired
    private SetmealDishMapper setmealDishMapper;

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

    /***
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        // 1. 开启分页，设置页码、每页条数
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        // 2. 执行MyBatis查询，返回Page<DishVO>分页对象
        Page<DishVO> page=dishMapper.pageQuery(dishPageQueryDTO);
        // 3. 封装自定义分页返回对象并返回
        return new PageResult(page.getTotal(),page.getResult());
    }

    /***
     * 批量删除菜品
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能够删除，即是否存在起售中的菜品
        for (Long id : ids) {
            Dish dish=dishMapper.getById(id);
            if (dish.getStatus()== StatusConstant.ENABLE){
                //当前菜品处于起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断当前菜品是否能够删除，即当前菜品是否被套餐关联
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds!=null&&setmealIds.size()>0){
            //当前菜品被套餐关联了，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品表中的菜品数据
        for (Long id : ids) {
            dishMapper.deleteById(id);
            //删除菜品关联的口味数据
            dishFlavorMapper.deleteByDishId(id);
        }
    }
}
