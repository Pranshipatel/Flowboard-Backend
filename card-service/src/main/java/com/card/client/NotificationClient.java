package com.card.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.card.client.dto.NotifyDueDateRequest;
import com.card.client.dto.NotifyOverdueRequest;

@FeignClient(
        name = "NOTIFICATION-SERVICE",
        path = "/api/v1/notifications"
)
public interface NotificationClient {

    @PostMapping("/notify/due-date-body")
    void notifyDueDate(@RequestBody NotifyDueDateRequest request);

    @PostMapping("/notify/overdue-body")
    void notifyOverdue(@RequestBody NotifyOverdueRequest request);

}