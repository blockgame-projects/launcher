package com.james090500.BlockGameLauncher.libs;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.james090500.BlockGameLauncher.Main;

public class AssetDownloader {

    private static String getAssetUrl() {
        String arch = Main.arch;
        String os = Main.os;

        boolean arm = arch.contains("aarch64") || arch.contains("arm64");
        boolean x64 = arch.contains("amd64") || arch.contains("x86_64");

        String finalPlatform = "unknown";
        String fianlArch = arm ? "arm64" : x64 ? "x64" : "x86";

        if (os.contains("win")) {
            finalPlatform = "windows";
        } else if (os.contains("mac")) {
            finalPlatform = "macos";
        } else if (os.contains("nux") || os.contains("nix")) {
            finalPlatform = "linux";
        }

        return "https://blockgame.james090500.com/api/assets?platform=" + finalPlatform + "&arch=" + fianlArch;
    }

    /**
     * Downloads every file listed in the JSON at jsonUrl into destDir.
     *

     * @throws IOException
     * @throws InterruptedException
     */
    public static void fetch(Path destDir) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();

        // Fetch JSON
        HttpRequest req = HttpRequest.newBuilder(URI.create(getAssetUrl())).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Failed to fetch JSON: HTTP " + resp.statusCode());
        }
        String body = resp.body();

        // Ensure destination exists
        if (!Files.exists(destDir)) {
            Files.createDirectories(destDir);
        }

        // Parse JSON using Gson (lenient reader to tolerate unquoted keys / minor deviations)
        Gson gson = new Gson();
        Type listType = new TypeToken<List<LibFile>>() {}.getType();
        JsonReader reader = new JsonReader(new StringReader(body));
        reader.setLenient(true); // allow slightly malformed JSON (like unquoted keys)
        List<LibFile> files = gson.fromJson(reader, listType);

        if (files == null || files.isEmpty()) {
            System.out.println("No files found in JSON.");
            return;
        }

        for (LibFile f : files) {
            if (f == null || f.name == null || f.url == null) {
                System.err.println("Skipping malformed entry: " + f);
                continue;
            }

            Path out = destDir.resolve(f.name);
            if(!out.toFile().exists()) {
                System.out.println("Downloading: " + f.name + " <- " + f.url);
                try {
                    HttpRequest fileReq = HttpRequest.newBuilder(URI.create(f.url)).GET().build();
                    HttpResponse<InputStream> fileResp = client.send(fileReq, HttpResponse.BodyHandlers.ofInputStream());
                    if (fileResp.statusCode() == 200) {
                        try (InputStream in = fileResp.body()) {
                            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                        }
                        System.out.println("Saved: " + out);
                    } else {
                        System.err.println("Failed to download " + f.url + " : HTTP " + fileResp.statusCode());
                    }
                } catch (Exception e) {
                    System.err.println("Error downloading " + f.url + " -> " + e.getMessage());
                }
            } else {
                System.out.println("File in cache: " + f.name);
            }
        }
    }

    static class LibFile {
        String name;
        String url;
        @Override public String toString() { return "LibFile{name='" + name + "', url='" + url + "'}"; }
    }
}
