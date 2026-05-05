package com.flowboard.workspace.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendNotificationRequest {

    private Long recipientId;
    private Long actorId;
    private String type;
    private String title;
    private String message;
    private Long relatedId;
    private String relatedType;
    private String deepLinkUrl;
    private boolean sendEmail;
    private String recipientEmail;

}
