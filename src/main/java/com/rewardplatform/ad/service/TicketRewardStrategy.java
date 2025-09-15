package com.rewardplatform.ad.service;

import com.rewardplatform.ticket.domain.TicketTransaction;
import com.rewardplatform.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 광고 시청 → 티켓 보상 전략
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketRewardStrategy implements AdRewardStrategy {

    private final TicketService ticketService;
    private static final int AD_WATCH_TICKET_REWARD = 3;

    @Override
    public void applyReward(Long userId, String adPlatform) {
        ticketService.earnTickets(userId, AD_WATCH_TICKET_REWARD,
                TicketTransaction.SourceType.AD,
                "Ad watch reward (" + adPlatform + ")");
        log.info("Ad ticket reward applied - userId: {}, tickets: {}, platform: {}",
                userId, AD_WATCH_TICKET_REWARD, adPlatform);
    }

    @Override
    public String getRewardType() {
        return "TICKET";
    }
}
