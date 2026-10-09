package com.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class Main extends Application {

    // ================= UI COMPONENTS =================
    private MediaView mediaView;
    private Label placeholderLabel;
    private Label statusLabel;
    private Label currentTimeLabel, totalTimeLabel, volumeLabel, playlistCountLabel;
    private Slider progressSlider, volumeSlider;
    private Button playBtn, muteBtn;
    private ListView<File> playlistView;

    // ================= STATE =================
    private MediaPlayer mediaPlayer;
    private final ObservableList<File> playlist = FXCollections.observableArrayList();
    private int currentIndex = -1;
    private boolean isMuted = false;
    private double volumeBeforeMute = 0.7;
    private boolean isDraggingProgress = false;

    // ================= COLORS =================
    private static final String BG_TOP    = "#0f0c29";
    private static final String BG_MID    = "#1a1a3e";
    private static final String BG_BOT    = "#24243e";
    private static final String ACCENT    = "#a78bfa";
    private static final String ACCENT_B  = "#60a5fa";
    private static final String TEXT      = "#e2e8f0";
    private static final String MUTED     = "#94a3b8";
    private static final String PANEL     = "rgba(20,20,45,0.85)";

    // ================= LOGO =================
    private static final String LOGO_PATH = "/logo.jpg";

    // ============================================================
    // VECTOR ICONS (SVG paths, 24x24 grid)
    // ============================================================
    private static final class Icons {
        static final String PLAY   = "M8 5v14l11-7z";
        static final String PAUSE  = "M6 19h4V5H6v14zm8-14v14h4V5h-4z";
        static final String STOP   = "M6 6h12v12H6z";
        static final String NEXT   = "M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z";
        static final String PREV   = "M6 6h2v12H6zm3.5 6l8.5 6V6z";
        static final String VOLUME = "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z";
        static final String MUTE   = "M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z";
        static final String ADD    = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
        static final String REMOVE = "M19 13H5v-2h14v2z";
        static final String TRASH  = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";
        static final String MUSIC  = "M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z";
        static final String VIDEO  = "M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z";
        static final String FILE   = "M6 2c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6H6zm7 7V3.5L18.5 9H13z";
    }

    /** Builds a themed SVG icon as a StackPane wrapping an SVGPath. */
    private static StackPane icon(String path, double size, Color color) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setFill(color);

        double scale = size / 24.0;
        svg.setScaleX(scale);
        svg.setScaleY(scale);

        Group group = new Group(svg);
        StackPane holder = new StackPane(group);
        holder.setMinSize(size, size);
        holder.setPrefSize(size, size);
        holder.setMaxSize(size, size);
        return holder;
    }

    /** Updates an existing SVG icon in-place (used to swap PLAY/PAUSE etc). */
    private static void swapIcon(Region iconNode, String newPath, Color color) {
        if (!(iconNode instanceof StackPane)) return;
        StackPane holder = (StackPane) iconNode;
        holder.getChildren().forEach(n -> {
            if (n instanceof Group) {
                ((Group) n).getChildren().forEach(c -> {
                    if (c instanceof SVGPath) {
                        ((SVGPath) c).setContent(newPath);
                        ((SVGPath) c).setFill(color);
                    }
                });
            }
        });
    }

    // ============================================================
    // START
    // ============================================================
    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, "
                + BG_TOP + ", " + BG_MID + ", " + BG_BOT + ");");

        root.setTop(buildTopBar());
        root.setCenter(buildCenterArea());
        root.setBottom(buildHintsBar());

        Scene scene = new Scene(root, 1050, 680);

        // KEY FIX: event FILTER so no control consumes keys before us
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        stage.setTitle("Pulse Player - JavaFX Media Center");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(580);
        stage.setOnCloseRequest(e -> shutdown());

        try {
            Image windowIcon = new Image(getClass().getResourceAsStream(LOGO_PATH));
            stage.getIcons().add(windowIcon);
        } catch (Exception ex) {
            System.err.println("Could not load window icon: " + ex.getMessage());
        }

        stage.show();
        root.requestFocus();
    }

    // ============================================================
    // TOP BAR
    // ============================================================
    private HBox buildTopBar() {
        ImageView logoView = loadLogo(38);
        DropShadow logoGlow = new DropShadow(14, Color.web(ACCENT, 0.75));
        logoView.setEffect(logoGlow);

        Rectangle clip = new Rectangle(38, 38);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        logoView.setClip(clip);

        StackPane logoFrame = new StackPane(logoView);
        logoFrame.setStyle("-fx-background-color: linear-gradient(to bottom right, #8b5cf6, #6366f1);"
                + "-fx-background-radius: 12;"
                + "-fx-padding: 2;");
        logoFrame.setMaxSize(42, 42);
        logoFrame.setMinSize(42, 42);

        Label title = new Label("PULSE PLAYER");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setTextFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(ACCENT)),
                new Stop(1, Color.web(ACCENT_B))));

        HBox titleBox = new HBox(12, logoFrame, title);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Ready - press A or click Add Files");
        statusLabel.setTextFill(Color.web(MUTED));
        statusLabel.setFont(Font.font("System", 12));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(15, titleBox, spacer, statusLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 22, 14, 22));
        bar.setStyle("-fx-background-color: " + PANEL + ";"
                + "-fx-border-color: transparent transparent rgba(120,90,255,0.35) transparent;"
                + "-fx-border-width: 0 0 1.5 0;");
        return bar;
    }

    private ImageView loadLogo(double size) {
        ImageView iv = new ImageView();
        try {
            Image img = new Image(getClass().getResourceAsStream(LOGO_PATH));
            iv.setImage(img);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
        } catch (Exception ex) {
            System.err.println("Could not load logo: " + ex.getMessage());
        }
        return iv;
    }

    // ============================================================
    // CENTER
    // ============================================================
    private SplitPane buildCenterArea() {
        SplitPane split = new SplitPane(buildVideoPanel(), buildPlaylistPanel());
        split.setDividerPositions(0.68);
        split.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        return split;
    }

    private VBox buildVideoPanel() {
        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);

        placeholderLabel = new Label("No media loaded\n\nPress A or click Add Files to begin");
        placeholderLabel.setTextFill(Color.web("#64748b"));
        placeholderLabel.setFont(Font.font("System", 15));
        placeholderLabel.setStyle("-fx-text-alignment: center; -fx-line-spacing: 4;");

        StackPane videoFrame = new StackPane(mediaView, placeholderLabel);
        videoFrame.setStyle("-fx-background-color: #000;"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: rgba(139,92,246,0.4);"
                + "-fx-border-radius: 12;"
                + "-fx-border-width: 1.5;");
        DropShadow glow = new DropShadow(22, Color.web(ACCENT, 0.35));
        glow.setOffsetY(6);
        videoFrame.setEffect(glow);
        VBox.setVgrow(videoFrame, Priority.ALWAYS);

        currentTimeLabel = makeTimeLabel("00:00");
        totalTimeLabel   = makeTimeLabel("00:00");

        progressSlider = new Slider(0, 100, 0);
        HBox.setHgrow(progressSlider, Priority.ALWAYS);
        progressSlider.setOnMousePressed(e -> isDraggingProgress = true);
        progressSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null)
                mediaPlayer.seek(Duration.seconds(progressSlider.getValue()));
            isDraggingProgress = false;
        });

        HBox progressRow = new HBox(10, currentTimeLabel, progressSlider, totalTimeLabel);
        progressRow.setAlignment(Pos.CENTER);

        // ---- Icon buttons ----
        Button prevBtn = makeCtrlBtn(Icons.PREV, "Previous (P)");
        playBtn        = makePlayBtn();
        Button stopBtn = makeCtrlBtn(Icons.STOP, "Stop (S)");
        Button nextBtn = makeCtrlBtn(Icons.NEXT, "Next (N)");

        prevBtn.setOnAction(e -> playPrevious());
        playBtn.setOnAction(e -> togglePlayPause());
        stopBtn.setOnAction(e -> stopMedia());
        nextBtn.setOnAction(e -> playNext());

        Separator sep = new Separator();
        sep.setOrientation(Orientation.VERTICAL);

        Region volIcon = icon(Icons.VOLUME, 18, Color.web(TEXT));

        volumeSlider = new Slider(0, 100, 70);
        volumeSlider.setPrefWidth(110);
        volumeLabel = makeTimeLabel("70%");
        volumeLabel.setMinWidth(48);

        muteBtn = makeCtrlBtn(Icons.MUTE, "Mute (M)");
        muteBtn.setOnAction(e -> toggleMute());

        volumeSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double vol = newV.doubleValue() / 100.0;
            if (mediaPlayer != null) mediaPlayer.setVolume(vol);
            volumeLabel.setText((int) newV.doubleValue() + "%");
            if (vol > 0) {
                isMuted = false;
                swapIcon((Region) muteBtn.getGraphic(), Icons.MUTE, Color.web(TEXT));
            }
        });

        HBox controls = new HBox(10, prevBtn, playBtn, stopBtn, nextBtn, sep,
                volIcon, volumeSlider, volumeLabel, muteBtn);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(6, 0, 0, 0));

        VBox panel = new VBox(10, videoFrame, progressRow, controls);
        panel.setPadding(new Insets(16));
        return panel;
    }

    private VBox buildPlaylistPanel() {
        Label sectionTitle = new Label("PLAYLIST");
        sectionTitle.setTextFill(Color.web(TEXT));
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 13));

        playlistCountLabel = new Label("0 items");
        playlistCountLabel.setTextFill(Color.web(MUTED));
        playlistCountLabel.setFont(Font.font("System", 11));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox titleRow = new HBox(10, sectionTitle, spacer, playlistCountLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        playlistView = new ListView<>(playlist);
        playlistView.setStyle("-fx-background-color: rgba(15,23,42,0.7);"
                + "-fx-background-radius: 10;"
                + "-fx-border-color: rgba(139,92,246,0.3);"
                + "-fx-border-radius: 10;"
                + "-fx-border-width: 1;");
        VBox.setVgrow(playlistView, Priority.ALWAYS);

        // Cells with vector icons per file type
        playlistView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String ext = getExtension(file.getName()).toLowerCase();
                    String path = switch (ext) {
                        case "mp4", "m4v", "mov", "avi", "mkv" -> Icons.VIDEO;
                        case "mp3", "wav", "aac", "m4a"          -> Icons.MUSIC;
                        default                                  -> Icons.FILE;
                    };
                    setGraphic(icon(path, 16, Color.web(ACCENT)));
                    setText(file.getName());
                    setContentDisplay(ContentDisplay.LEFT);
                    setGraphicTextGap(10);
                    setTextFill(Color.web(TEXT));
                    setFont(Font.font("System", 13));
                }
            }
        });

        playlistView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int idx = playlistView.getSelectionModel().getSelectedIndex();
                if (idx >= 0) playIndex(idx);
            }
        });

        playlistCountLabel.textProperty().bind(
            Bindings.createStringBinding(
                () -> playlist.size() + (playlist.size() == 1 ? " item" : " items"),
                playlist
            )
        );

        Button addBtn    = makeIconTextBtn(Icons.ADD, "Add Files",
                "linear-gradient(to right, #10b981, #059669)");
        Button removeBtn = makeIconTextBtn(Icons.REMOVE, "Remove",
                "linear-gradient(to right, #f59e0b, #d97706)");
        Button clearBtn  = makeIconTextBtn(Icons.TRASH, "Clear Playlist",
                "linear-gradient(to right, #ef4444, #b91c1c)");

        addBtn.setOnAction(e -> addFiles());
        removeBtn.setOnAction(e -> removeSelected());
        clearBtn.setOnAction(e -> clearPlaylist());

        addBtn.setMaxWidth(Double.MAX_VALUE);
        removeBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(addBtn, Priority.ALWAYS);
        HBox.setHgrow(removeBtn, Priority.ALWAYS);

        HBox btnRow = new HBox(8, addBtn, removeBtn);

        VBox panel = new VBox(10, titleRow, playlistView, btnRow, clearBtn);
        panel.setPadding(new Insets(16, 16, 16, 8));
        return panel;
    }

    // ============================================================
    // BOTTOM HINTS BAR (with mini icons)
    // ============================================================
    private HBox buildHintsBar() {
        Label header = new Label("SHORTCUTS:");
        header.setTextFill(Color.web(ACCENT));
        header.setFont(Font.font("System", FontWeight.BOLD, 11));

        HBox bar = new HBox(8, header);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(9, 16, 9, 16));

        String[][] hints = {
                {"Space", "Play/Pause", Icons.PLAY},
                {"S",     "Stop",       Icons.STOP},
                {"N",     "Next",       Icons.NEXT},
                {"P",     "Previous",   Icons.PREV},
                {"Up/Down", "Volume",   Icons.VOLUME},
                {"M",     "Mute",       Icons.MUTE},
                {"A",     "Add",        Icons.ADD},
                {"Del",   "Remove",     Icons.REMOVE}
        };
        for (String[] h : hints) {
            Region ic = icon(h[2], 13, Color.web(ACCENT));
            Label lbl = new Label(h[0] + " = " + h[1]);
            lbl.setGraphic(ic);
            lbl.setContentDisplay(ContentDisplay.LEFT);
            lbl.setGraphicTextGap(6);
            lbl.setTextFill(Color.web(MUTED));
            lbl.setFont(Font.font("System", 11));
            lbl.setPadding(new Insets(2, 10, 2, 8));
            lbl.setStyle("-fx-background-color: rgba(51,65,85,0.5); -fx-background-radius: 10;");
            bar.getChildren().add(lbl);
        }

        bar.setStyle("-fx-background-color: rgba(15,23,42,0.9);"
                + "-fx-border-color: rgba(120,90,255,0.3) transparent transparent transparent;"
                + "-fx-border-width: 1.5 0 0 0;");
        return bar;
    }

    // ============================================================
    // KEYBOARD HANDLING — attached via addEventFilter
    // ============================================================
    private void handleKeyPress(KeyEvent event) {
        if (event.getTarget() instanceof TextInputControl) return;

        switch (event.getCode()) {
            case SPACE  -> { togglePlayPause(); event.consume(); }
            case S      -> { stopMedia();       event.consume(); }
            case N      -> { playNext();        event.consume(); }
            case P      -> { playPrevious();    event.consume(); }
            case UP     -> { bumpVolume(+5);    event.consume(); }
            case DOWN   -> { bumpVolume(-5);    event.consume(); }
            case M      -> { toggleMute();      event.consume(); }
            case A      -> { addFiles();        event.consume(); }
            case DELETE, BACK_SPACE -> { removeSelected(); event.consume(); }
            default -> { }
        }
    }

    // ============================================================
    // PLAYBACK CONTROLS
    // ============================================================
    private void togglePlayPause() {
        if (mediaPlayer == null) {
            if (!playlist.isEmpty()) playIndex(0);
            else setStatus("Playlist empty - add files first (press A)");
            return;
        }
        MediaPlayer.Status st = mediaPlayer.getStatus();
        if (st == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            swapIcon((Region) playBtn.getGraphic(), Icons.PLAY, Color.WHITE);
            setStatus("Paused");
        } else if (st == MediaPlayer.Status.PAUSED
                || st == MediaPlayer.Status.READY
                || st == MediaPlayer.Status.STOPPED) {
            mediaPlayer.play();
            swapIcon((Region) playBtn.getGraphic(), Icons.PAUSE, Color.WHITE);
            setStatus("Playing");
        }
    }

    private void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            swapIcon((Region) playBtn.getGraphic(), Icons.PLAY, Color.WHITE);
            progressSlider.setValue(0);
            currentTimeLabel.setText("00:00");
            setStatus("Stopped");
        }
    }

    private void playNext() {
        if (playlist.isEmpty()) return;
        playIndex((currentIndex + 1) % playlist.size());
    }

    private void playPrevious() {
        if (playlist.isEmpty()) return;
        if (mediaPlayer != null
                && mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(3))) {
            mediaPlayer.seek(Duration.ZERO);
            return;
        }
        playIndex((currentIndex - 1 + playlist.size()) % playlist.size());
    }

    private void bumpVolume(double delta) {
        double v = Math.max(0, Math.min(100, volumeSlider.getValue() + delta));
        volumeSlider.setValue(v);
        setStatus("Volume: " + (int) v + "%");
    }

    private void toggleMute() {
        if (mediaPlayer == null) return;
        if (isMuted) {
            mediaPlayer.setVolume(volumeBeforeMute);
            volumeSlider.setValue(volumeBeforeMute * 100);
            swapIcon((Region) muteBtn.getGraphic(), Icons.MUTE, Color.web(TEXT));
            isMuted = false;
            setStatus("Unmuted");
        } else {
            volumeBeforeMute = volumeSlider.getValue() / 100.0;
            mediaPlayer.setVolume(0);
            swapIcon((Region) muteBtn.getGraphic(), Icons.VOLUME, Color.web(TEXT));
            isMuted = true;
            setStatus("Muted");
        }
    }

    // ============================================================
    // PLAYLIST MANAGEMENT
    // ============================================================
    private void addFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Add Media Files");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Media",
                "*.mp4","*.mp3","*.wav","*.m4a","*.aac","*.m4v","*.mov","*.avi","*.mkv"),
            new FileChooser.ExtensionFilter("Video", "*.mp4","*.m4v","*.mov","*.avi","*.mkv"),
            new FileChooser.ExtensionFilter("Audio", "*.mp3","*.wav","*.m4a","*.aac")
        );

        List<File> files = chooser.showOpenMultipleDialog(playlistView.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            playlist.addAll(files);
            setStatus("Added " + files.size() + " file(s)");
            if (currentIndex == -1) Platform.runLater(() -> playIndex(0));
        }
    }

    private void removeSelected() {
        File selected = playlistView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Select a file to remove");
            return;
        }
        int idx = playlist.indexOf(selected);

        if (idx == currentIndex) {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
                mediaView.setMediaPlayer(null);
            }
            playlist.remove(idx);
            placeholderLabel.setVisible(true);
            if (!playlist.isEmpty()) {
                playIndex(Math.min(idx, playlist.size() - 1));
            } else {
                currentIndex = -1;
                swapIcon((Region) playBtn.getGraphic(), Icons.PLAY, Color.WHITE);
                resetTimeLabels();
            }
        } else {
            playlist.remove(idx);
            if (idx < currentIndex) currentIndex--;
        }
        setStatus("Removed: " + selected.getName());
    }

    private void clearPlaylist() {
        if (playlist.isEmpty()) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Clear the entire playlist?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                    mediaPlayer = null;
                    mediaView.setMediaPlayer(null);
                }
                playlist.clear();
                currentIndex = -1;
                placeholderLabel.setVisible(true);
                swapIcon((Region) playBtn.getGraphic(), Icons.PLAY, Color.WHITE);
                resetTimeLabels();
                setStatus("Playlist cleared");
            }
        });
    }

    // ============================================================
    // CORE PLAYBACK ENGINE
    // ============================================================
    private void playIndex(int index) {
        if (index < 0 || index >= playlist.size()) return;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        File file = playlist.get(index);
        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            placeholderLabel.setVisible(false);

            mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);

            mediaPlayer.currentTimeProperty().addListener((obs, oldT, newT) -> {
                if (!isDraggingProgress)
                    progressSlider.setValue(newT.toSeconds());
                currentTimeLabel.setText(formatTime(newT));
            });

            mediaPlayer.setOnReady(() -> {
                Duration total = mediaPlayer.getMedia().getDuration();
                progressSlider.setMin(0);
                progressSlider.setMax(total.toSeconds());
                totalTimeLabel.setText(formatTime(total));
                setStatus("Playing: " + file.getName());
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                if (playlist.size() > 1) playNext();
                else {
                    mediaPlayer.seek(Duration.ZERO);
                    mediaPlayer.pause();
                    swapIcon((Region) playBtn.getGraphic(), Icons.PLAY, Color.WHITE);
                }
            });

            mediaPlayer.setOnError(() ->
                setStatus("Error: " + mediaPlayer.getError().getMessage()));

            mediaPlayer.play();
            swapIcon((Region) playBtn.getGraphic(), Icons.PAUSE, Color.WHITE);
            currentIndex = index;
            playlistView.getSelectionModel().select(index);
            playlistView.scrollTo(index);

        } catch (Exception ex) {
            setStatus("Cannot play: " + file.getName());
            ex.printStackTrace();
        }
    }

    // ============================================================
    // HELPERS / UI FACTORIES
    // ============================================================
    private Label makeTimeLabel(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#cbd5e1"));
        l.setFont(Font.font("Consolas", 12));
        l.setMinWidth(45);
        l.setAlignment(Pos.CENTER);
        return l;
    }

    private Button makeCtrlBtn(String svgPath, String tooltip) {
        Button b = new Button();
        b.setGraphic(icon(svgPath, 18, Color.web(TEXT)));
        b.setMinSize(48, 42);
        b.setStyle(baseCtrlStyle());
        b.setTooltip(new Tooltip(tooltip));
        b.setOnMouseEntered(e -> {
            b.setStyle(hoverCtrlStyle());
            swapIcon((Region) b.getGraphic(), svgPath, Color.WHITE);
        });
        b.setOnMouseExited(e -> {
            b.setStyle(baseCtrlStyle());
            swapIcon((Region) b.getGraphic(), svgPath, Color.web(TEXT));
        });
        return b;
    }

    private Button makePlayBtn() {
        Button b = new Button();
        b.setGraphic(icon(Icons.PLAY, 22, Color.WHITE));
        b.setMinSize(62, 48);
        DropShadow glow = new DropShadow(14, Color.web(ACCENT, 0.6));
        glow.setOffsetY(3);
        b.setEffect(glow);
        String base = "-fx-background-color: linear-gradient(to bottom right, #8b5cf6, #6366f1);"
                + "-fx-background-radius: 10; -fx-cursor: hand;";
        String hover = "-fx-background-color: linear-gradient(to bottom right, #a78bfa, #818cf8);"
                + "-fx-background-radius: 10; -fx-cursor: hand;";
        b.setStyle(base);
        b.setTooltip(new Tooltip("Play / Pause (Space)"));
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    private Button makeIconTextBtn(String svgPath, String text, String gradient) {
        Button b = new Button(text);
        b.setGraphic(icon(svgPath, 14, Color.WHITE));
        b.setContentDisplay(ContentDisplay.LEFT);
        b.setGraphicTextGap(8);
        b.setFont(Font.font("System", FontWeight.BOLD, 12));
        b.setStyle("-fx-background-color: " + gradient + ";"
                + "-fx-text-fill: white; -fx-padding: 9 14;"
                + "-fx-background-radius: 8; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setOpacity(0.85));
        b.setOnMouseExited(e -> b.setOpacity(1.0));
        return b;
    }

    private String baseCtrlStyle() {
        return "-fx-background-color: rgba(51,65,85,0.7);"
                + "-fx-background-radius: 10;"
                + "-fx-border-color: rgba(148,163,184,0.25);"
                + "-fx-border-radius: 10;"
                + "-fx-cursor: hand;";
    }

    private String hoverCtrlStyle() {
        return "-fx-background-color: rgba(99,102,241,0.55);"
                + "-fx-background-radius: 10;"
                + "-fx-border-color: #a78bfa;"
                + "-fx-border-radius: 10;"
                + "-fx-cursor: hand;";
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    private void resetTimeLabels() {
        currentTimeLabel.setText("00:00");
        totalTimeLabel.setText("00:00");
        progressSlider.setValue(0);
    }

    private static String formatTime(Duration d) {
        if (d == null) return "00:00";
        int total = (int) d.toSeconds();
        int m = total / 60, s = total % 60;
        if (m >= 60) return String.format("%d:%02d:%02d", m / 60, m % 60, s);
        return String.format("%02d:%02d", m, s);
    }

    private static String getExtension(String name) {
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(i + 1) : "";
    }

    private void shutdown() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}