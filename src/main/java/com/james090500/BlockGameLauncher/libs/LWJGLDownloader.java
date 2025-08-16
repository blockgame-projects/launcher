package com.james090500.BlockGameLauncher.libs;

import com.james090500.BlockGameLauncher.Main;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public final class LWJGLDownloader {
    public static final String LWJGL_VERSION = "3.3.6";

    private record Platform(String classifier, List<String> fallbacks) { }

    private static Platform detectPlatform() {
        String arch = Main.arch;
        String os = Main.os;

        boolean arm = arch.contains("aarch64") || arch.contains("arm64");
        boolean x64 = arch.contains("amd64") || arch.contains("x86_64");

        if (os.contains("win")) {
            if (arm) return new Platform("natives-windows-arm64", List.of("natives-windows"));
            if (x64) return new Platform("natives-windows", List.of());
        } else if (os.contains("mac")) {
            if (arm) return new Platform("natives-macos-arm64", List.of("natives-macos"));
            if (x64) return new Platform("natives-macos", List.of());
        } else if (os.contains("nux") || os.contains("nix")) {
            if (arm) return new Platform("natives-linux-arm64", List.of("natives-linux"));
            if (x64) return new Platform("natives-linux", List.of());
        }
        throw new IllegalStateException("Unsupported OS/arch: " + os + " / " + arch);
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public static void fetch(Set<String> features, Path outDir) {
        Objects.requireNonNull(features);
        Objects.requireNonNull(outDir);

        Platform platform = detectPlatform();

        for (String artifact : features) {
            // regular jar
            String jarUrl = String.format(
                    "https://repo1.maven.org/maven2/org/lwjgl/%s/%s/%s-%s.jar",
                    artifact, LWJGL_VERSION, artifact, LWJGL_VERSION
            );
            LibDownloader.downloadUrl(jarUrl, outDir);

            // natives jar (try primary classifier, then fallbacks)
            List<String> classifiers = new ArrayList<>();
            classifiers.add(platform.classifier());
            classifiers.addAll(platform.fallbacks());

            boolean gotNatives = false;
            for (String cls : classifiers) {
                String nUrl = String.format(
                        "https://repo1.maven.org/maven2/org/lwjgl/%s/%s/%s-%s-%s.jar",
                        artifact, LWJGL_VERSION, artifact, LWJGL_VERSION, cls
                );
                if (LibDownloader.downloadAllow404(nUrl, outDir)) {
                    gotNatives = true;
                    break;
                }
            }
            if (!gotNatives) {
                System.err.println("Warning: no natives jar for " + artifact + " on " + platform.classifier());
            }
        }
    }
}