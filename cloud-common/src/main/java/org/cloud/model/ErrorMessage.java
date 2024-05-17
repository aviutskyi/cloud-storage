package org.cloud.model;

import lombok.Getter;

@Getter
public class ErrorMessage implements CloudMessage {
    private final String issue;

    public ErrorMessage(String issue) {
        this.issue = issue;
    }

    @Override
    public MessageType getType() {
        return MessageType.ERROR;
    }
}
