package com.example.test_snake;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import android.graphics.BitmapFactory;

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
    private Direction nextDirection    = Direction.RIGHT;

    // Comida
    private Point food;

    // Tiempo
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 200; // ms entre movimientos

    // Sistema de monedas
    private int coins = 0;             // Cantidad de monedas acumuladas
    private boolean isGolden = false;  // Marca si la manzana actual es dorada (vale 5 monedas)
    private Random random = new Random();
    private SharedPreferences prefs;   // Preferencias para persistir las monedas y la skin equipada
    private String equippedSkin;       // Skin actualmente equipada (default, azul, roja)

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

        // Inicializar preferencias y cargar monedas y skin guardadas
        prefs = getContext().getSharedPreferences("SnakePrefs", Context.MODE_PRIVATE);
        coins = prefs.getInt("coins", 0);
        equippedSkin = prefs.getString("equipped_skin", "skin_default");

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

        score            = 0;
        currentDirection = Direction.RIGHT;
        nextDirection    = Direction.RIGHT;
    }

    private void generateFood() {
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
        // Determinar si la fruta es dorada (20% de probabilidad)
        isGolden = random.nextFloat() < 0.2;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running    = true;
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
                Thread.sleep(12); // 60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateGame() {
        // Actualizar dirección con la siguiente dirección pendiente
        currentDirection = nextDirection;

        // Mover serpiente - obtener la cabeza actual y calcular nueva posición
        Point head = new Point(snake.get(0));
        switch (currentDirection) {
            case UP:    head.y--; break;
            case DOWN:  head.y++; break;
            case LEFT:  head.x--; break;
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

        // Agregar nueva cabeza a la serpiente
        snake.add(0, head);

        // Verificar si comió comida
        if (head.equals(food)) {
            // Incrementar puntuación (mantiene compatibilidad con el juego original)
            score += 10;
            // Actualizar monedas según el tipo de manzana
            coins += isGolden ? 5 : 1;
            // Guardar monedas en SharedPreferences
            prefs.edit().putInt("coins", coins).apply();
            generateFood();
            // No remover cola para hacer crecer la serpiente
        } else {
            // Remover cola si no comió (mantener mismo tamaño)
            snake.remove(snake.size() - 1);
        }
    }

    private void gameOver() {
        // Reiniciar juego (las monedas acumuladas se conservan gracias a SharedPreferences)
        initGame();
    }

    private void drawGame(Canvas canvas) {
        // Fondo negro completo
        canvas.drawColor(Color.BLACK);

        // CALCULO MODIFICADO para área de juego más grande
        int availableWidth  = getWidth() - 300; // Dejar 300px para controles + margen
        int availableHeight = getHeight() - 40; // Dejar margen superior e inferior

        // Calcular el tamaño máximo que quepa en el espacio disponible
        int maxGridSize = Math.min(availableWidth, availableHeight);

        // Calcular BLOCK_SIZE dinámico basado en el espacio disponible
        int dynamicBlockSize = maxGridSize / Math.max(GRID_WIDTH, GRID_HEIGHT);

        int gridWidthPx  = GRID_WIDTH  * dynamicBlockSize;
        int gridHeightPx = GRID_HEIGHT * dynamicBlockSize;

        // Centrar el área de juego
        int offsetX = (getWidth() - gridWidthPx - 300) / 2;
        int offsetY = (getHeight() - gridHeightPx) / 2;

        // Dibujar imagen de fondo con opacidad dentro del área de juego
        try {
            android.graphics.Bitmap backgroundBitmap =
                    BitmapFactory.decodeResource(getResources(), R.mipmap.fondolvl1);
            if (backgroundBitmap != null) {
                Paint backgroundPaint = new Paint();
                backgroundPaint.setAlpha(100); // 40% de opacidad
                Rect destRect = new Rect(
                        offsetX, offsetY,
                        offsetX + gridWidthPx,
                        offsetY + gridHeightPx
                );
                canvas.drawBitmap(backgroundBitmap, null, destRect, backgroundPaint);
            }
        } catch (Exception e) {
            canvas.drawColor(Color.BLACK);
        }

        // Dibujar bordes blancos
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setAlpha(255);

        Rect borderRect = new Rect(
                offsetX,
                offsetY,
                offsetX + gridWidthPx,
                offsetY + gridHeightPx
        );
        canvas.drawRect(borderRect, paint);

        // Volver al estilo de relleno
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);

        // Dibujar comida: dorada o roja según isGolden
        if (isGolden) {
            paint.setColor(Color.parseColor("#FFD700")); // dorada
        } else {
            paint.setColor(Color.RED); // roja
        }
        Rect foodRect = new Rect(
                offsetX + food.x * dynamicBlockSize,
                offsetY + food.y * dynamicBlockSize,
                offsetX + (food.x + 1) * dynamicBlockSize,
                offsetY + (food.y + 1) * dynamicBlockSize
        );
        canvas.drawRect(foodRect, paint);

        // Actualizar la skin equipada (por si cambió en la tienda)
        equippedSkin = prefs.getString("equipped_skin", "skin_default");

        // Dibujar serpiente con colores según la skin equipada
        for (int i = 0; i < snake.size(); i++) {
            Point segment = snake.get(i);
            if (i == 0) {
                // Cabeza
                if ("skin_red".equals(equippedSkin)) {
                    paint.setColor(Color.parseColor("#FF4444")); // Rojo claro
                } else if ("skin_blue".equals(equippedSkin)) {
                    paint.setColor(Color.parseColor("#448AFF")); // Azul claro
                } else {
                    paint.setColor(Color.GREEN); // Verde por defecto
                }
            } else {
                // Cuerpo
                if ("skin_red".equals(equippedSkin)) {
                    paint.setColor(Color.parseColor("#B71C1C")); // Rojo oscuro
                } else if ("skin_blue".equals(equippedSkin)) {
                    paint.setColor(Color.parseColor("#0D47A1")); // Azul oscuro
                } else {
                    paint.setColor(Color.rgb(0, 150, 0)); // Verde oscuro
                }
            }

            Rect segmentRect = new Rect(
                    offsetX + segment.x * dynamicBlockSize,
                    offsetY + segment.y * dynamicBlockSize,
                    offsetX + (segment.x + 1) * dynamicBlockSize,
                    offsetY + (segment.y + 1) * dynamicBlockSize
            );
            canvas.drawRect(segmentRect, paint);

            // Dibujar borde del segmento
            paint.setColor(Color.DKGRAY);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            canvas.drawRect(segmentRect, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        // Dibujar información (puntuación)
        drawGameInfo(canvas);
    }

    private void drawGameInfo(Canvas canvas) {
        paint.setColor(Color.WHITE);
        paint.setTextSize(36);

        // Usar fuente personalizada o monospace de respaldo
        try {
            paint.setTypeface(getResources().getFont(R.font.vcr_osd_mono_1_001));
        } catch (Exception e) {
            paint.setTypeface(Typeface.MONOSPACE);
        }

        // Puntuación
        canvas.drawText("PUNTUACIÓN: " + score, 50, 50, paint);

        // Instrucciones
        paint.setTextSize(20);
        canvas.drawText("Come las manzanas!", 50, 90, paint);
    }

    // Métodos para controlar la dirección de la serpiente
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
