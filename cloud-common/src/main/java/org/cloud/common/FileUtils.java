package org.cloud.common;

import lombok.extern.slf4j.Slf4j;
import org.cloud.model.FileCutMessage;

import java.io.*;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FileUtils {
    private static final int CUT_SIZE = 1024*1000;

    public static void sendFileToNetwork(String srcDirectory, String fileName, NetworkHandler networkHandler) {
        File file = new File(srcDirectory + "/" + fileName);
        if (file.isFile()) {
            try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                long fileSize = file.length();
                if (fileSize < 1) {
                    throw new RuntimeException("File is empty");
                }
                byte[] bytes = new byte[CUT_SIZE];
                long numberOfCuts = (fileSize + CUT_SIZE - 1) / CUT_SIZE;
                for (int i = 1; i <= numberOfCuts; i++) {
                    int bytesRead = in.read(bytes);
                    networkHandler.writeToNetwork(new FileCutMessage(fileName, bytesRead, bytes, i,
                                numberOfCuts));
                }
                log.debug("File '{}' sent", fileName);
            } catch (IOException | RuntimeException e) {
                log.error("File transfer interrupted! Error: {}", e.toString());
            }
        } else {
            log.debug("Unable to transfer a directory");
        }
    }

    public static List<String> getFilesFromDir(String directory, NetworkHandler handler) {
        File dir = new File(directory);
        if (dir.isDirectory()) {
            File[] content = dir.listFiles();
            if (content != null) {
                List<String> directories = new ArrayList<>();
                List<String> files = new ArrayList<>();
                for (File file : content) {
                    if (file.isDirectory()) {
                        directories.add(file.getName());
                    } else {
                        files.add(file.getName());
                    }
                }
                directories.sort(Collator.getInstance());
                files.sort(Collator.getInstance());
                List<String> list = new ArrayList<>();
                handler.addUpwardNavigation(list);
                list.addAll(directories);
                list.addAll(files);
                return list;
            }
        }
        return List.of();
    }

    public static void handleFileCut(FileCutMessage fileCut, NetworkHandler handler) {
        switch (fileCut.getCutType()) {
            case FIRST, COMMON:
                handler.getFileFlow().writeCut(fileCut);
                handler.setProgress(fileCut.getTransferredCutsWeight());
                break;
            case LAST, SINGLE:
                handler.getFileFlow().writeLastCut(fileCut);
                handler.updateCurrentDirContent();
                handler.setProgress(0);
                handler.onGettingLastFileCut();
                break;
            default:
                log.error("Unknown CutType received");
                break;
        }
    }
}
