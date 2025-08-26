package az.inci.bmsanbar.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import lombok.Getter;

public class Logger {
    private final Context context;
    private final DateFormat dateFormat;
    @Getter
    private File file;

    public Logger(Context context) {
        this.context = context;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        createLogFile();
    }

    private void createLogFile() {
        String logFolder = Objects.requireNonNull(context.getExternalFilesDir("/")).getPath() + "/logs";
        File logDir = new File(logFolder);
        boolean logFolderExists = logDir.exists();
        if (!logFolderExists) {
            logFolderExists = logDir.mkdirs();
        }

        if (!logFolderExists)
            logFolder = logFolder.replace("/logs", "");

        String filePath = logFolder + "/ZLog.txt";
        file = new File(filePath);
        if (!file.exists()) {
            try {
                boolean newFile = file.createNewFile();
                if (newFile) {
                    Log.i("ZLogger", "Log file created successfully.");
                } else {
                    Log.e("ZLogger", "Failed to create log file.");
                }
            } catch (Exception e) {
                Log.e("ZLogger", "Error creating log file: " + e);
            }
        } else if (file.length() > 1024 * 1024 * 10) {
            try {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                File newFile = new File(filePath.replace(".txt", "_" + timestamp + ".txt"));
                if (file.renameTo(newFile)) {
                    boolean newFileCreated = file.createNewFile();
                    if (newFileCreated) {
                        Log.i("ZLogger", "Log file created successfully.");
                        try (FileWriter fileWriter = new FileWriter(file, true)) {
                            fileWriter.write("== Log started at " + timestamp + " ==\n");
                            fileWriter.flush();
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("ZLogger", "Error creating log file: " + e);
            }
        }
    }

    /**
     * @noinspection unused
     */
    public void logError(String message) {
        Log.e(context.getClass().getSimpleName(), message);
        writeToFile(message, "ERROR");
    }

    /**
     * @noinspection unused
     */
    public void logInfo(String message) {
        Log.i("ZLogger", message);
        writeToFile(message, "INFO");
    }

    /**
     * @noinspection unused
     */
    public void logWarning(String message) {
        Log.w("ZLogger", message);
        writeToFile(message, "WARNING");
    }

    /**
     * @noinspection unused
     */
    public void logDebug(String message) {
        Log.d("ZLogger", message);
        writeToFile(message, "DEBUG");
    }

    public void writeToFile(String message, String level) {
        if (file.exists()) {
            try (FileWriter fileWriter = new FileWriter(file, true)) {
                fileWriter.append("[").append(dateFormat.format(new Date()))
                        .append("] - ")
                        .append(context.getClass().getSimpleName())
                        .append(": ")
                        .append(level)
                        .append(": ")
                        .append(message)
                        .append("\n");
                fileWriter.flush();
            } catch (Exception e) {
                Log.e("ZLogger", "Error writing to log file: " + e.toString());
            }
        }
    }
}
