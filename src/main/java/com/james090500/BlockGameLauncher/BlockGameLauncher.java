package com.james090500.BlockGameLauncher;

import com.james090500.BlockGameLauncher.libs.AssetDownloader;
import com.james090500.BlockGameLauncher.libs.ClientDownloader;
import com.james090500.BlockGameLauncher.utils.EnvironmentUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class BlockGameLauncher extends Application {

    public static BlockGameLauncher instance;

    private double baseWidth = 854;
    private double baseHeight = 480;

    private final Group currentTask = this.createText("Click play to begin", 24);

    @Override
    public void start(Stage stage) {
        // Set instance
        instance = this;

        stage.setTitle("BlockGame Launcher");
        stage.setResizable(false);

        Group root = new Group();

        WebView webView = new WebView();
        final WebEngine webEngine = webView.getEngine();
        webEngine.load("https://blockgame.james090500.com/changelog");
        webView.setPrefWidth(baseWidth);
        webView.setPrefHeight(baseHeight - 125);
        root.getChildren().add(webView);

        StackPane bottomBar = new StackPane();
        bottomBar.setPrefWidth(baseWidth);
        bottomBar.setPrefHeight(125);
        bottomBar.setTranslateY(baseHeight - 125);

        // Add the background
        Image imgBackground = loadImage("/gui/background.png");
        BackgroundSize bgSize = new BackgroundSize(imgBackground.getWidth() * 3,imgBackground.getHeight() * 3, false, false, false, false);
        BackgroundImage bgImage = new BackgroundImage(imgBackground, BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.CENTER, bgSize);
        Background background = new Background(bgImage);
        bottomBar.setBackground(background);

        // Add the logo
        ImageView logo = new ImageView(loadImage("/gui/logo.png"));
        logo.setFitWidth(400);
        logo.setPreserveRatio(true);
        bottomBar.getChildren().add(logo);
        StackPane.setAlignment(logo, Pos.BOTTOM_LEFT);

        bottomBar.getChildren().add(currentTask);
        StackPane.setAlignment(currentTask, Pos.TOP_CENTER);

        // Play Button
        double btnWidth = 300;
        double btnHeight = 50;
        Button btnPlay = new Button();

        btnPlay.setGraphic(createButtonGraphics("Play", 24, btnWidth, btnHeight, false));
        btnPlay.setMinSize(btnWidth, btnHeight);
        btnPlay.setMaxSize(btnWidth, btnHeight);

        btnPlay.setOnMouseEntered(e -> btnPlay.setGraphic(createButtonGraphics("Play", 24, btnWidth, btnHeight, true)));
        btnPlay.setOnMouseExited(e -> btnPlay.setGraphic(createButtonGraphics("Play", 24, btnWidth, btnHeight, false)));

        btnPlay.setOnAction(e -> play());

        btnPlay.setTranslateX(-20);
        btnPlay.setTranslateY(-20);
        bottomBar.getChildren().add(btnPlay);
        StackPane.setAlignment(btnPlay, Pos.BOTTOM_RIGHT);

        root.getChildren().add(bottomBar);

        Scene scene = new Scene(root, baseWidth, baseHeight);
        stage.setScene(scene);
        stage.show();
    }

    public Image loadImage(String path) {
        InputStream inputstream = Main.class.getResourceAsStream(path);
        return new Image(inputstream);
    }

    /**
     * Create a special text object with the shadow
     * @param text The text string
     * @param size The text size
     * @return
     */
    public Group createText(String text, int size) {
        Font font = Font.loadFont(
                getClass().getResourceAsStream("/fonts/Minecraftia-Regular.ttf"),
                size
        );

        Text txtBackground = new Text(text);
        txtBackground.setFont(font);
        txtBackground.setTranslateX(3);
        txtBackground.setTranslateY(3);

        Text txtForeground = new Text(text);
        txtForeground.setFont(font);
        txtForeground.setFill(Color.WHITE);

        return new Group(
                txtBackground,
                txtForeground
        );
    }

    /**
     * Create a button object
     * @param text The text string
     * @param size The text size
     * @param btnWidth The button width
     * @param btnHeight The button height
     * @param hover If hovering
     * @return
     */
    public StackPane createButtonGraphics(String text, int size, double btnWidth, double btnHeight, boolean hover) {
        StackPane btnGraphic = new StackPane();

        ImageView imgBtnBackground;

        if(hover) {
            imgBtnBackground = new ImageView(loadImage("/gui/button_active.png"));
        } else {
            imgBtnBackground = new ImageView(loadImage("/gui/button.png"));
        }

        imgBtnBackground.setFitWidth(btnWidth);
        imgBtnBackground.setFitHeight(btnHeight);
        btnGraphic.getChildren().add(imgBtnBackground);

        btnGraphic.getChildren().add(createText(text, size));

        return btnGraphic;
    }

    /**
     * Set the current tasks through a label
     * @param s
     */
    public void setCurrentTask(String s) {
        System.out.println(s);

        Platform.runLater(() -> {
            for (Node n : this.currentTask.getChildren()) {
                if (n instanceof Text t) t.setText(s);
            }
        });
    }

    /**
     * Handle the player button
     */
    public void play() {
        this.setCurrentTask("Downloading Libs...");

        // Create lib dir
        try {
            Files.createDirectories(EnvironmentUtils.runDir);
        } catch (IOException e) {
            this.setCurrentTask("Failed to create directory: " + e.getMessage());
            return;
        }

        // Download Libs
        // Download Client
        // Close launcher
        Thread downloadThread = new Thread(() -> {
            try {
                AssetDownloader.fetch();
            } catch (IOException | InterruptedException e) {
                this.setCurrentTask("Failed to download libraries: " + e.getMessage());
                return;
            }

            // Download Client
            Path blockGame = null;
            try {
                blockGame = ClientDownloader.fetch();
            } catch (IOException | InterruptedException e) {
                this.setCurrentTask("Failed to download client: " + e.getMessage());
            }


            this.setCurrentTask("Launching...");

            if (blockGame != null) {
                try {
                    launchGame(blockGame);

                    // Close launcher
                    this.setCurrentTask("Game Launched. Exiting this...");
                    System.exit(0);
                } catch (Exception e) {
                    this.setCurrentTask("Something went wrong: " + e.getMessage());
                }
            } else {
                this.setCurrentTask("Game Launch Failed! Client does not exist.");
            }
        });

        downloadThread.start();
    }

    /**
     * Launch the game with all libs
     * @param blockGame The blockgame jar
     */
    private static void launchGame(Path blockGame) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");

        //If MacOS ARM
        boolean arm = EnvironmentUtils.arch.contains("aarch64") || EnvironmentUtils.arch.contains("arm64");
        if (EnvironmentUtils.os.contains("mac") && arm) {
            cmd.add("-XstartOnFirstThread");
        }

        cmd.add("-cp");

        // Build classpath: main jar + all jars in libs/
        String classpath = new StringJoiner(File.pathSeparator)
                .add(blockGame.toAbsolutePath().toString())       // your app jar or classes dir
                .add(EnvironmentUtils.libDir.toAbsolutePath() + File.separator + "*")    // all jars in libs/
                .toString();

        cmd.add(classpath);

        cmd.add("com.james090500.Main"); // Replace with your main class name

        System.out.println(cmd);

        // Launch process
        new ProcessBuilder(cmd)
                .inheritIO()
                .directory(EnvironmentUtils.runDir.toFile())
                .start();
    }
}
