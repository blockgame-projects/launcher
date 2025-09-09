package com.james090500.BlockGameLauncher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.InputStream;

public class Launcher extends Application {

    private double baseWidth = 854;
    private double baseHeight = 480;

    private Group currentTask = new Group();

    @Override
    public void start(Stage stage) {

        StackPane root = new StackPane();

        // Add the background
        Image imgBackground = loadImage("/gui/background.png");
        BackgroundSize bgSize = new BackgroundSize(imgBackground.getWidth() * 3,imgBackground.getHeight() * 3, false, false, false, false);
        BackgroundImage bgImage = new BackgroundImage(imgBackground, BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.CENTER, bgSize);
        Background background = new Background(bgImage);
        root.setBackground(background);

        // Add the logo
        ImageView logo = new ImageView(loadImage("/gui/logo.png"));
        root.getChildren().add(logo);
        StackPane.setAlignment(logo, Pos.TOP_CENTER);

        currentTask.setTranslateY(-50);
        root.getChildren().add(currentTask);
        StackPane.setAlignment(currentTask, Pos.BOTTOM_CENTER);

        // Play Button
        double btnWidth = 300;
        double btnHeight = 50;
        Button btnPlay = new Button();

        btnPlay.setGraphic(createButtonGraphics("Play", 24, btnWidth, btnHeight, false));
        btnPlay.setMinSize(btnWidth, btnHeight);
        btnPlay.setMaxSize(btnWidth, btnHeight);

        btnPlay.setOnMouseEntered(e -> btnPlay.setGraphic(createButtonGraphics("Play", 24, btnWidth, btnHeight, true)));
        btnPlay.setOnMouseExited(e -> btnPlay.setGraphic(createButtonGraphics("Play", 24, btnWidth, btnHeight, false)));

        btnPlay.setOnAction(e -> Main.play());

        root.getChildren().add(btnPlay);
        StackPane.setAlignment(btnPlay, Pos.BOTTOM_CENTER);

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

    public void setCurrentTask(String s) {
        System.out.println(s);
        Platform.runLater(() -> {
            this.currentTask.getChildren().clear();
            this.currentTask.getChildren().add(createText(s, 24));
        });
    }
}
