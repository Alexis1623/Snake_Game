package com.example.test_snake;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private Thread gameThread;
    private boolean running = false;
    private SurfaceHolder holder;
    private Paint paint;

    // Variables del juego
    private int score = 0;
    private static final int BLOCK_SIZE = 40;
    private static final int GRID_WIDTH = 20;
    private static final int GRID_HEIGHT = 15;

    // Serpiente
    private List<Point> snake;
    private Direction currentDirection = Direction.RIGHT;
    private Direction nextDirection = Direction.RIGHT;

    // Comida
    private Point food;

    // Tiempo
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 200; // ms entre movimientos

    // Direcciones
    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    // Constructores
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
        initGame();
    }

    private void initGame() {
        // Inicializar serpiente en el centro
        snake = new ArrayList<>();
        snake.add(new Point(GRID_WIDTH / 2, GRID_HEIGHT / 2));
        snake.add(new Point(GRID_WIDTH / 2 - 1, GRID_HEIGHT / 2));
        snake.add(new Point(GRID_WIDTH / 2 - 2, GRID_HEIGHT / 2));

        // Generar primera comida
        generateFood();

        score = 0;
        currentDirection = Direction.RIGHT;
        nextDirection = Direction.RIGHT;
    }

    private void generateFood() {
        Random random = new Random();
        while (true) {
            int x = random.nextInt(GRID_WIDTH);
            int y = random.nextInt(GRID_HEIGHT);
            food = new Point(x, y);

            // Verificar que la comida no esté en la serpiente
            boolean collision = false;
            for (Point segment : snake) {
                if (segment.equals(food)) {
                    collision = true;
                    break;
                }
            }

            if (!collision) break;
        }
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
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void run() {
        while (running) {
            if (!holder.getSurface().isValid()) continue;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime > UPDATE_INTERVAL) {
                updateGame();
                lastUpdateTime = currentTime;
            }

            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                drawGame(canvas);
                holder.unlockCanvasAndPost(canvas);
            }

            try {
                Thread.sleep(10); // <-- Descanso pequeño (10-16 ms)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private void updateGame() {
        // Actualizar dirección
        currentDirection = nextDirection;

        // Mover serpiente
        Point head = new Point(snake.get(0));
        switch (currentDirection) {
            case UP: head.y--; break;
            case DOWN: head.y++; break;
            case LEFT: head.x--; break;
            case RIGHT: head.x++; break;
        }

        // Verificar colisiones con bordes
        if (head.x < 0 || head.x >= GRID_WIDTH || head.y < 0 || head.y >= GRID_HEIGHT) {
            gameOver();
            return;
        }

        // Verificar colisión con sí misma
        for (int i = 1; i < snake.size(); i++) {
            if (head.equals(snake.get(i))) {
                gameOver();
                return;
            }
        }

        // Agregar nueva cabeza
        snake.add(0, head);

        // Verificar si comió comida
        if (head.equals(food)) {
            score += 10;
            generateFood();
            // No remover cola para hacer crecer la serpiente
        } else {
            // Remover cola si no comió
            snake.remove(snake.size() - 1);
        }
    }

    private void gameOver() {
        // Reiniciar juego (SIN guardar puntuación)
        initGame();
    }


    private void drawGame(Canvas canvas) {
        // Fondo
        canvas.drawColor(Color.BLACK);

        // Calcular margenes para centrar el juego
        int gridWidthPx = GRID_WIDTH * BLOCK_SIZE;
        int gridHeightPx = GRID_HEIGHT * BLOCK_SIZE;
        int offsetX = (getWidth() - gridWidthPx - 250) / 2; // Dejar espacio para controles
        int offsetY = (getHeight() - gridHeightPx) / 2;

        // Dibujar comida (punto blanco)
        paint.setColor(Color.WHITE);
        Rect foodRect = new Rect(
                offsetX + food.x * BLOCK_SIZE,
                offsetY + food.y * BLOCK_SIZE,
                offsetX + (food.x + 1) * BLOCK_SIZE,
                offsetY + (food.y + 1) * BLOCK_SIZE
        );
        canvas.drawRect(foodRect, paint);

        // Dibujar serpiente
        for (int i = 0; i < snake.size(); i++) {
            Point segment = snake.get(i);

            // Cabeza verde
            if (i == 0) {
                paint.setColor(Color.GREEN);
            } else {
                // Cuerpo verde más oscuro
                paint.setColor(Color.rgb(0, 150, 0));
            }

            Rect segmentRect = new Rect(
                    offsetX + segment.x * BLOCK_SIZE,
                    offsetY + segment.y * BLOCK_SIZE,
                    offsetX + (segment.x + 1) * BLOCK_SIZE,
                    offsetY + (segment.y + 1) * BLOCK_SIZE
            );
            canvas.drawRect(segmentRect, paint);

            // Bordes de los segmentos
            paint.setColor(Color.DKGRAY);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            canvas.drawRect(segmentRect, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        // Dibujar información
        drawGameInfo(canvas);
    }

    private void drawGameInfo(Canvas canvas) {
        paint.setColor(Color.WHITE);
        paint.setTextSize(36);
        paint.setTypeface(getResources().getFont(R.font.vcr_osd_mono_1_001));

        // Puntuación
        canvas.drawText("PUNTUACIÓN: " + score, 50, 50, paint);

        // Instrucciones
        paint.setTextSize(20);
        canvas.drawText("Come los puntos blancos!", 50, 90, paint);
    }

    // Métodos para controlar la dirección
    public void setDirectionUp() {
        if (currentDirection != Direction.DOWN) {
            nextDirection = Direction.UP;
        }
    }

    public void setDirectionDown() {
        if (currentDirection != Direction.UP) {
            nextDirection = Direction.DOWN;
        }
    }

    public void setDirectionLeft() {
        if (currentDirection != Direction.RIGHT) {
            nextDirection = Direction.LEFT;
        }
    }

    public void setDirectionRight() {
        if (currentDirection != Direction.LEFT) {
            nextDirection = Direction.RIGHT;
        }
    }

    public int getScore() {
        return score;
    }

    public void stopGame() {
        running = false;
    }
}