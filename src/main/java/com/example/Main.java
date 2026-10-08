package com.example;

import java.io.File;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

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

    // ================= LOGO PATH =================
    private static final String LOGO_PATH = "/logo.jpg";

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
        scene.setOnKeyPressed(this::handleKeyPress);

        stage.setTitle("🎵 Pulse Player — JavaFX Media Center");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(580);
        stage.setOnCloseRequest(e -> shutdown());

        // ---------- WINDOW ICON (taskbar + window corner) ----------
        try {
            Image windowIcon = new Image(getClass().getResourceAsStream(LOGO_PATH));
            stage.getIcons().add(windowIcon);
        } catch (Exception ex) {
            System.err.println("Could not load window icon: " + ex.getMessage());
        }

        stage.show();
    }

    // ============================================================
    // TOP BAR (with logo)
    // ============================================================
    private HBox buildTopBar() {
        // ---------- LOGO ----------
        ImageView logoView = loadLogo(38);
        DropShadow logoGlow = new DropShadow(14, Color.web(ACCENT, 0.75));
        logoView.setEffect(logoGlow);

        // Round the corners of the JPG (since JPG has no transparency)
        Rectangle clip = new Rectangle(38, 38);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        logoView.setClip(clip);

        // Small neon border ring around the logo
        StackPane logoFrame = new StackPane(logoView);
        logoFrame.setStyle("-fx-background-color: linear-gradient(to bottom right, #8b5cf6, #6366f1);"
                + "-fx-background-radius: 12;"
                + "-fx-padding: 2;");
        logoFrame.setMaxSize(42, 42);
        logoFrame.setMinSize(42, 42);

        // ---------- TITLE ----------
        Label title = new Label("PULSE PLAYER");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setTextFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(ACCENT)),
                new Stop(1, Color.web(ACCENT_B))));

        HBox titleBox = new HBox(12, logoFrame, title);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // ---------- STATUS ----------
        statusLabel = new Label("Ready — press A or click Add Files");
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

    /**
     * Loads the logo image from resources at the given size.
     */
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
    // CENTER — SPLIT VIDEO + PLAYLIST
    // ============================================================
    private SplitPane buildCenterArea() {
        SplitPane split = new SplitPane(buildVideoPanel(), buildPlaylistPanel());
        split.setDividerPositions(0.68);
        split.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        return split;
    }

    // ---------- LEFT: VIDEO + CONTROLS ----------
    private VBox buildVideoPanel() {
        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);

        placeholderLabel = new Label("🎬\n\nNo media loaded\n\nPress 'A' or click Add Files to begin");
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

        // Progress row
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

        // Control buttons
        Button prevBtn = makeCtrlBtn("⏮");
        playBtn = makePlayBtn();
        Button stopBtn = makeCtrlBtn("⏹");
        Button nextBtn = makeCtrlBtn("⏭");

        prevBtn.setOnAction(e -> playPrevious());
        playBtn.setOnAction(e -> togglePlayPause());
        stopBtn.setOnAction(e -> stopMedia());
        nextBtn.setOnAction(e -> playNext());

        Separator sep = new Separator();
        sep.setOrientation(Orientation.VERTICAL);

        Label volIcon = new Label("🔊");
        volIcon.setFont(Font.font(15));
        volIcon.setTextFill(Color.web(TEXT));

        volumeSlider = new Slider(0, 100, 70);
        volumeSlider.setPrefWidth(110);
        volumeLabel = makeTimeLabel("70%");
        volumeLabel.setMinWidth(48);

        muteBtn = makeCtrlBtn("🔇");
        muteBtn.setOnAction(e -> toggleMute());

        volumeSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double vol = newV.doubleValue() / 100.0;
            if (mediaPlayer != null) mediaPlayer.setVolume(vol);
            volumeLabel.setText((int) newV.doubleValue() + "%");
            if (vol > 0) {
                isMuted = false;
                muteBtn.setText("🔇");
            }
        });

        HBox controls = new HBox(12, prevBtn, playBtn, stopBtn, nextBtn, sep,
                volIcon, volumeSlider, volumeLabel, muteBtn);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(6, 0, 0, 0));

        VBox panel = new VBox(10, videoFrame, progressRow, controls);
        panel.setPadding(new Insets(16));
        return panel;
    }

    // ---------- RIGHT: PLAYLIST ----------
    private VBox buildPlaylistPanel() {
        Label sectionTitle = new Label("📋 PLAYLIST");
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

        // Cell factory — emoji icon per media type
        playlistView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String ext = getExtension(file.getName()).toLowerCase();
                    String icon = switch (ext) {
                        case "mp4", "m4v", "mov", "avi", "mkv" -> "🎬";
                        case "mp3", "wav", "aac", "m4a"          -> "🎵";
                        default -> "📁";
                    };
                    setText(icon + "  " + file.getName());
                    setTextFill(Color.web(TEXT));
                    setFont(Font.font("System", 13));
                }
            }
        });

        // Double-click to play
        playlistView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int idx = playlistView.getSelectionModel().getSelectedIndex();
                if (idx >= 0) playIndex(idx);
            }
        });

        // Count binding
        playlistCountLabel.textProperty().bind(
            Bindings.createStringBinding(
                () -> playlist.size() + (playlist.size() == 1 ? " item" : " items"),
                playlist
            )
        );

        // Action buttons
        Button addBtn    = makeActionBtn("➕ Add Files", "linear-gradient(to right, #10b981, #059669)");
        Button removeBtn = makeActionBtn("➖ Remove",    "linear-gradient(to right, #f59e0b, #d97706)");
        Button clearBtn  = makeActionBtn("🗑 Clear Playlist", "linear-gradient(to right, #ef4444, #b91c1c)");

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
    // BOTTOM HINTS BAR
    // ============================================================
    private HBox buildHintsBar() {
        Label header = new Label("⌨ SHORTCUTS:");
        header.setTextFill(Color.web(ACCENT));
        header.setFont(Font.font("System", FontWeight.BOLD, 11));

        HBox bar = new HBox(10, header);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(9, 16, 9, 16));

        String[][] hints = {
                {"Space", "Play/Pause"}, {"S", "Stop"}, {"N", "Next"}, {"P", "Previous"},
                {"↑/↓", "Volume"},       {"M", "Mute"}, {"A", "Add"},  {"Del", "Remove"}
        };
        for (String[] h : hints) {
            Label lbl = new Label(h[0] + " = " + h[1]);
            lbl.setTextFill(Color.web(MUTED));
            lbl.setFont(Font.font("System", 11));
            lbl.setPadding(new Insets(2, 8, 2, 8));
            lbl.setStyle("-fx-background-color: rgba(51,65,85,0.5); -fx-background-radius: 10;");
            bar.getChildren().add(lbl);
        }

        bar.setStyle("-fx-background-color: rgba(15,23,42,0.9);"
                + "-fx-border-color: rgba(120,90,255,0.3) transparent transparent transparent;"
                + "-fx-border-width: 1.5 0 0 0;");
        return bar;
    }

    // ============================================================
    // KEYBOARD HANDLING
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
            else setStatus("⚠ Playlist empty — add files first (press A)");
            return;
        }
        MediaPlayer.Status st = mediaPlayer.getStatus();
        if (st == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playBtn.setText("▶");
            setStatus("⏸ Paused");
        } else if (st == MediaPlayer.Status.PAUSED
                || st == MediaPlayer.Status.READY
                || st == MediaPlayer.Status.STOPPED) {
            mediaPlayer.play();
            playBtn.setText("⏸");
            setStatus("▶ Playing");
        }
    }

    private void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            playBtn.setText("▶");
            progressSlider.setValue(0);
            currentTimeLabel.setText("00:00");
            setStatus("⏹ Stopped");
        }
    }

    // ============================================================
    // NEXT / PREVIOUS / VOLUME
    // ============================================================
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
        setStatus("🔊 Volume: " + (int) v + "%");
    }

    private void toggleMute() {
        if (mediaPlayer == null) return;
        if (isMuted) {
            mediaPlayer.setVolume(volumeBeforeMute);
            volumeSlider.setValue(volumeBeforeMute * 100);
            muteBtn.setText("🔇");
            isMuted = false;
            setStatus("🔊 Unmuted");
        } else {
            volumeBeforeMute = volumeSlider.getValue() / 100.0;
            mediaPlayer.setVolume(0);
            muteBtn.setText("🔊");
            isMuted = true;
            setStatus("🔇 Muted");
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
            setStatus("➕ Added " + files.size() + " file(s)");
            if (currentIndex == -1) Platform.runLater(() -> playIndex(0));
        }
    }

    private void removeSelected() {
        File selected = playlistView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("⚠ Select a file to remove");
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
                playBtn.setText("▶");
                resetTimeLabels();
            }
        } else {
            playlist.remove(idx);
            if (idx < currentIndex) currentIndex--;
        }
        setStatus("➖ Removed: " + selected.getName());
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
                playBtn.setText("▶");
                resetTimeLabels();
                setStatus("🗑 Playlist cleared");
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
                setStatus("▶ Playing: " + file.getName());
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                if (playlist.size() > 1) playNext();
                else {
                    mediaPlayer.seek(Duration.ZERO);
                    mediaPlayer.pause();
                    playBtn.setText("▶");
                }
            });

            mediaPlayer.setOnError(() ->
                setStatus(" Error: " + mediaPlayer.getError().getMessage()));

            mediaPlayer.play();
            playBtn.setText("⏸");
            currentIndex = index;
            playlistView.getSelectionModel().select(index);
            playlistView.scrollTo(index);

        } catch (Exception ex) {
            setStatus(" Cannot play: " + file.getName());
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

    private Button makeCtrlBtn(String text) {
        Button b = new Button(text);
        b.setMinSize(48, 42);
        b.setFont(Font.font(16));
        b.setStyle(baseCtrlStyle());
        b.setOnMouseEntered(e -> b.setStyle(hoverCtrlStyle()));
        b.setOnMouseExited(e -> b.setStyle(baseCtrlStyle()));
        return b;
    }

    private Button makePlayBtn() {
        Button b = new Button("▶");
        b.setMinSize(62, 48);
        b.setFont(Font.font(20));
        DropShadow glow = new DropShadow(14, Color.web(ACCENT, 0.6));
        glow.setOffsetY(3);
        b.setEffect(glow);
        String base = "-fx-background-color: linear-gradient(to bottom right, #8b5cf6, #6366f1);"
                + "-fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;";
        String hover = "-fx-background-color: linear-gradient(to bottom right, #a78bfa, #818cf8);"
                + "-fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    private Button makeActionBtn(String text, String gradient) {
        Button b = new Button(text);
        b.setFont(Font.font("System", FontWeight.BOLD, 12));
        b.setStyle("-fx-background-color: " + gradient + ";"
                + "-fx-text-fill: white; -fx-padding: 9 12;"
                + "-fx-background-radius: 8; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setOpacity(0.85));
        b.setOnMouseExited(e -> b.setOpacity(1.0));
        return b;
    }

    private String baseCtrlStyle() {
        return "-fx-background-color: rgba(51,65,85,0.7);"
                + "-fx-text-fill: #e2e8f0;"
                + "-fx-background-radius: 10;"
                + "-fx-border-color: rgba(148,163,184,0.25);"
                + "-fx-border-radius: 10;"
                + "-fx-cursor: hand;";
    }

    private String hoverCtrlStyle() {
        return "-fx-background-color: rgba(99,102,241,0.55);"
                + "-fx-text-fill: #e2e8f0;"
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