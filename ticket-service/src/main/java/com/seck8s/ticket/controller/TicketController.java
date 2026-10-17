package com.seck8s.ticket.controller;

import com.seck8s.common.result.Result;
import com.seck8s.ticket.entity.Sku;
import com.seck8s.ticket.entity.Ticket;
import com.seck8s.ticket.service.TicketService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /** 查票列表（可带关键字） */
    @GetMapping
    public Result<List<Ticket>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(ticketService.listTickets(keyword));
    }

    /** 查票详情 */
    @GetMapping("/{ticketId}")
    public Result<Ticket> get(@PathVariable String ticketId) {
        return Result.ok(ticketService.getTicket(ticketId));
    }

    /** 票的 SKU 列表（含实时库存余量） */
    @GetMapping("/{ticketId}/skus")
    public Result<List<Sku>> skus(@PathVariable String ticketId) {
        return Result.ok(ticketService.listSkusWithStock(ticketId));
    }
}
