package com.james090500.BlockGameLauncher.libs;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.james090500.BlockGameLauncher.Main;

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
import java.time.Duration;
import java.util.List;

public class ClientDownloader {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final Gson GSON = new Gson();

    // Defaults (you can override by calling methods with other URLs/paths)
    public static final String LATEST_JSON_URL = "https://assets.blockgame.james090500.com/builds/latest.json";
    public static final String BASE_BUILD_URL = "https://assets.blockgame.james090500.com/builds/";

    /**
     * Fetches latest.json and returns the version string (e.g. "0.0.1").
     */
    public static String fetchLatestVersion() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(LATEST_JSON_URL))
                .GET()
                .build();

        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Failed to fetch latest.json: HTTP " + resp.statusCode());
        }

        String body = resp.body();
        JsonReader reader = new JsonReader(new StringReader(body));
        reader.setLenient(true); // tolerate unquoted keys etc.
        JsonObject obj = GSON.fromJson(reader, JsonObject.class);

        if (obj == null || !obj.has("version")) {
            throw new IOException("latest.json did not contain a 'version' field. Body: " + body);
        }

        return obj.get("version").getAsString();
    }

    /**
     * Downloads blockgame-client-<version>.jar from the BASE_BUILD_URL into destDir and returns path to saved file.
     */
    public static Path fetch(Path destDir) throws IOException, InterruptedException {
        String version = fetchLatestVersion();

        String fileName = "blockgame-client-" + version + ".jar";
        String fileUrl = BASE_BUILD_URL + fileName;

        // Ensure destination directory exists
        if (!Files.exists(destDir)) {
            Files.createDirectories(destDir);
        }

        HttpRequest req = HttpRequest.newBuilder(URI.create(fileUrl))
                .GET()
                .build();

        HttpResponse<InputStream> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new IOException("Failed to download file: " + fileUrl + " -> HTTP " + resp.statusCode());
        }

        Path out = destDir.resolve(fileName);
        if(!out.toFile().exists()) {
            try (InputStream in = resp.body()) {
                Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        return out;
    }
}
