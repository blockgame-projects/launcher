package com.james090500.BlockGameLauncher.libs;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.james090500.BlockGameLauncher.Main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BlockGameDownloader {
    private static final String OWNER_REPO = "james090500/blockgame-client";
    private static final String API_URL    = "https://api.github.com/repos/" + OWNER_REPO + "/releases";
    private static final HttpClient HTTP   = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private static final Gson GSON = new Gson();

    /** Return all tag names, newest → oldest (by published_at). */
    public static List<String> listTagsNewestFirst(Path runDir) {
        List<String> tags = new ArrayList<>();
        List<JsonObject> releases = fetchAllReleases();

        if(releases.isEmpty()) {
            Pattern pattern = Pattern.compile("^blockgame-client-(.+)\\.jar$");

            File[] files = runDir.toFile().listFiles((d, name) -> name.endsWith(".jar"));
            if (files != null) {
                for (File f : files) {
                    Matcher m = pattern.matcher(f.getName());
                    if (m.matches()) {
                        tags.add(m.group(1)); // xxx part
                    }
                }
            }
            return tags;
        } else {
            releases.sort((a, b) -> b.get("published_at").getAsString()
                    .compareTo(a.get("published_at").getAsString()));
            for (JsonObject r : releases) tags.add(r.get("tag_name").getAsString());
            return tags;
        }
    }

    /**
     * Download an asset from a specific tag.
     * @param tagName exact tag_name (e.g., "v0.1.0")
     * @param outDir directory to save to
     * @return Path to downloaded file
     */
    public static Path downloadAssetFromTag(String tagName, Path outDir) {
        if(tagName.equalsIgnoreCase("latest")) {
            tagName = Main.versions.getFirst();
        }

        String name = "blockgame-client-" + tagName + ".jar";
        Path out = outDir.resolve(name);

        System.out.println(out.toFile());
        if(out.toFile().exists()) {
            return out;
        }

        List<JsonObject> releases = fetchAllReleases();
        JsonObject target = null;

        for (JsonObject r : releases) {
            if (tagName.equals(r.get("tag_name").getAsString())) {
                target = r;
                break;
            }
        }
        if (target == null) throw new IllegalArgumentException("Tag not found: " + tagName);

        JsonArray assets = target.getAsJsonArray("assets");
        if (assets.size() == 0) throw new IllegalStateException("No assets for tag: " + tagName);

        JsonObject chosen = assets.get(0).getAsJsonObject();


        String url  = chosen.get("browser_download_url").getAsString();

        try { Files.createDirectories(outDir); } catch (IOException e) { throw new UncheckedIOException(e); }
        downloadTo(url, out);
        return out;
    }

    /* ---------------- internals ---------------- */

    private static List<JsonObject> fetchAllReleases() {
        List<JsonObject> releases = new ArrayList<>();
        int page = 1;
        while (true) {
            String url = API_URL + "?per_page=100&page=" + page;
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "BlockGameLauncher/1.0")
                    .GET().build();
            try {
                HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) {
                    throw new IOException("GitHub API HTTP " + res.statusCode() + ": " + res.body());
                }
                JsonArray arr = GSON.fromJson(res.body(), JsonArray.class);
                if (arr.size() == 0) break; // no more pages
                for (JsonElement e : arr) releases.add(e.getAsJsonObject());
                page++;
            } catch (IOException | InterruptedException e) {
                Main.launcher.setCurrentTask("Rate Limit Exceeded, using cache");
                return releases;
            }
        }
        return releases;
    }

    private static void downloadTo(String url, Path out) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(120))
                    .header("User-Agent", "BlockGameLauncher/1.0")
                    .GET().build();
            HttpResponse<InputStream> res = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() != 200) throw new IOException("HTTP " + res.statusCode() + " for " + url);
            try (InputStream in = res.body()) {
                Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("Downloaded: " + out.toAbsolutePath());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("Download failed: " + url, e);
        }
    }
}