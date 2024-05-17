package org.cloud.common;

import org.cloud.model.CloudMessage;
import java.util.List;

public interface NetworkHandler {
    <T extends CloudMessage> void writeToNetwork(T cloudMessage) throws RuntimeException;

    void addUpwardNavigation(List<String> list);

    void updateCurrentDirContent();

    FileFlow getFileFlow();

    void setProgress(double percentage);

    void onGettingLastFileCut();
}
