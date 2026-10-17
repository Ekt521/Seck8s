package com.seck8s.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seck8s.order.entity.MessageOutbox;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageOutboxMapper extends BaseMapper<MessageOutbox> {
}
