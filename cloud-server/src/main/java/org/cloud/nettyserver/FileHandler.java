package org.cloud.nettyserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.cloud.common.FileFlow;
import org.cloud.common.FileUtils;
import org.cloud.common.NetworkHandler;
import org.cloud.model.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class FileHandler extends SimpleChannelInboundHandler<CloudMessage> implements NetworkHandler {

    private final Path userTopDir;
    private Path currentDir;

    public FileFlow fileFlow;

    private ChannelHandlerContext ctx;

    public FileHandler(Path userDir, ChannelHandlerContext ctx) {
        this.ctx = ctx;
        userTopDir = userDir;
        currentDir = userTopDir;
        updateCurrentDirContent();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Error: ", cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.debug("client on channel {} from {} disconnected", ctx.channel().id(), ctx.channel().remoteAddress());
        fileFlow.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) throws Exception {
        this.ctx = ctx;
        log.debug("File Handler Received: {}", cloudMessage.getType());
        switch (cloudMessage) {
            case FileRequest fileRequest:
                FileUtils.sendFileToNetwork(currentDir.toString(), fileRequest.getFileName(), this);
                break;
            case NavigationRequest navigationRequest:
                Path dir = currentDir.resolve(Path.of(navigationRequest.getDirName()));
                if (Files.isDirectory(dir)) {
                    currentDir = dir.normalize();
                    updateCurrentDirContent();
                }
                break;
            case FileCutMessage fileCut:
                if (fileFlow == null || !fileFlow.isReceiving()) {
                    fileFlow = new FileFlow(currentDir.toString(), fileCut.getFileName());
                }
                FileUtils.handleFileCut(fileCut, this);
                break;
            case RenameRequest renameRequest:
                Path file = currentDir.resolve(Path.of(renameRequest.getOldName()));
                Files.move(file, file.resolveSibling(renameRequest.getNewName()));
                writeToNetwork(new ListDirMessage(currentDir.toString(), this));
                break;
            case DeleteRequest deleteRequest:
                Path deletedFile = currentDir.resolve(Path.of(deleteRequest.getFileName()));
                Files.delete(deletedFile);
                updateCurrentDirContent();
                break;
            default:
                log.error("Unknown Message type received");
                break;
        }
    }

    @Override
    public <T extends CloudMessage> void writeToNetwork(T cloudMessage) {
        ctx.writeAndFlush(cloudMessage);
    }

    @Override
    public void addUpwardNavigation(List<String> list) {
        if (!currentDir.equals(userTopDir)) {
            list.add("..");
        }
    }

    @Override
    public void updateCurrentDirContent() {
        writeToNetwork(new ListDirMessage(currentDir.toString(), this));
    }

    @Override
    public FileFlow getFileFlow() {
        return fileFlow;
    }

    @Override
    public void setProgress(double percentage) {
        writeToNetwork(new ProgressMessage(percentage));
    }

    @Override
    public void onGettingLastFileCut() {

    }
}
