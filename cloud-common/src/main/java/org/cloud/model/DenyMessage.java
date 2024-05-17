package org.cloud.model;

import lombok.Getter;

@Getter
public class DenyMessage implements CloudMessage {

    private final String reason;

    public DenyMessage(String reason) {
        this.reason = reason;
    }

    @Override
    public MessageType getType() {
        return MessageType.DENY;
    }
}
