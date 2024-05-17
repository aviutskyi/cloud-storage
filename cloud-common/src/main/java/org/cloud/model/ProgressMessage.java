package org.cloud.model;

import lombok.Getter;

@Getter
public class ProgressMessage implements CloudMessage {

    private final double percentage;

    public ProgressMessage(double percentage) {
        this.percentage = percentage;
    }

    @Override
    public MessageType getType() {
        return MessageType.PROGRESS;
    }
}
