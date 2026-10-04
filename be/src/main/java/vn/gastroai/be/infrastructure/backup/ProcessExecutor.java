package vn.gastroai.be.infrastructure.backup;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class ProcessExecutor {

    private static final int MAX_STDERR_CHARS = 2000;

    public void run(ProcessBuilder processBuilder, String name, byte[] stdin) {
        // Bo han stdout ngay tu he dieu hanh - tranh treo khi tien trinh con in nhieu
        // (vd psql -f) ma khong ai doc.
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        processBuilder.redirectErrorStream(false);

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new IllegalStateException(name + " khong khoi dong duoc: " + e.getMessage(), e);
        }

        StringBuilder stderr = new StringBuilder();
        Thread errorReader = new Thread(() -> {
            try {
                stderr.append(new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException ignored) {
            }
        });
        errorReader.start();

        if (stdin != null) {
            try {
                process.getOutputStream().write(stdin);
            } catch (IOException ignored) {
            } finally {
                try {
                    process.getOutputStream().close();
                } catch (IOException ignored) {
                }
            }
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
            errorReader.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(name + " bi ngat giua chung", e);
        }

        if (exitCode != 0) {
            throw new IllegalStateException(
                    name + " loi, exit code " + exitCode + ": " + truncate(stderr.toString()));
        }
    }

    private String truncate(String text) {
        if (text.length() <= MAX_STDERR_CHARS) {
            return text;
        }
        return text.substring(0, MAX_STDERR_CHARS) + "... (da cat bot)";
    }
}