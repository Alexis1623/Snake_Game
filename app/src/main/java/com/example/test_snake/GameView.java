package com.example.test_snake;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import android.graphics.BitmapFactory;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import androidx.core.content.res.ResourcesCompat;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private Thread gameThread;
    private boolean running = false;
    private SurfaceHolder holder;
    private Paint paint;

    // Puntos actuales y constantes de la cuadricula
    private int score = 0;
    private static final int GRID_WIDTH = 40;
    private static final int GRID_HEIGHT = 20;

    // La serpiente: lista de segmentos (cada uno es un punto en la cuadrícula)
    private List<Point> snake;
    private Direction currentDirection = Direction.RIGHT;
    private Direction nextDirection    = Direction.RIGHT;

    // La comida (manzana) en la cuadrícula
    private Point food;

    // Control del tiempo entre frames
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 200;

    // Monedas y sistema relacionado
    private int coins = 0;
    private boolean isGolden = false;
    private Random random = new Random();
    private SharedPreferences prefs;
    private String equippedSkin;
    private DatabaseReference coinsRef;
    private String username;

    // Modo noche
    private boolean nightMode = true; // por defecto

    // Variables que ayudan a escalar el juego según pantalla
    private int dynamicBlockSize;
    private int gameAreaWidth, gameAreaHeight;
    private int gameAreaOffsetX, gameAreaOffsetY;

    // Estado: ¿terminó la partida?
    private boolean gameOver = false;

    // Sistema de fondos y transición
    private int currentBackground = R.mipmap.fondolvl1;
    private int targetBackground = R.mipmap.fondo_marte;
    private boolean backgroundChanged = false;
    private boolean transitionInProgress = false;
    private float transitionProgress = 0f;
    private static final float TRANSITION_DURATION = 1.5f; // 1.5 segundos
    private long transitionStartTime = 0;
    private boolean soundPlayed = false;

    // MediaPlayer para el sonido de transición
    private MediaPlayer transitionSound;

    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        holder = getHolder();
        holder.addCallback(this);
        paint = new Paint();
        setFocusable(true);

        prefs = getContext().getSharedPreferences("SnakePrefs", Context.MODE_PRIVATE);
        coins = prefs.getInt("coins", 0);
        equippedSkin = prefs.getString("equipped_skin", "skin_default");
        username = prefs.getString("username", "Invitado");
        // Leer night_mode (true = modo noche, false = modo día)
        nightMode = prefs.getBoolean("night_mode", true);

        // Inicializamos la referencia a Firebase para las monedas del usuario
        coinsRef = FirebaseDatabase.getInstance()
                .getReference("user_coins")
                .child(username);

        // Inicializar el MediaPlayer para el sonido de transición
        initTransitionSound();

        initGame();
    }

    private void initTransitionSound() {
        try {
            transitionSound = MediaPlayer.create(getContext(), R.raw.lvlup);
            if (transitionSound != null) {
                transitionSound.setVolume(0.7f, 0.7f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initGame() {
        snake = new ArrayList<>();
        snake.add(new Point(GRID_WIDTH / 2, GRID_HEIGHT / 2));
        snake.add(new Point(GRID_WIDTH / 2 - 1, GRID_HEIGHT / 2));
        snake.add(new Point(GRID_WIDTH / 2 - 2, GRID_HEIGHT / 2));

        generateFood();

        score = 0;
        gameOver = false;
        currentDirection = Direction.RIGHT;
        nextDirection = Direction.RIGHT;

        currentBackground = R.mipmap.fondolvl1;
        backgroundChanged = false;
        transitionInProgress = false;
        transitionProgress = 0f;
        soundPlayed = false;
    }

    private void generateFood() {
        while (true) {
            int x = random.nextInt(GRID_WIDTH);
            int y = random.nextInt(GRID_HEIGHT);
            food = new Point(x, y);

            boolean collision = false;
            for (Point segment : snake) {
                if (segment.equals(food)) {
                    collision = true;
                    break;
                }
            }
            if (!collision) break;
        }
        isGolden = random.nextFloat() < 0.2;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (transitionSound != null) {
            transitionSound.release();
            transitionSound = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        calculateGameArea();
    }

    private void calculateGameArea() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;

        int controlsAreaWidth = (int) (180 * density);
        int margin = (int) (16 * density);

        int availableWidth = getWidth() - controlsAreaWidth - (2 * margin);
        int availableHeight = getHeight() - (2 * margin);

        int maxBlockSizeByWidth = availableWidth / GRID_WIDTH;
        int maxBlockSizeByHeight = availableHeight / GRID_HEIGHT;

        dynamicBlockSize = Math.min(maxBlockSizeByWidth, maxBlockSizeByHeight);
        if (dynamicBlockSize < 1) {
            dynamicBlockSize = 1;
        }

        gameAreaWidth = GRID_WIDTH * dynamicBlockSize;
        gameAreaHeight = GRID_HEIGHT * dynamicBlockSize;

        gameAreaOffsetX = margin;
        gameAreaOffsetY = (getHeight() - gameAreaHeight) / 2;
    }

    @Override
    public void run() {
        while (running) {
            if (!holder.getSurface().isValid()) continue;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime > UPDATE_INTERVAL) {
                if (!gameOver) {
                    updateGame();
                }
                lastUpdateTime = currentTime;
            }

            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                drawGame(canvas);
                holder.unlockCanvasAndPost(canvas);
            }

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateGame() {
        currentDirection = nextDirection;

        Point head = new Point(snake.get(0));
        switch (currentDirection) {
            case UP: head.y--; break;
            case DOWN: head.y++; break;
            case LEFT: head.x--; break;
            case RIGHT: head.x++; break;
        }

        if (!backgroundChanged && !transitionInProgress && score >= 50) {
            startBackgroundTransition();
        }

        if (transitionInProgress) {
            updateTransition();
        }

        if (head.x < 0 || head.x >= GRID_WIDTH || head.y < 0 || head.y >= GRID_HEIGHT) {
            gameOver = true;
            return;
        }

        for (int i = 1; i < snake.size(); i++) {
            if (head.equals(snake.get(i))) {
                gameOver = true;
                return;
            }
        }

        snake.add(0, head);

        if (head.equals(food)) {
            score += 10;
            coins += isGolden ? 5 : 1;
            prefs.edit().putInt("coins", coins).apply();
            saveCoinsToFirebase();
            generateFood();
            calculateGameArea();
        } else {
            snake.remove(snake.size() - 1);
        }
    }

    private void startBackgroundTransition() {
        transitionInProgress = true;
        transitionStartTime = System.currentTimeMillis();
        transitionProgress = 0f;
        playTransitionSound();
    }

    private void playTransitionSound() {
        if (transitionSound != null && !soundPlayed) {
            try {
                transitionSound.seekTo(0);
                transitionSound.start();
                soundPlayed = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateTransition() {
        long currentTime = System.currentTimeMillis();
        float elapsed = (currentTime - transitionStartTime) / 1000f;
        transitionProgress = elapsed / TRANSITION_DURATION;
        if (transitionProgress >= 1f) {
            transitionInProgress = false;
            backgroundChanged = true;
            currentBackground = targetBackground;
        }
    }

    private void saveCoinsToFirebase() {
        if (coinsRef != null) {
            coinsRef.setValue(coins);
        }
    }

    private void gameOver() {
        gameOver = true;
    }

    private void drawGame(Canvas canvas) {
        // Actualizar nightMode en cada frame
        nightMode = prefs.getBoolean("night_mode", true);

        // Fondo: negro en modo noche, gris claro en modo día
        canvas.drawColor(nightMode ? Color.BLACK : Color.parseColor("#F5F5F5"));

        if (dynamicBlockSize == 0) {
            calculateGameArea();
        }

        // Dibujar fondos con transición (sin cambios)
        drawBackgroundWithTransition(canvas);

        paint.setColor(nightMode ? Color.WHITE : Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setAlpha(255);

        Rect borderRect = new Rect(
                gameAreaOffsetX,
                gameAreaOffsetY,
                gameAreaOffsetX + gameAreaWidth,
                gameAreaOffsetY + gameAreaHeight
        );
        canvas.drawRect(borderRect, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);

        if (isGolden) {
            paint.setColor(Color.parseColor("#FFD700"));
        } else {
            // Manzana blanca en noche, negra en día
            paint.setColor(nightMode ? Color.WHITE : Color.BLACK);
        }
        Rect foodRect = new Rect(
                gameAreaOffsetX + food.x * dynamicBlockSize,
                gameAreaOffsetY + food.y * dynamicBlockSize,
                gameAreaOffsetX + (food.x + 1) * dynamicBlockSize,
                gameAreaOffsetY + (food.y + 1) * dynamicBlockSize
        );
        canvas.drawRect(foodRect, paint);

        equippedSkin = prefs.getString("equipped_skin", "skin_default");

        for (int i = 0; i < snake.size(); i++) {
            Point segment = snake.get(i);
            if (i == 0) {
                if ("skin_red".equals(equippedSkin)) {
                    paint.setColor(Color.parseColor("#FF4444"));
                } else if ("skin_blue".equals(equippedSkin)) {
                    paint.setColor(Color.parseColor("#448AFF"));
                } else {
                    paint.setColor(Color.GREEN);
                }
            } else {
                if ("skin_red".equals(equippedSkin)) {
                    paint.setColor(Color.parseColor("#B71C1C"));
                } else if ("skin_blue".equals(equippedSkin)) {
                    paint.setColor(Color.parseColor("#0D47A1"));
                } else {
                    paint.setColor(Color.rgb(0, 150, 0));
                }
            }

            Rect segmentRect = new Rect(
                    gameAreaOffsetX + segment.x * dynamicBlockSize,
                    gameAreaOffsetY + segment.y * dynamicBlockSize,
                    gameAreaOffsetX + (segment.x + 1) * dynamicBlockSize,
                    gameAreaOffsetY + (segment.y + 1) * dynamicBlockSize
            );
            canvas.drawRect(segmentRect, paint);

            // Borde de la serpiente: gris oscuro en noche, gris claro en día
            paint.setColor(nightMode ? Color.DKGRAY : Color.LTGRAY);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            canvas.drawRect(segmentRect, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        if (gameOver) {
            drawGameOver(canvas);
        }

        drawGameInfo(canvas);
    }

    private void drawBackgroundWithTransition(Canvas canvas) {
        Rect destRect = new Rect(
                gameAreaOffsetX, gameAreaOffsetY,
                gameAreaOffsetX + gameAreaWidth,
                gameAreaOffsetY + gameAreaHeight
        );

        if (transitionInProgress) {
            int offset = (int) (gameAreaHeight * transitionProgress);
            android.graphics.Bitmap currentBg = BitmapFactory.decodeResource(getResources(), R.mipmap.fondolvl1);
            if (currentBg != null) {
                Rect currentRect = new Rect(
                        gameAreaOffsetX,
                        gameAreaOffsetY - offset,
                        gameAreaOffsetX + gameAreaWidth,
                        gameAreaOffsetY + gameAreaHeight - offset
                );
                canvas.drawBitmap(currentBg, null, currentRect, paint);
            }
            android.graphics.Bitmap targetBg = BitmapFactory.decodeResource(getResources(), R.mipmap.fondo_marte);
            if (targetBg != null) {
                Rect targetRect = new Rect(
                        gameAreaOffsetX,
                        gameAreaOffsetY + (gameAreaHeight - offset),
                        gameAreaOffsetX + gameAreaWidth,
                        gameAreaOffsetY + gameAreaHeight + (gameAreaHeight - offset)
                );
                canvas.drawBitmap(targetBg, null, targetRect, paint);
            }
        } else {
            android.graphics.Bitmap backgroundBitmap = BitmapFactory.decodeResource(getResources(), currentBackground);
            if (backgroundBitmap != null) {
                Paint backgroundPaint = new Paint();
                backgroundPaint.setAlpha(100);
                canvas.drawBitmap(backgroundBitmap, null, destRect, backgroundPaint);
            } else {
                canvas.drawColor(Color.BLACK);
            }
        }
    }

    private void drawGameOver(Canvas canvas) {
        paint.setColor(Color.RED);
        paint.setTextSize(60);
        paint.setStyle(Paint.Style.FILL);
        Typeface tf = ResourcesCompat.getFont(getContext(), R.font.vcr_osd_mono_1_001);
        if (tf != null) {
            paint.setTypeface(tf);
        } else {
            paint.setTypeface(Typeface.MONOSPACE);
        }

        String gameOverText = "GAME OVER";
        float textWidth = paint.measureText(gameOverText);
        canvas.drawText(gameOverText, (getWidth() - textWidth) / 2, getHeight() / 2, paint);

        paint.setTextSize(30);
        String restartText = "Toca para reiniciar";
        float restartWidth = paint.measureText(restartText);
        canvas.drawText(restartText, (getWidth() - restartWidth) / 2, getHeight() / 2 + 50, paint);
    }

    private void drawGameInfo(Canvas canvas) {
        // Texto: blanco en noche, negro en día
        paint.setColor(nightMode ? Color.WHITE : Color.BLACK);
        paint.setTextSize(36);
        Typeface tf = ResourcesCompat.getFont(getContext(), R.font.vcr_osd_mono_1_001);
        if (tf != null) {
            paint.setTypeface(tf);
        } else {
            paint.setTypeface(Typeface.MONOSPACE);
        }
        canvas.drawText("PUNTUACIÓN: " + score, 50, 50, paint);

        paint.setTextSize(20);
        canvas.drawText("Come las manzanas!", 50, 90, paint);
    }

    public void restartGame() {
        initGame();
    }

    public void setDirectionUp() {
        if (currentDirection != Direction.DOWN && !gameOver) {
            nextDirection = Direction.UP;
        }
    }

    public void setDirectionDown() {
        if (currentDirection != Direction.UP && !gameOver) {
            nextDirection = Direction.DOWN;
        }
    }

    public void setDirectionLeft() {
        if (currentDirection != Direction.RIGHT && !gameOver) {
            nextDirection = Direction.LEFT;
        }
    }

    public void setDirectionRight() {
        if (currentDirection != Direction.LEFT && !gameOver) {
            nextDirection = Direction.RIGHT;
        }
    }

    public int getScore() {
        return score;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void stopGame() {
        running = false;
        if (transitionSound != null) {
            transitionSound.release();
            transitionSound = null;
        }
    }
}
