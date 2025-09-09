package com.james090500.BlockGameLauncher;

import com.james090500.BlockGameLauncher.libs.AssetDownloader;
import com.james090500.BlockGameLauncher.libs.ClientDownloader;
import javafx.application.Application;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    static Path runDir = Paths.get("blockgame");
    static Path libDir = runDir.resolve("libs");

    public static String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    public static String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

    public static Launcher launcher = new Launcher();

    /**
     * Entry point
     * @param args
     */
    public static void main(String[] args) {
        Application.launch(Launcher.class);
    }

    public static void play() {
        launcher.setCurrentTask("Downloading Libs...");

        // Create lib dir
        try {
            Files.createDirectories(libDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Download Libs
        try {
            AssetDownloader.fetch(libDir);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Download Client
        Path blockGame;
        try {
            blockGame = ClientDownloader.fetch(runDir);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        launcher.setCurrentTask("Launching...");
        try {
            launchGame(blockGame, runDir, libDir);

            // Close launcher
            launcher.setCurrentTask("Game Launched. Exiting...");
            System.exit(0);
        } catch (Exception e) {
            launcher.setCurrentTask("Something went wrong: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Launch the game with all libs
     * @param runDir Run Dir
     * @param libDir Lib Dir
     */
    private static void launchGame(Path blockGame, Path runDir, Path libDir) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");

        //If MacOS ARM
        boolean arm = arch.contains("aarch64") || arch.contains("arm64");
        if (os.contains("mac") && arm) {
            cmd.add("-XstartOnFirstThread");
        }

        cmd.add("-cp");

        // Build classpath: main jar + all jars in libs/
        StringBuilder cp = new StringBuilder();
        cp.append(blockGame.toFile().getAbsolutePath());
        for (File f : Objects.requireNonNull(libDir.toFile().listFiles((d, name) -> name.endsWith(".jar")))) {
            cp.append(File.pathSeparator).append(f.getAbsolutePath());
        }
        cmd.add(cp.toString());

        cmd.add("com.james090500.Main"); // Replace with your main class name

        System.out.println(cmd);

        // Launch process
        new ProcessBuilder(cmd)
                .inheritIO()
                .directory(runDir.toFile())
                .start();
    }
}