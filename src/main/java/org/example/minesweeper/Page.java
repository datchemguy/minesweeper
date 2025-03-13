package org.example.minesweeper;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.image.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.util.Arrays;
import java.util.Random;
import java.util.ResourceBundle;

public class Page implements Initializable {
    @FXML private ImageView hun;
    @FXML private ImageView ten;
    @FXML private ImageView one;

    @FXML private ImageView face;

    @FXML private ImageView huns;
    @FXML private ImageView tens;
    @FXML private ImageView ones;

    @FXML private GridPane grid;

    private boolean[][] base = new boolean[1][1], open = new boolean[1][1], flagged = new boolean[1][1];
    private int rows = 1, cols = 1, bombs = 0, left = 0, toOpen = 1, time = 0;
    private final Image[] faces = new Image[4], tiles = new Image[9], nums = new Image[10];
    private Image cover, bomb, flag, expl;
    private ImageView[][] blocks;
    private final RowConstraints rowcon = new RowConstraints();
    private final ColumnConstraints colcon = new ColumnConstraints();
    private boolean started = false, done = false;
    static final int boxSize = 20;
    private final MediaPlayer click = new MediaPlayer(new Media(getClass().getResource("click.wav").toString())),
            boom = new MediaPlayer(new Media(getClass().getResource("boom.wav").toString()));
    private final Timeline timer = new Timeline(new KeyFrame(Duration.seconds(1), _ -> {
        if(time < 999) time++;
        syncTimer();
    }));

    public void initialize(URL location, ResourceBundle resources) {
        Image faceSheet = new Image(getClass().getResourceAsStream("faces.png")),
            tileSheet = new Image(getClass().getResourceAsStream("tiles.png")),
            numSheet = new Image(getClass().getResourceAsStream("nums.png"));
        PixelReader fr = faceSheet.getPixelReader(), tr = tileSheet.getPixelReader(), nr = numSheet.getPixelReader();

        int size = (int) faceSheet.getHeight();
        faces[0] = new WritableImage(fr, 0, 0, size, size);
        faces[1] = new WritableImage(fr, size, 0, size, size);
        faces[2] = new WritableImage(fr, 3*size, 0, size, size);
        faces[3] = new WritableImage(fr, 4*size, 0, size, size);
        face.setImage(faces[0]);
        face.setFitWidth(2*boxSize);
        face.setFitHeight(2*boxSize);

        size = (int) (tileSheet.getHeight() / 2);
        tiles[0] = new WritableImage(tr, size, 0, size, size);
        for(int i=1; i<9; i++) tiles[i] = new WritableImage(tr, (i-1)*size, size, size, size);
        cover = new WritableImage(tr, 0, 0, size, size);
        flag = new WritableImage(tr, 2*size, 0, size, size);
        bomb = new WritableImage(tr, 5*size, 0, size, size);
        expl = new WritableImage(tr, 6*size, 0, size, size);

        int w = (int) (numSheet.getWidth() / 10), h = (int) numSheet.getHeight();
        for(int i=0; i<10; i++) nums[i] = new WritableImage(nr, i * w, 0, w, h);
        initNums(hun, ten, one, huns, tens, ones);

        rowcon.setVgrow(Priority.ALWAYS);
        colcon.setHgrow(Priority.ALWAYS);

        boom.setVolume(0.2);

        timer.setCycleCount(Timeline.INDEFINITE);
    }

    public void build(int rows, int cols, int bombs) {
        rows = Math.max(1, rows);
        cols = Math.max(1, cols);
        bombs = Math.min(Math.max(1, bombs), rows * cols - 1);
        this.rows = rows;
        this.cols = cols;
        this.bombs = bombs;
        base = new boolean[rows][cols];
        open = new boolean[rows][cols];
        flagged = new boolean[rows][cols];
        blocks = new ImageView[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++) {
                blocks[i][j] = blockGen();
                int finalI = i, finalJ = j;
                blocks[i][j].setOnMousePressed(e -> clickTile(e.getButton(), finalI, finalJ));
                blocks[i][j].setOnMouseReleased(e -> releaseTile(e.getButton(), finalI, finalJ));
            }
        grid.getRowConstraints().clear();
        grid.getColumnConstraints().clear();
        grid.getChildren().clear();
        for (int i = 0; i < rows; i++) grid.getRowConstraints().add(rowcon);
        for (int i = 0; i < cols; i++) grid.getColumnConstraints().add(colcon);
        for (int i = 0; i < rows; i++) grid.addRow(i, blocks[i]);
        restart();
    }

    private void syncScore() {
        one.setImage(nums[left%10]);
        ten.setImage(nums[left/10%10]);
        hun.setImage(nums[left/100%10]);
    }

