package az.inci.wmsclient.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import az.inci.wmsclient.R;
import az.inci.wmsclient.util.Logger;

public class LogViewActivity extends AppBaseActivity {
    File currentLogFile;
    File[] logFiles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_view);
        setEdgeToEdge();

        currentLogFile = new Logger(this).getFile();
        logFiles = getLogFiles();
        displayLog();

        Button shareButton = findViewById(R.id.shareButton);
        shareButton.setOnClickListener(v -> shareLogFile());

        displayLogSize();

        Button clearLogs = findViewById(R.id.clearLogs);
        clearLogs.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Logları Sil")
                    .setMessage("Loqları silmək istəyirsinizmi")
                    .setPositiveButton("Bəli", (dialog, which) -> clearLogFiles())
                    .setNegativeButton("Xeyr", null)
                    .create();
            builder.show();
        });
    }

    private void displayLog() {
        TextView logArea = findViewById(R.id.logArea);
        try (BufferedReader reader = new BufferedReader(new FileReader(currentLogFile))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            Collections.reverse(lines);
            StringBuilder sb = new StringBuilder();
            for (String newLine : lines) {
                sb.append(newLine).append("\n\n");
            }
            logArea.setText(sb.toString());
        } catch (Exception e) {
            logger.logError(e.toString());
        }
    }

    private void displayLogSize() {
        TextView logSizeTextView = findViewById(R.id.logSize);
        double logSize = 0;
        if (logFiles != null) {
            for (File f : logFiles) {
                if (f.getName().startsWith("ZLog") && f.getName().endsWith(".txt")) {
                    logSize += f.length();
                }
            }
        }
        logSizeTextView.setText(String.format(Locale.getDefault(), "Log həcmi: %.2f KB", logSize / 1024.0));
    }

    private void shareLogFile() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, getPackageName() + ".provider", currentLogFile));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Log File");
        startActivity(Intent.createChooser(intent, "Share Log File"));
    }

    private void clearLogFiles() {
        for (File file : logFiles) {
            if (file.getName().startsWith("ZLog_") && file.getName().endsWith(".txt")) {
                if (!file.delete())
                    logger.logError("Log file could not be deleted: " + file.getAbsolutePath());
            }
        }

        try (FileWriter writer = new FileWriter(currentLogFile, false)) {
            writer.write("");
            writer.flush();
        } catch (IOException e) {
            logger.logError(e.toString());
        }
        displayLog();
        displayLogSize();
    }

    private File[] getLogFiles() {
        File logFolder = currentLogFile.getParentFile();
        if (logFolder != null && logFolder.isDirectory()) {
            return logFolder.listFiles();
        }
        return new File[0];
    }
}