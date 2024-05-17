package org.cloud.model;

public class AcceptMessage implements CloudMessage {

    @Override
    public MessageType getType() {
        return MessageType.ACCEPT;
    }
}
