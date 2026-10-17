package com.seck8s.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seck8s.payment.entity.MessageOutbox;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageOutboxMapper extends BaseMapper<MessageOutbox> {
}
