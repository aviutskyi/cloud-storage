package org.cloud.model;

import lombok.Getter;

@Getter
public class SignupMessage implements CloudMessage {
    private final String userName;
    private final String password;

    public SignupMessage(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    @Override
    public MessageType getType() {
        return MessageType.SIGNUP;
    }
}
