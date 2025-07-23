package gg.nextforge.io;

import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BackupHandler {

    private static final Logger LOGGER = Logger.getLogger("BackupHandler");
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private final Path backupDirectory;

    public BackupHandler(Path backupDirectory) {
        this.backupDirectory = backupDirectory;
        FileService.ensureDirectoryExists(backupDirectory);
    }

    public boolean backupFile(Path originalFile) {
        if (!Files.exists(originalFile)) {
            LOGGER.warning("Cannot backup missing file: " + originalFile);
            return false;
        }

        String timestamp = TIMESTAMP_FORMAT.format(new Date());
        String filename = originalFile.getFileName().toString();
        String backupFilename = filename + "." + timestamp + ".bak";
        Path target = backupDirectory.resolve(backupFilename);

        try {
            Files.copy(originalFile, target, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Backup created: " + target);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to backup file: " + originalFile, e);
            return false;
        }
    }
}
