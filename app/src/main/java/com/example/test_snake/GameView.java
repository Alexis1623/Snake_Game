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

    // Meta del juego
    private static final int WIN_SCORE = 350;

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
    private boolean gameWon = false; // Nuevo: estado de victoria

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
    private MediaPlayer winSound;

    // ============ SISTEMA DE ENEMIGOS ============
    private List<EnemySnake> enemies;
    private int maxEnemies = 0;
    private boolean enemiesActive = false;

    // ============ SISTEMA DE ASTEROIDES ============
    private List<Asteroid> asteroids;
    private int maxAsteroids = 0;
    private boolean asteroidsActive = false;

    // ============ CLASE ASTEROIDE ============
    private class Asteroid {
        Point position;
        int speed;
        int directionX; // -1 izquierda, 1 derecha, 0 estático
        int directionY; // -1 arriba, 1 abajo, 0 estático
        int size; // 1 = pequeño, 2 = mediano, 3 = grande

        Asteroid(Point pos, int speed, int dirX, int dirY, int size) {
            this.position = pos;
            this.speed = speed;
            this.directionX = dirX;
            this.directionY = dirY;
            this.size = size;
        }

        void move() {
            for (int i = 0; i < speed; i++) {
                position.x += directionX;
                position.y += directionY;

                // Rebotar en los bordes
                if (position.x <= 0 || position.x >= GRID_WIDTH - 1) {
                    directionX *= -1;
                    position.x = Math.max(0, Math.min(GRID_WIDTH - 1, position.x));
                }

                if (position.y <= 0 || position.y >= GRID_HEIGHT - 1) {
                    directionY *= -1;
                    position.y = Math.max(0, Math.min(GRID_HEIGHT - 1, position.y));
                }
            }
        }

        boolean collidesWith(Point point) {
            // Colisión basada en el tamaño del asteroide
            int halfSize = size / 2;
            return point.x >= position.x - halfSize &&
                    point.x <= position.x + halfSize &&
                    point.y >= position.y - halfSize &&
                    point.y <= position.y + halfSize;
        }

        void draw(Canvas canvas) {
            // Color de asteroide (grises)
            paint.setColor(Color.parseColor("#808080"));

            // Dibujar asteroide redondeado - CORREGIDO: agregar casting a int
            int centerX = gameAreaOffsetX + (int)((position.x + 0.5f) * dynamicBlockSize);
            int centerY = gameAreaOffsetY + (int)((position.y + 0.5f) * dynamicBlockSize);
            int radius = dynamicBlockSize * size / 3;

            canvas.drawCircle(centerX, centerY, radius, paint);

            // Detalles del asteroide
            paint.setColor(Color.DKGRAY);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            canvas.drawCircle(centerX, centerY, radius, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    // ============ ANIMACIÓN DE QUEMARSE ============
    private BurnAnimation burnAnimation;
    private boolean isBurning = false;
    private long burnStartTime = 0;
    private static final long BURN_DURATION = 1000; // 1 segundo de animación
    private MediaPlayer burnSound;

    // ============ CLASE ENEMY SNAKE (MÁS LARGA) ============
    private class EnemySnake {
        List<Point> body;
        Direction direction;
        boolean isVertical;
        int speedCounter = 0;
        int speedDelay = 3; // Más lento que el jugador
        int length = 3; // Longitud inicial del enemigo

        EnemySnake(Point startPos, boolean vertical) {
            this.body = new ArrayList<>();
            this.isVertical = vertical;

            // Determinar dirección inicial
            if (vertical) {
                this.direction = random.nextBoolean() ? Direction.DOWN : Direction.UP;
                // Crear cuerpo vertical
                for (int i = 0; i < length; i++) {
                    if (direction == Direction.DOWN) {
                        body.add(new Point(startPos.x, startPos.y - i));
                    } else {
                        body.add(new Point(startPos.x, startPos.y + i));
                    }
                }
            } else {
                this.direction = random.nextBoolean() ? Direction.RIGHT : Direction.LEFT;
                // Crear cuerpo horizontal
                for (int i = 0; i < length; i++) {
                    if (direction == Direction.RIGHT) {
                        body.add(new Point(startPos.x - i, startPos.y));
                    } else {
                        body.add(new Point(startPos.x + i, startPos.y));
                    }
                }
            }
        }

        void move() {
            speedCounter++;
            if (speedCounter < speedDelay) return;
            speedCounter = 0;

            // Mover la cabeza según dirección
            Point head = new Point(body.get(0));
            switch (direction) {
                case UP:
                    head.y--;
                    if (head.y < 0) {
                        head.y = GRID_HEIGHT - 1;
                        // Cambiar dirección aleatoriamente al cruzar bordes
                        if (random.nextBoolean()) {
                            direction = Direction.DOWN;
                        } else if (random.nextBoolean() && !isVertical) {
                            direction = random.nextBoolean() ? Direction.LEFT : Direction.RIGHT;
                            isVertical = false;
                        }
                    }
                    break;
                case DOWN:
                    head.y++;
                    if (head.y >= GRID_HEIGHT) {
                        head.y = 0;
                        if (random.nextBoolean()) {
                            direction = Direction.UP;
                        } else if (random.nextBoolean() && !isVertical) {
                            direction = random.nextBoolean() ? Direction.LEFT : Direction.RIGHT;
                            isVertical = false;
                        }
                    }
                    break;
                case LEFT:
                    head.x--;
                    if (head.x < 0) {
                        head.x = GRID_WIDTH - 1;
                        if (random.nextBoolean()) {
                            direction = Direction.RIGHT;
                        } else if (random.nextBoolean() && isVertical) {
                            direction = random.nextBoolean() ? Direction.UP : Direction.DOWN;
                            isVertical = true;
                        }
                    }
                    break;
                case RIGHT:
                    head.x++;
                    if (head.x >= GRID_WIDTH) {
                        head.x = 0;
                        if (random.nextBoolean()) {
                            direction = Direction.LEFT;
                        } else if (random.nextBoolean() && isVertical) {
                            direction = random.nextBoolean() ? Direction.UP : Direction.DOWN;
                            isVertical = true;
                        }
                    }
                    break;
            }

            // Insertar nueva cabeza
            body.add(0, head);

            // Mantener longitud fija (remover cola)
            if (body.size() > length) {
                body.remove(body.size() - 1);
            }
        }

        // Verificar colisión con la serpiente del jugador
        boolean collidesWith(Point point) {
            for (Point segment : body) {
                if (segment.equals(point)) {
                    return true;
                }
            }
            return false;
        }

        // Verificar colisión con otro enemigo
        boolean collidesWithEnemy(EnemySnake other) {
            for (Point segment : body) {
                if (other.collidesWith(segment)) {
                    return true;
                }
            }
            return false;
        }
    }

    // ============ CLASE ANIMACIÓN DE QUEMARSE ============
    private class BurnAnimation {
        Point position;
        float progress = 0f; // 0 a 1
        int particleCount = 25;
        List<Particle> particles = new ArrayList<>();

        BurnAnimation(Point pos) {
            this.position = pos;
            createParticles();
        }

        void createParticles() {
            particles.clear();
            for (int i = 0; i < particleCount; i++) {
                particles.add(new Particle(
                        position.x + 0.5f,
                        position.y + 0.5f,
                        random.nextFloat() * 360,
                        random.nextFloat() * 3 + 1,
                        random.nextFloat() * 0.7f + 0.3f
                ));
            }
        }

        void update(float deltaTime) {
            progress += deltaTime / (BURN_DURATION / 1000f);
            if (progress > 1f) progress = 1f;

            for (Particle particle : particles) {
                particle.update(deltaTime);
            }
        }

        boolean isFinished() {
            return progress >= 1f;
        }

        void draw(Canvas canvas) {
            // Dibujar partículas de fuego
            for (Particle particle : particles) {
                particle.draw(canvas);
            }

            // Dibujar explosión central
            float explosionProgress = Math.min(progress * 2, 1f);
            if (explosionProgress < 1f) {
                float explosionSize = explosionProgress * dynamicBlockSize * 1.5f;
                paint.setColor(Color.YELLOW);
                paint.setAlpha((int)(255 * (1 - explosionProgress)));
                canvas.drawCircle(
                        gameAreaOffsetX + (position.x + 0.5f) * dynamicBlockSize,
                        gameAreaOffsetY + (position.y + 0.5f) * dynamicBlockSize,
                        explosionSize / 2,
                        paint
                );
                paint.setAlpha(255);
            }
        }
    }

    // ============ CLASE PARTÍCULAS DE FUEGO ============
    private class Particle {
        float x, y;
        float angle;
        float speed;
        float life;
        float originalLife;

        Particle(float startX, float startY, float angle, float speed, float life) {
            this.x = startX;
            this.y = startY;
            this.angle = angle;
            this.speed = speed;
            this.life = life;
            this.originalLife = life;
        }

        void update(float deltaTime) {
            life -= deltaTime;
            if (life <= 0) return;

            float rad = (float)Math.toRadians(angle);
            x += Math.cos(rad) * speed * deltaTime * 8;
            y += Math.sin(rad) * speed * deltaTime * 8;

            // Gravedad leve
            y += deltaTime * 2;
        }

        void draw(Canvas canvas) {
            if (life <= 0) return;

            float progress = life / originalLife;
            int alpha = (int)(255 * progress);
            int size = (int)(dynamicBlockSize * 0.4f * progress);

            // Color de fuego (naranja a rojo)
            int r = 255;
            int g = (int)(80 + 175 * (1 - progress));
            int b = 0;

            paint.setColor(Color.argb(alpha, r, g, b));
            paint.setStyle(Paint.Style.FILL);

            canvas.drawCircle(
                    gameAreaOffsetX + x * dynamicBlockSize,
                    gameAreaOffsetY + y * dynamicBlockSize,
                    size / 2,
                    paint
            );
        }
    }

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
        nightMode = prefs.getBoolean("night_mode", true);

        coinsRef = FirebaseDatabase.getInstance()
                .getReference("user_coins")
                .child(username);

        initTransitionSound();
        initBurnSound();
        initWinSound();

        enemies = new ArrayList<>();
        asteroids = new ArrayList<>();

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

    private void initWinSound() {
        try {
            winSound = MediaPlayer.create(getContext(), R.raw.win_sound); // Necesitarás agregar este archivo
            if (winSound != null) {
                winSound.setVolume(1.0f, 1.0f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initBurnSound() {
        try {
            burnSound = MediaPlayer.create(getContext(), R.raw.explosion);
            if (burnSound != null) {
                burnSound.setVolume(1.0f, 1.0f);
                burnSound.setOnCompletionListener(mp -> {
                    // No hacer nada al completar
                });
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

        enemies.clear();
        asteroids.clear();
        maxEnemies = 0;
        maxAsteroids = 0;
        enemiesActive = false;
        asteroidsActive = false;
        isBurning = false;
        burnAnimation = null;

        score = 0;
        gameOver = false;
        gameWon = false;
        currentDirection = Direction.RIGHT;
        nextDirection = Direction.RIGHT;

        currentBackground = R.mipmap.fondolvl1;
        backgroundChanged = false;
        transitionInProgress = false;
        transitionProgress = 0f;
        soundPlayed = false;
    }

    private void generateFood() {
        int attempts = 0;
        while (attempts < 100) {
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

            // Verificar colisión con enemigos
            if (!collision && enemiesActive) {
                for (EnemySnake enemy : enemies) {
                    if (enemy.collidesWith(food)) {
                        collision = true;
                        break;
                    }
                }
            }

            // Verificar colisión con asteroides
            if (!collision && asteroidsActive) {
                for (Asteroid asteroid : asteroids) {
                    if (asteroid.collidesWith(food)) {
                        collision = true;
                        break;
                    }
                }
            }

            if (!collision) return;
            attempts++;
        }
        isGolden = random.nextFloat() < 0.2;
    }

    private void generateAsteroid() {
        int attempts = 0;
        while (attempts < 50) {
            int x = random.nextInt(GRID_WIDTH);
            int y = random.nextInt(GRID_HEIGHT);
            Point pos = new Point(x, y);

            // Verificar que no esté muy cerca de la serpiente
            boolean tooClose = false;
            for (Point segment : snake) {
                int distance = Math.abs(segment.x - x) + Math.abs(segment.y - y);
                if (distance < 3) {
                    tooClose = true;
                    break;
                }
            }

            if (tooClose) {
                attempts++;
                continue;
            }

            // Verificar colisión con comida
            if (food != null && (Math.abs(food.x - x) < 2 && Math.abs(food.y - y) < 2)) {
                attempts++;
                continue;
            }

            // Crear asteroide con propiedades aleatorias
            int speed = random.nextInt(2) + 1; // 1-2
            int dirX = random.nextInt(3) - 1; // -1, 0, 1
            int dirY = random.nextInt(3) - 1; // -1, 0, 1
            int size = random.nextInt(3) + 1; // 1-3

            // Asegurar que se mueva en alguna dirección
            if (dirX == 0 && dirY == 0) {
                dirX = random.nextBoolean() ? 1 : -1;
            }

            Asteroid asteroid = new Asteroid(pos, speed, dirX, dirY, size);
            asteroids.add(asteroid);
            return;
        }
    }

    private void generateEnemy() {
        int attempts = 0;
        while (attempts < 50) {
            // POSICIÓN ALEATORIA EN CUALQUIER PARTE DEL MAPA (no solo abajo)
            int startX, startY;
            boolean vertical = random.nextBoolean();

            if (vertical) {
                // Enemigo vertical - puede aparecer en cualquier columna
                startX = random.nextInt(GRID_WIDTH);
                // Aparece en borde superior o inferior
                startY = random.nextBoolean() ? 0 : GRID_HEIGHT - 1;
            } else {
                // Enemigo horizontal - puede aparecer en cualquier fila
                startY = random.nextInt(GRID_HEIGHT);
                // Aparece en borde izquierdo o derecho
                startX = random.nextBoolean() ? 0 : GRID_WIDTH - 1;
            }

            Point startPos = new Point(startX, startY);
            EnemySnake newEnemy = new EnemySnake(startPos, vertical);

            // Verificar colisiones
            boolean collision = false;

            // Con la serpiente del jugador
            for (Point segment : snake) {
                if (newEnemy.collidesWith(segment)) {
                    collision = true;
                    break;
                }
            }

            // Con la comida
            if (newEnemy.collidesWith(food)) {
                collision = true;
            }

            // Con asteroides
            for (Asteroid asteroid : asteroids) {
                if (asteroid.collidesWith(startPos)) {
                    collision = true;
                    break;
                }
            }

            // Con otros enemigos
            if (!collision) {
                for (EnemySnake existingEnemy : enemies) {
                    if (newEnemy.collidesWithEnemy(existingEnemy)) {
                        collision = true;
                        break;
                    }
                }
            }

            if (!collision) {
                enemies.add(newEnemy);
                return;
            }

            attempts++;
        }
    }


    private void manageEnemiesAndAsteroids() {
        // GESTIÓN DE ENEMIGOS EXISTENTES (30-90 puntos) - NO SOBREESCRIBIR
        if (score >= 30 && score < 130) {
            if (!enemiesActive) {
                enemiesActive = true;
            }

            // Enemigos progresivos del sistema original
            if (score >= 30 && maxEnemies < 1) {
                maxEnemies = 1;
            }

            if (score >= 90 && maxEnemies < 2) {
                maxEnemies = 2;
            }
        }

        // GESTIÓN DE ENEMIGOS (130-180 puntos) - FASE 2
        if (score >= 130 && score <= 180) {
            if (!enemiesActive) {
                enemiesActive = true;
            }

            // Aumentar enemigos progresivamente en esta fase
            if (score >= 130 && score < 150 && maxEnemies < 3) {
                maxEnemies = 3;
            }

            if (score >= 150 && score < 170 && maxEnemies < 5) {
                maxEnemies = 5;
            }

            if (score >= 170 && maxEnemies < 7) {
                maxEnemies = 7;
            }
        }

        // DESACTIVAR ENEMIGOS AL ENTRAR EN FASE DE ASTEROIDES
        if (score > 180 && score <= 230) {
            enemiesActive = false;
            maxEnemies = 0;
        }

        // GESTIÓN DE ASTEROIDES (180-230 puntos)
        if (score >= 180 && score <= 230) {
            if (!asteroidsActive) {
                asteroidsActive = true;
                maxAsteroids = 3;
            }

            // Aumentar asteroides progresivamente
            if (score >= 200 && score < 220 && maxAsteroids < 5) {
                maxAsteroids = 5;
            }

            if (score >= 220 && maxAsteroids < 7) {
                maxAsteroids = 7;
            }
        } else if (score > 230) {
            asteroidsActive = false;
            maxAsteroids = 0;
        }

        // VICTORIA (500 puntos)
        if (score >= WIN_SCORE && !gameWon) {
            gameWon = true;
            playWinSound();
        }

        // Generar enemigos si están activos
        while (enemies.size() < maxEnemies && enemiesActive) {
            generateEnemy();
        }

        // Generar asteroides si están activos
        while (asteroids.size() < maxAsteroids && asteroidsActive) {
            generateAsteroid();
        }
    }
    private void playWinSound() {
        if (winSound != null) {
            try {
                winSound.seekTo(0);
                winSound.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startBurnAnimation(Point position) {
        isBurning = true;
        burnStartTime = System.currentTimeMillis();
        burnAnimation = new BurnAnimation(position);

        // REPRODUCIR SONIDO DE EXPLOSIÓN - CORREGIDO
        if (burnSound != null) {
            try {
                // Detener si está sonando
                if (burnSound.isPlaying()) {
                    burnSound.stop();
                }
                // Resetear y reproducir
                burnSound.seekTo(0);
                burnSound.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateBurnAnimation() {
        if (!isBurning || burnAnimation == null) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - burnStartTime) / 1000f;
        burnAnimation.update(deltaTime);

        if (burnAnimation.isFinished()) {
            isBurning = false;
            burnAnimation = null;
            gameOver = true; // Game Over después de la animación
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
        if (transitionSound != null) {
            transitionSound.release();
            transitionSound = null;
        }
        if (burnSound != null) {
            burnSound.release();
            burnSound = null;
        }
        if (winSound != null) {
            winSound.release();
            winSound = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        calculateGameArea();
    }

    private void calculateGameArea() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;

        // Panel de controles compacto
        int controlsAreaWidth = (int) (100 * density); // Muy compacto
        int margin = (int) (2 * density); // Margen mínimo

        // Calcular espacio disponible (90% del ancho total para el juego)
        int totalAvailableWidth = getWidth() - controlsAreaWidth;
        int gameWidth = (int) (totalAvailableWidth * 2); // 95% del espacio
        int gameHeight = (int) (getHeight() * 0.90); // 90% de la altura

        // Calcular tamaño de bloque
        dynamicBlockSize = Math.min(gameWidth / GRID_WIDTH, gameHeight / GRID_HEIGHT);

        // Asegurar tamaño mínimo decente
        if (dynamicBlockSize < 40) {
            dynamicBlockSize = 40;
        }

        // Dimensiones finales
        gameAreaWidth = GRID_WIDTH * dynamicBlockSize;
        gameAreaHeight = GRID_HEIGHT * dynamicBlockSize;

        // Posición: alineado a la izquierda con margen mínimo
        gameAreaOffsetX = margin;
        // Centrar verticalmente
        gameAreaOffsetY = (getHeight() - gameAreaHeight) / 2;
    }

    @Override
    public void run() {
        while (running) {
            if (!holder.getSurface().isValid()) continue;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime > UPDATE_INTERVAL) {
                if (!gameOver && !gameWon) {
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
        // Actualizar animación de quemarse si está activa
        if (isBurning) {
            updateBurnAnimation();
            return; // Pausar el juego durante la animación
        }

        currentDirection = nextDirection;

        // Gestionar enemigos y asteroides
        manageEnemiesAndAsteroids();

        // Mover enemigos
        for (EnemySnake enemy : enemies) {
            enemy.move();
        }

        // Mover asteroides
        for (Asteroid asteroid : asteroids) {
            asteroid.move();
        }

        // Verificar colisión con enemigos
        Point head = snake.get(0);
        for (EnemySnake enemy : enemies) {
            if (enemy.collidesWith(head)) {
                // COLISIÓN DETECTADA - Iniciar animación de quemarse
                startBurnAnimation(head);
                return;
            }
        }

        // Verificar colisión con asteroides
        for (Asteroid asteroid : asteroids) {
            if (asteroid.collidesWith(head)) {
                startBurnAnimation(head);
                return;
            }
        }

        if (!backgroundChanged && !transitionInProgress && score >= 50) {
            startBackgroundTransition();
        }

        if (transitionInProgress) {
            updateTransition();
        }

        Point newHead = new Point(head);
        switch (currentDirection) {
            case UP: newHead.y--; break;
            case DOWN: newHead.y++; break;
            case LEFT: newHead.x--; break;
            case RIGHT: newHead.x++; break;
        }

        if (newHead.x < 0 || newHead.x >= GRID_WIDTH || newHead.y < 0 || newHead.y >= GRID_HEIGHT) {
            gameOver = true;
            return;
        }

        for (int i = 1; i < snake.size(); i++) {
            if (newHead.equals(snake.get(i))) {
                gameOver = true;
                return;
            }
        }

        snake.add(0, newHead);

        if (newHead.equals(food)) {
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

    public void updateCoinsFromFirebase(int firebaseCoins) {
        this.coins = firebaseCoins;
        prefs.edit().putInt("coins", firebaseCoins).apply();
    }

    private void drawGame(Canvas canvas) {
        nightMode = prefs.getBoolean("night_mode", true);

        canvas.drawColor(nightMode ? Color.BLACK : Color.parseColor("#F5F5F5"));

        if (dynamicBlockSize == 0) {
            calculateGameArea();
        }

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
            paint.setColor(nightMode ? Color.WHITE : Color.BLACK);
        }
        Rect foodRect = new Rect(
                gameAreaOffsetX + food.x * dynamicBlockSize,
                gameAreaOffsetY + food.y * dynamicBlockSize,
                gameAreaOffsetX + (food.x + 1) * dynamicBlockSize,
                gameAreaOffsetY + (food.y + 1) * dynamicBlockSize
        );
        canvas.drawRect(foodRect, paint);

        // DIBUJAR ASTEROIDES
        for (Asteroid asteroid : asteroids) {
            asteroid.draw(canvas);
        }

        // DIBUJAR ENEMIGOS (serpientes completas)
        for (EnemySnake enemy : enemies) {
            for (int i = 0; i < enemy.body.size(); i++) {
                Point segment = enemy.body.get(i);

                // Color del enemigo: rojo oscuro a rojo claro
                if (i == 0) {
                    paint.setColor(Color.parseColor("#FF4444")); // Cabeza más brillante
                } else {
                    paint.setColor(Color.parseColor("#CC0000")); // Cuerpo más oscuro
                }

                Rect enemyRect = new Rect(
                        gameAreaOffsetX + segment.x * dynamicBlockSize,
                        gameAreaOffsetY + segment.y * dynamicBlockSize,
                        gameAreaOffsetX + (segment.x + 1) * dynamicBlockSize,
                        gameAreaOffsetY + (segment.y + 1) * dynamicBlockSize
                );
                canvas.drawRect(enemyRect, paint);

                // Borde amarillo para la cabeza, naranja para el cuerpo
                if (i == 0) {
                    paint.setColor(Color.YELLOW);
                } else {
                    paint.setColor(Color.parseColor("#FFA500"));
                }
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                canvas.drawRect(enemyRect, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }

        // Dibujar animación de quemarse si está activa
        if (isBurning && burnAnimation != null) {
            burnAnimation.draw(canvas);

            // No dibujar la cabeza de la serpiente (está siendo "quemada")
            for (int i = 1; i < snake.size(); i++) {
                Point segment = snake.get(i);
                drawSnakeSegment(canvas, segment, i);
            }
        } else {
            // Dibujar serpiente completa si no hay animación
            equippedSkin = prefs.getString("equipped_skin", "skin_default");
            for (int i = 0; i < snake.size(); i++) {
                drawSnakeSegment(canvas, snake.get(i), i);
            }
        }

        if (gameOver && !isBurning) {
            drawGameOver(canvas);
        }

        if (gameWon) {
            drawGameWon(canvas);
        }

        drawGameInfo(canvas);
    }

    private void drawSnakeSegment(Canvas canvas, Point segment, int index) {
        if ("skin_red".equals(equippedSkin)) {
            paint.setColor(index == 0 ? Color.parseColor("#FF4444") : Color.parseColor("#B71C1C"));
        } else if ("skin_blue".equals(equippedSkin)) {
            paint.setColor(index == 0 ? Color.parseColor("#448AFF") : Color.parseColor("#0D47A1"));
        } else {
            paint.setColor(index == 0 ? Color.GREEN : Color.rgb(0, 150, 0));
        }

        Rect segmentRect = new Rect(
                gameAreaOffsetX + segment.x * dynamicBlockSize,
                gameAreaOffsetY + segment.y * dynamicBlockSize,
                gameAreaOffsetX + (segment.x + 1) * dynamicBlockSize,
                gameAreaOffsetY + (segment.y + 1) * dynamicBlockSize
        );
        canvas.drawRect(segmentRect, paint);

        paint.setColor(nightMode ? Color.DKGRAY : Color.LTGRAY);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawRect(segmentRect, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawGameWon(Canvas canvas) {
        paint.setColor(Color.GREEN);
        paint.setTextSize(60);
        paint.setStyle(Paint.Style.FILL);
        Typeface tf = ResourcesCompat.getFont(getContext(), R.font.vcr_osd_mono_1_001);
        if (tf != null) {
            paint.setTypeface(tf);
        } else {
            paint.setTypeface(Typeface.MONOSPACE);
        }

        String winText = "¡VICTORIA!";
        float textWidth = paint.measureText(winText);
        canvas.drawText(winText, (getWidth() - textWidth) / 2, getHeight() / 2 - 50, paint);

        paint.setColor(Color.YELLOW);
        paint.setTextSize(40);
        String scoreText = "Puntos: " + score + "/" + WIN_SCORE;
        float scoreWidth = paint.measureText(scoreText);
        canvas.drawText(scoreText, (getWidth() - scoreWidth) / 2, getHeight() / 2 + 20, paint);

        paint.setTextSize(30);
        String restartText = "Toca para jugar de nuevo";
        float restartWidth = paint.measureText(restartText);
        canvas.drawText(restartText, (getWidth() - restartWidth) / 2, getHeight() / 2 + 70, paint);
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
        paint.setColor(nightMode ? Color.WHITE : Color.BLACK);
        paint.setTextSize(36);
        Typeface tf = ResourcesCompat.getFont(getContext(), R.font.vcr_osd_mono_1_001);
        if (tf != null) {
            paint.setTypeface(tf);
        } else {
            paint.setTypeface(Typeface.MONOSPACE);
        }

        // Mostrar nivel de dificultad actual
        paint.setTextSize(20);
        String difficultyText = getDifficultyText();
        paint.setColor(Color.CYAN);
        canvas.drawText(difficultyText, 10, 40, paint);

        // Mostrar progreso hacia la victoria
        paint.setColor(Color.YELLOW);
        String progressText = score + "/" + WIN_SCORE;
        canvas.drawText(progressText, getWidth() - 80, 40, paint);
    }

    private String getDifficultyText() {
        if (score >= 180 && score <= 230) {
            return "Nivel: Asteroides";
        } else if (score >= 130 && score <= 180) {
            return "Nivel: Enemigos x" + maxEnemies;
        } else if (score >= 90 && score <= 130) {
            return "Nivel: Enemigos x" + maxEnemies;
        } else if (score >= 30 && score <= 90) {
            return "Nivel: Enemigos x1";
        } else if (score >= 50) {
            return "Nivel: Fondo cambiado";
        } else {
            return "Nivel: Principiante";
        }
    }

    public void restartGame() {
        initGame();
    }

    public void setDirectionUp() {
        if (currentDirection != Direction.DOWN && !gameOver && !isBurning && !gameWon) {
            nextDirection = Direction.UP;
        }
    }

    public void setDirectionDown() {
        if (currentDirection != Direction.UP && !gameOver && !isBurning && !gameWon) {
            nextDirection = Direction.DOWN;
        }
    }

    public void setDirectionLeft() {
        if (currentDirection != Direction.RIGHT && !gameOver && !isBurning && !gameWon) {
            nextDirection = Direction.LEFT;
        }
    }

    public void setDirectionRight() {
        if (currentDirection != Direction.LEFT && !gameOver && !isBurning && !gameWon) {
            nextDirection = Direction.RIGHT;
        }
    }

    public int getScore() {
        return score;
    }

    public boolean isGameOver() {
        return gameOver || gameWon;
    }

    public boolean isGameWon() {
        return gameWon;
    }

    public void stopGame() {
        running = false;
        if (transitionSound != null) {
            transitionSound.release();
            transitionSound = null;
        }
        if (burnSound != null) {
            burnSound.release();
            burnSound = null;
        }
        if (winSound != null) {
            winSound.release();
            winSound = null;
        }
    }
}