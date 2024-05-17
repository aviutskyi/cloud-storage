package org.cloud.model;

import lombok.Getter;

@Getter
public class LoginMessage implements CloudMessage {
    private final String userName;
    private final String password;

    public LoginMessage(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    @Override
    public MessageType getType() {
        return MessageType.LOGIN;
    }
}
