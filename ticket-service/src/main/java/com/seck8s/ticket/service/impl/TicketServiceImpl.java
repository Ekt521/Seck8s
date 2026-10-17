package com.seck8s.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seck8s.common.exception.BizException;
import com.seck8s.common.result.ResultCode;
import com.seck8s.ticket.entity.Sku;
import com.seck8s.ticket.entity.Ticket;
import com.seck8s.ticket.feign.InventoryClient;
import com.seck8s.ticket.mapper.SkuMapper;
import com.seck8s.ticket.mapper.TicketMapper;
import com.seck8s.ticket.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements TicketService {

    @Autowired
    private SkuMapper skuMapper;

    @Autowired(required = false)
    private InventoryClient inventoryClient;   // 库存服务可能未部署，容忍降级

    @Override
    public List<Ticket> listTickets(String keyword) {
        return this.list(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getTicketStatus, 1)
                .like(StringUtils.hasText(keyword), Ticket::getName, keyword)
                .orderByDesc(Ticket::getCreateTime));
    }

    @Override
    public Ticket getTicket(String ticketId) {
        Ticket ticket = this.getOne(new LambdaQueryWrapper<Ticket>().eq(Ticket::getTicketId, ticketId));
        if (ticket == null) {
            throw new BizException(ResultCode.NOT_FOUND, "票不存在");
        }
        return ticket;
    }

    @Override
    public List<Sku> listSkusWithStock(String ticketId) {
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getTicketId, ticketId)
                .eq(Sku::getSkuStatus, 1));
        // 填充实时库存余量（失败降级为 -1 表示不可查）
        for (Sku sku : skus) {
            try {
                Integer stock = inventoryClient == null ? null : inventoryClient.getStock(sku.getSkuId());
                sku.setExtStock(stock == null ? -1 : stock);
            } catch (Exception e) {
                log.warn("查询库存失败 skuId={}: {}", sku.getSkuId(), e.getMessage());
                sku.setExtStock(-1);
            }
        }
        return skus;
    }
}
