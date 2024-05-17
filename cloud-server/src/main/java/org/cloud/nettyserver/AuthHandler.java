package org.cloud.nettyserver;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.cloud.common.FieldVerificationException;
import org.cloud.common.FieldVerifier;
import org.cloud.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class AuthHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private final AuthHandler authHandler = this;
    private final Path serverDir = Path.of("server_files");

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("client on channel {} from {} connected", ctx.channel().id(), ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Error: ", cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.debug("client on channel {} from {} disconnected", ctx.channel().id(), ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) {
        log.debug("Auth Handler Received: {}", cloudMessage.getType());
        switch (cloudMessage) {
            case LoginMessage loginMessage -> handleAuthorization(ctx, loginMessage);
            case SignupMessage signupMessage -> handleRegistration(ctx, signupMessage);
            default -> {
                log.error("Received a message of an unexpected type: {}", cloudMessage.getType());
                ctx.close();
            }
        }
    }

    private void handleAuthorization(ChannelHandlerContext ctx, LoginMessage loginMessage) {
        String login = loginMessage.getUserName();
        try {
            if (Authenticator.loginMatchesPass(login, loginMessage.getPassword())) {
                handleSuccessfulCheck(ctx, login);
            } else {
                ctx.writeAndFlush(new DenyMessage("Invalid user name or password"));
            }
        } catch (IOException e) {
            handleDirCreationError(ctx, login, e);
        }
    }

    private void handleRegistration(ChannelHandlerContext ctx, SignupMessage signupMessage) {
        String login = signupMessage.getUserName();
        String password = signupMessage.getPassword();
        try {
            FieldVerifier.verifyCredentials(login, password);
            if (Authenticator.checkUser(login)) {
                ctx.writeAndFlush(new ErrorMessage("User " + login + " already exists"));
            } else if (Authenticator.addNewUser(login, password)) {
                handleSuccessfulCheck(ctx, login);
            } else {
                ctx.writeAndFlush(new ErrorMessage("Failed to create a user"));
            }
        } catch (FieldVerificationException e) {
            ctx.writeAndFlush(new ErrorMessage(e.getMessage()));
        } catch (IOException e) {
            handleDirCreationError(ctx, login, e);
        }
    }

    private void handleSuccessfulCheck(ChannelHandlerContext ctx, String login) throws IOException {
        Path userDir = checkDirPresence(login);
        ChannelFuture future = ctx.writeAndFlush(new AcceptMessage());
        replaceThisHandler(future, ctx, userDir);
    }

    private void replaceThisHandler(ChannelFuture future, ChannelHandlerContext ctx, Path userDir) {
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                ctx.pipeline().addLast(new FileHandler(userDir, ctx));
                log.debug("new FileHandler created");
                ctx.pipeline().remove(authHandler);
                log.debug("AuthHandler removed");
            }
        });
    }

    private Path checkDirPresence(String login) throws IOException {
        Path userDir = serverDir.resolve(login);
        if (!Files.isDirectory(userDir)) {
            Files.createDirectory(userDir);
        }
        return userDir;
    }

    private static void handleDirCreationError(ChannelHandlerContext ctx, String login, IOException e) {
        log.error("Failed to create directory for {}", login);
        e.printStackTrace();
        ctx.writeAndFlush(new ErrorMessage("An error occurred on the server"));
    }
}
