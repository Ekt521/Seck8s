package com.seck8s.ticket.service;

import com.seck8s.ticket.entity.Sku;
import com.seck8s.ticket.entity.Ticket;

import java.util.List;

public interface TicketService {

    List<Ticket> listTickets(String keyword);

    Ticket getTicket(String ticketId);

    /** 某票的 SKU 列表，附带实时库存余量 */
    List<Sku> listSkusWithStock(String ticketId);
}
