package com.james090500.BlockGameLauncher.libs;

import com.james090500.BlockGameLauncher.Main;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;


public class LibDownloader {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    /**
     * Downloads the file from the given URL into outDir.
     * Skips download if the file already exists.
     * @param url    Full URL to the file.
     * @param outDir Directory to save the file.
     * @return Path to the downloaded file.
     */
    public static Path downloadUrl(String url, Path outDir) {
        try {
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            Path outFile = outDir.resolve(fileName);

            if (Files.exists(outFile)) {
                return outFile;
            }

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<InputStream> res = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() != 200) {
                throw new IOException("HTTP " + res.statusCode() + " for " + url);
            }

            try (InputStream in = res.body()) {
                Files.copy(in, outFile, StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("Downloaded: " + outFile.getFileName());
            return outFile;

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to download " + url, e);
        }
    }

    /**
     * Download a file but allow a 404 as it may not exist (natives)
     * @param url The url to download
     * @param outDir The location to save to
     * @return
     */
    static boolean downloadAllow404(String url, Path outDir) {
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        Path outFile = outDir.resolve(fileName);

        if (Files.exists(outFile)) {
            return true;
        }

        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60)).GET().build();
            HttpResponse<InputStream> res = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() == 404) return false;
            if (res.statusCode() != 200) throw new IOException("HTTP " + res.statusCode());
            try (InputStream in = res.body()) {
                Files.copy(in, outFile, StandardCopyOption.REPLACE_EXISTING);
            }
            Main.launcher.setCurrentTask("Downloaded " + outFile.getFileName());
            return true;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
