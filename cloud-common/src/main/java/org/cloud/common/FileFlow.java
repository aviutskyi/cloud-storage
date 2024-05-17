package org.cloud.common;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cloud.model.FileCutMessage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Getter
public class FileFlow implements Closeable {
    private final File tempFile;
    private final String fileName;
    private boolean receiving;
    private OutputStream out;
    private FileOutputStream fos;
    private final String EXTENSION = ".tmp";

    public FileFlow(String currentDir, String fileName) {
        this.fileName = fileName;
        tempFile = new File(currentDir + "/" + this.fileName + EXTENSION);
        try {
            fos = new FileOutputStream(tempFile, true);
            out = new BufferedOutputStream(fos);
            log.debug("Writing streams opened");
            receiving = true;
        } catch (FileNotFoundException e) {
            log.error("Unable to create a file", e);
        }
    }

    public void writeCut(FileCutMessage fileCut) {
        try {
            out.write(fileCut.getFileBytes(), 0, fileCut.getCutSize());
            out.flush();
            log.debug("{} file cut #{} from {} written", fileCut.getCutType(), fileCut.getCutNumber(), fileCut.getFileName());
        } catch (IOException e) {
            log.error("File writing is interrupted", e);
            close();
        }
    }

    public void writeLastCut(FileCutMessage fileCut) {
        writeCut(fileCut);
        Path file = tempFile.toPath();
        try {
            Files.move(file, file.resolveSibling(fileName), StandardCopyOption.REPLACE_EXISTING);
            log.debug("FILE '{}' WRITTEN", fileCut.getFileName());
        } catch (IOException e) {
            log.error("Unable to rename temp file {} to {}", tempFile, fileName);
        }
        receiving = false;
        close();
    }

    @Override
    public void close() {
        try {
            fos.close();
            out.close();
            log.debug("Writing streams closed");
        } catch (IOException e) {
            log.error("Unable to close writing streams", e);
        }
        if (receiving) {
            log.error("Download of {} has been interrupted", fileName);
            var d = tempFile.delete();
            if (!d) {
                log.error("Failed to delete a temporary file {}", tempFile);
            } else {
                log.debug("TempFile deleted");
            }
        }
        receiving = false;
    }
}
