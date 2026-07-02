package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealService setmealService;

    /***
     * 新增套餐，同时需要保存套餐和菜品关系
     * @param setmealDTO
     */
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //向套餐表插入数据
        setmealMapper.insert(setmeal);

        //获取生成的套餐id
        //Mapper的insert方法 XML 中必须开启主键回填，才能通过setmeal.getId()拿到数据库自增生成的套餐主键
        Long setmealId = setmeal.getId();

        List<SetmealDish> setmealDishes=setmealDTO.getSetmealDishes();
        //流式遍历，给每一条关联记录统一赋值 setmealId = 刚插入的套餐主键；
        //保证中间表每条数据都能和当前套餐绑定。
        setmealDishes.forEach(setmealDish->{
            setmealDish.setSetmealId(setmealId);
        });
        //保存套餐和菜品的关联关系
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /***
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO){
        // 1. 从DTO取出分页参数：当前页码、每页条数
        int pageNum=setmealPageQueryDTO.getPage();
        int pageSize=setmealPageQueryDTO.getPageSize();

        // 2. PageHelper开启分页，拦截下一条SQL自动拼接limit、自动统计总条数
        PageHelper.startPage(pageNum,pageSize);

        // 3. 执行多条件分页查询，返回Page对象（PageHelper专属分页容器）
        Page<SetmealVO> page=setmealMapper.pageQuery(setmealPageQueryDTO);

        // 4. 封装自定义统一分页返回体，总记录数 + 当前页数据列表
        return new PageResult(page.getTotal(),page.getResult());
    }

    /***
     * 批量删除套餐
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        ids.forEach(id->{
           Setmeal setmeal =setmealMapper.getById(id);
           if (StatusConstant.ENABLE ==setmeal.getStatus()){
               //起售中的套餐不能删除
               throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
           }
        });

        ids.forEach(setmealId->{
            //删除套餐表中的数据
            setmealMapper.deleteById(setmealId);
            //删除套餐菜品关系表中的数据
            setmealDishMapper.deleteBySetmealId(setmealId);
        });
    }

    /***
     * 根据id查询套餐和关联的菜品数据
     * @param id
     * @return
     */
    public SetmealVO getByIdWithDish(Long id) {
        // 1. 根据套餐id查询套餐基础信息（对应setmeal主表）
        Setmeal setmeal = setmealMapper.getById(id);
        // 2. 根据套餐id查询中间表，获取该套餐绑定的所有菜品关联数据
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        // 3. 创建视图VO对象，用于向前端返回完整复合数据
        SetmealVO setmealVO = new SetmealVO();
        // 工具类拷贝：将套餐实体中同名属性复制到VO
        BeanUtils.copyProperties(setmeal, setmealVO);
        // 手动给VO设置关联菜品集合（实体Setmeal无该集合字段，无法自动拷贝）
        setmealVO.setSetmealDishes(setmealDishes);
        // 封装完成，返回携带套餐+菜品的视图对象
        return setmealVO;
    }

    /***
     * 修改套餐
     * @param setmealDTO
     */
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        // 1. 初始化套餐实体，将DTO中的套餐基础属性拷贝到Setmeal实体
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        // 2. 更新setmeal套餐主表的基础信息
        setmealMapper.update(setmeal);
        // 取出当前正在修改的套餐主键ID，用于操作中间关联表
        Long setmealId = setmealDTO.getId();

        // 3. 根据套餐id，删除setmeal_dish中间表该套餐下所有旧的菜品关联数据
        setmealDishMapper.deleteBySetmealId(setmealId);
        // 获取前端传入编辑后的最新菜品绑定列表
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        // 遍历所有菜品关联对象，统一填充套餐外键setmealId（前端未携带该字段）
        setmealDishes.forEach(setmealDish->{
            setmealDish.setSetmealId(setmealId);
        });
        // 4. 批量插入最新的套餐-菜品关联数据，完成菜品更新
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /***
     * 套餐起售停售
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        // 判断：当前操作是【起售套餐】，需要校验套餐绑定的全部菜品状态
        if(StatusConstant.ENABLE ==status){
            // 根据套餐id关联查询该套餐绑定的所有菜品信息
            List<Dish> dishList =dishMapper.getBySetmealId(id);
            // 仅当套餐绑定了菜品时，执行菜品状态循环校验
            if(dishList != null && dishList.size()>0){
                // 若任意一道菜品状态为停售，抛出自定义业务异常，终止整个方法
                dishList.forEach(dish->{
                    if (StatusConstant.DISABLE ==dish.getStatus()){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }
        // 构建套餐实体，仅填充主键id和待更新的目标状态
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

}