    private void syncTimer() {
        ones.setImage(nums[time%10]);
        tens.setImage(nums[time/10%10]);
        huns.setImage(nums[time/100%10]);
    }

    private int isBomb(int i, int j) {
        return (i >= 0 && i < base.length && j >= 0 && j < base[i].length && base[i][j]) ? 1 : 0;
    }

    private int isFlag(int i, int j) {
        return (i >= 0 && i < base.length && j >= 0 && j < base[i].length && flagged[i][j]) ? 1 : 0;
    }

    private int around(int i, int j) {
        return isBomb(i - 1, j - 1) + isBomb(i - 1, j) + isBomb(i - 1, j + 1)
                + isBomb(i, j - 1) + isBomb(i, j + 1)
                + isBomb(i + 1, j - 1) + isBomb(i + 1, j) + isBomb(i + 1, j + 1);
    }

    private int aroundFlags(int i, int j) {
        return isFlag(i - 1, j - 1) + isFlag(i - 1, j) + isFlag(i - 1, j + 1)
                + isFlag(i, j - 1) + isFlag(i, j + 1)
                + isFlag(i + 1, j - 1) + isFlag(i + 1, j) + isFlag(i + 1, j + 1);
    }

    private void uncover(int i, int j, boolean first) {
        if(i >= 0 && i < rows && j >= 0 && j < cols && (!open[i][j] || first) && !flagged[i][j]) {
            if(!started) {
                generateBase(rows, cols, bombs, new Random(), i, j);
                started = true;
                timer.play();
            }
            if(!open[i][j]) {
                toOpen--;
                if(first) {click.stop(); click.play();}
            }
            open[i][j] = true;
            if(base[i][j]) {
                blocks[i][j].setImage(expl);
                lose();
            } else {
                int around = around(i, j);
                blocks[i][j].setImage(tiles[around]);
                if(around == 0 || (first && around == aroundFlags(i, j))) {
                    uncover(i-1, j-1, false); uncover(i-1, j, false); uncover(i-1, j+1, false);
                    uncover(i, j-1, false);                                     uncover(i, j+1, false);
                    uncover(i+1, j-1, false); uncover(i+1, j, false); uncover(i+1, j+1, false);
                }
                if(!done && toOpen == 0) win();
            }
        }
    }

    public void restart() {
        for(int i=0; i<rows; i++) {
            Arrays.fill(base[i], false);
            Arrays.fill(open[i], false);
            Arrays.fill(flagged[i], false);
        }
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                blocks[i][j].setImage(cover);
        left = bombs;
        toOpen = rows * cols - bombs;
        syncScore();
        timer.stop();
        time = 0;
        syncTimer();
        started = done = false;
    }

    public void clickFace() {
        face.setImage(faces[1]);
    }

    public void releaseFace() {
        face.setImage(faces[0]);
        click.stop(); click.play();
        restart();
    }

    public void clickTile(MouseButton button, int i, int j) {
        if(!open[i][j]) switch(button) {
            case PRIMARY -> {if(!flagged[i][j]) blocks[i][j].setImage(tiles[0]);}
            case SECONDARY -> {if(left > 0 || flagged[i][j]) {
                flagged[i][j] = !flagged[i][j];
                left += flagged[i][j] ? -1 : 1;
                syncScore();
                blocks[i][j].setImage(flagged[i][j] ? flag : cover);
            }}
        }
    }

    public void releaseTile(MouseButton button, int i, int j) {
        if(button == MouseButton.PRIMARY) uncover(i, j, true);
    }

    private void win() {
        clear();
        face.setImage(faces[2]);
    }

    private void lose() {
        boom.stop();
        boom.play();
        clear();
        face.setImage(faces[3]);
    }

    private void clear() {
        done = true;
        timer.pause();
        for(int i = 0; i < rows; i++)
            for(int j = 0; j < cols; j++)
                if(!open[i][j]) {
                    blocks[i][j].setImage(base[i][j] ? bomb : tiles[around(i, j)]);
                    open[i][j] = true;
                }
    }

    private void generateBase(int rows, int cols, int bombs, Random rand, int noi, int noj) {
        base = new boolean[rows][cols];
        int except = noi * cols + noj;
        rand.ints(0, rows * cols)
                .distinct().filter(x -> x != except).limit(bombs)
                .forEach(spot -> base[spot / cols][spot % cols] = true);
    }

    private ImageView blockGen() {
        ImageView block = new ImageView();
        block.setFitHeight(boxSize);
        block.setFitWidth(boxSize);
        GridPane.setHalignment(block, HPos.CENTER);
        GridPane.setValignment(block, VPos.CENTER);
        return block;
    }

    private void initNums(ImageView... cells) {
        for(ImageView cell : cells) {
            cell.setImage(nums[0]);
            cell.setFitWidth(boxSize);
            cell.setFitHeight(2*boxSize);
        }
    }
}