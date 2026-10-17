package com.seck8s.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seck8s.inventory.entity.SkuStock;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SkuStockMapper extends BaseMapper<SkuStock> {
}
