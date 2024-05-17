package org.cloud.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cloud.common.FileCutType;

@Slf4j
@Getter
public class FileCutMessage implements CloudMessage {
    private final String fileName;
    private final int cutSize;
    private final long cutNumber;
    private final double transferredCutsWeight;
    private final byte[] fileBytes;
    private final FileCutType cutType;

    public FileCutMessage(String fileName, int cutSize, byte[] fileBytes, long cutNumber,
                          long numberOfCuts) {
        this.fileName = fileName;
        this.cutSize = cutSize;
        this.fileBytes = fileBytes;
        this.cutNumber = cutNumber;
        cutType = defineCutType(cutNumber, numberOfCuts);
        transferredCutsWeight = 1.0 / numberOfCuts * cutNumber;
        log.debug("{} file cut #{} / {} from {} created", cutType, cutNumber,
                numberOfCuts, fileName);
    }

    @Override
    public MessageType getType() {
        return MessageType.FILE_CUT;
    }

    private FileCutType defineCutType(long cutNumber, long numberOfCuts) {
        if (numberOfCuts == 1) {
            return FileCutType.SINGLE;
        } else if (cutNumber == 1) {
            return FileCutType.FIRST;
        } else if (cutNumber == numberOfCuts) {
            return FileCutType.LAST;
        } else {
            return FileCutType.COMMON;
        }
    }
}
