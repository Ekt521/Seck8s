package com.seck8s.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seck8s.ticket.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
}
