package vn.gastroai.be.infrastructure.backup;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessExecutorTest {

    private final ProcessExecutor executor = new ProcessExecutor();

    private ProcessBuilder helperProcessBuilder(String... helperArgs) {
        String javaBin = ProcessHandle.current().info().command()
                .orElseThrow(() -> new IllegalStateException("Khong xac dinh duoc duong dan java hien tai"));

        String classpath;
        try {
            classpath = Path.of(
                    Helper.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toString();
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException(e);
        }

        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-cp");
        command.add(classpath);
        command.add(Helper.class.getName());
        command.addAll(List.of(helperArgs));

        return new ProcessBuilder(command);
    }

    @Test
    void floodingStdoutAndStderrDoesNotHangBecauseStdoutIsDiscarded() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () ->
                executor.run(helperProcessBuilder("flood"), "flood test", null));
    }

    @Test
    void processExitingWithNonZeroCodeThrowsWithExitCodeAndStderr() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                executor.run(helperProcessBuilder("fail"), "fail test", null));

        assertTrue(exception.getMessage().contains("exit code 3"));
        assertTrue(exception.getMessage().contains("gia lap loi"));
    }

    @Test
    void stdinIsWrittenToChildProcessCorrectly() {
        byte[] stdin = "noi dung sql gia".getBytes(StandardCharsets.UTF_8);

        assertDoesNotThrow(() ->
                executor.run(helperProcessBuilder("expect-stdin", "noi dung sql gia"), "stdin test", stdin));
    }

    @Test
    void startingNonExistentProgramThrowsClearError() {
        ProcessBuilder processBuilder = new ProcessBuilder("khong-ton-tai-chuong-trinh-nay-xyz");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                executor.run(processBuilder, "ghost program", null));

        assertTrue(exception.getMessage().contains("ghost program"));
    }

    /** Tien trinh con gia - chay duoc tren ca Windows/Linux vi dung chinh JVM hien tai. */
    static class Helper {

        public static void main(String[] args) throws IOException {
            if (args.length == 0) {
                System.err.println("Thieu mode: flood | fail | expect-stdin");
                System.exit(2);
                return;
            }

            switch (args[0]) {
                case "flood" -> flood();
                case "fail" -> fail();
                case "expect-stdin" -> expectStdin(args.length > 1 ? args[1] : "");
                default -> {
                    System.err.println("Mode khong hop le: " + args[0]);
                    System.exit(2);
                }
            }
        }

        private static void flood() {
            String line = "x".repeat(200) + System.lineSeparator();
            for (int i = 0; i < 20_000; i++) {
                System.out.print(line);
                System.err.print(line);
            }
        }

        private static void fail() {
            System.err.println("gia lap loi: cot khong ton tai");
            System.exit(3);
        }

        private static void expectStdin(String expected) throws IOException {
            String actual = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
            if (actual.equals(expected)) {
                System.out.println("OK");
                System.exit(0);
            } else {
                System.err.println("stdin khong khop. Mong doi=[" + expected + "] Thuc te=[" + actual + "]");
                System.exit(4);
            }
        }
    }
}