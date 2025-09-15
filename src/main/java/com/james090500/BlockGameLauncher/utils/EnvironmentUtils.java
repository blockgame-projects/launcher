package com.james090500.BlockGameLauncher.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class EnvironmentUtils {
    public static final Path runDir = Paths.get("blockgame");
    public static final Path libDir = runDir.resolve("libs");

    public static final String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    public static final String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
}
