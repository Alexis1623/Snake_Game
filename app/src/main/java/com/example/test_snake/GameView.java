package com.example.test_snake;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private Thread thread;
    private boolean running = false;
    private SurfaceHolder holder;

    private Bitmap spriteSheet;
    private int frameWidth = 64;
    private int frameHeight = 64;
    private int columns = 3;
    private int rows = 4;

    // Anim state
    private int currentRow = 0;
    private int currentCol = 0;
    private long lastFrameChangeTime = 0;
    private int frameLengthInMilliseconds = 200;

    // Posición del sprite en pantalla
    private int x = 100;  // Más a la izquierda para dar espacio
    private int y = 300;  // Más centrado verticalmente

    private int scale = 2;

    private Paint paint;
    private Typeface typeface;

    // CONSTRUCTOR 1: Para crear desde código
    public GameView(Context context) {
        super(context);
        init(context);
    }

    // CONSTRUCTOR 2: Para inflar desde XML (¡ESTE FALTABA!)
    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    // CONSTRUCTOR 3: Para inflar desde XML con estilo
    public GameView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        paint = new Paint();

        // Cargar fuente
        try {
            typeface = Typeface.createFromAsset(context.getAssets(), "fonts/vcr_osd_mono_1_001.ttf");
        } catch (Exception e) {
            typeface = Typeface.MONOSPACE;
        }

        // Cargar sprite sheet
        try {
            spriteSheet = BitmapFactory.decodeResource(getResources(), R.drawable.snake_spritesheet);
        } catch (Exception e) {
            spriteSheet = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        boolean retry = true;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void run() {
        while (running) {
            if (!holder.getSurface().isValid()) continue;

            long now = System.currentTimeMillis();
            updateAnimation(now);

            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                drawFrame(canvas);
                holder.unlockCanvasAndPost(canvas);
            }

            try { Thread.sleep(16); } catch (InterruptedException e) { }
        }
    }

    private void updateAnimation(long now) {
        if (now > lastFrameChangeTime + frameLengthInMilliseconds) {
            lastFrameChangeTime = now;
            currentCol++;
            if (currentCol >= columns) currentCol = 0;
        }
    }

    private void drawFrame(Canvas canvas) {
        canvas.drawColor(Color.BLACK);

        if (spriteSheet != null) {
            int srcX = currentCol * frameWidth;
            int srcY = currentRow * frameHeight;
            Rect src = new Rect(srcX, srcY, srcX + frameWidth, srcY + frameHeight);

            int destW = frameWidth * scale;
            int destH = frameHeight * scale;
            Rect dst = new Rect(x, y, x + destW, y + destH);

            canvas.drawBitmap(spriteSheet, src, dst, null);
        } else {
            drawBasicSnake(canvas);
        }

        drawDebugInfo(canvas);
    }

    private void drawBasicSnake(Canvas canvas) {
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.FILL);

        int headSize = 30 * scale;
        int centerX = x + headSize / 2;
        int centerY = y + headSize / 2;

        canvas.drawCircle(centerX, centerY, headSize / 2, paint);

        paint.setColor(Color.BLACK);
        int eyeOffset = headSize / 4;

        int leftEyeX = centerX;
        int leftEyeY = centerY;
        int rightEyeX = centerX;
        int rightEyeY = centerY;

        switch (currentRow) {
            case 0: // Derecha
                leftEyeX = centerX + eyeOffset;
                leftEyeY = centerY - eyeOffset;
                rightEyeX = centerX + eyeOffset;
                rightEyeY = centerY + eyeOffset;
                break;
            case 1: // Izquierda
                leftEyeX = centerX - eyeOffset;
                leftEyeY = centerY - eyeOffset;
                rightEyeX = centerX - eyeOffset;
                rightEyeY = centerY + eyeOffset;
                break;
            case 2: // Arriba
                leftEyeX = centerX - eyeOffset;
                leftEyeY = centerY - eyeOffset;
                rightEyeX = centerX + eyeOffset;
                rightEyeY = centerY - eyeOffset;
                break;
            case 3: // Abajo
                leftEyeX = centerX - eyeOffset;
                leftEyeY = centerY + eyeOffset;
                rightEyeX = centerX + eyeOffset;
                rightEyeY = centerY + eyeOffset;
                break;
        }

        canvas.drawCircle(leftEyeX, leftEyeY, headSize / 8, paint);
        canvas.drawCircle(rightEyeX, rightEyeY, headSize / 8, paint);
    }

    private void drawDebugInfo(Canvas canvas) {
        paint.setColor(Color.WHITE);
        paint.setTextSize(24);
        paint.setTypeface(typeface);

        String direction = "";
        switch (currentRow) {
            case 0: direction = "DERECHA"; break;
            case 1: direction = "IZQUIERDA"; break;
            case 2: direction = "ARRIBA"; break;
            case 3: direction = "ABAJO"; break;
        }

        canvas.drawText("Dirección: " + direction, 20, 50, paint);
        canvas.drawText("Frame: " + currentCol, 20, 80, paint);
        canvas.drawText("Pos: " + x + "," + y, 20, 110, paint);
    }

    public void setDirectionRight() {
        currentRow = 0;
        moveBy(10, 0);
    }

    public void setDirectionLeft() {
        currentRow = 1;
        moveBy(-10, 0);
    }

    public void setDirectionUp() {
        currentRow = 2;
        moveBy(0, -10);
    }

    public void setDirectionDown() {
        currentRow = 3;
        moveBy(0, 10);
    }

    public void moveBy(int dx, int dy) {
        x += dx;
        y += dy;

        if (x < 0) x = 0;
        if (y < 0) y = 0;

        if (getWidth() > 0 && getHeight() > 0) {
            // En horizontal, el ancho es mayor que el alto
            int maxX = getWidth() - (frameWidth * scale) - 250; // Dejar espacio para controles
            int maxY = getHeight() - (frameHeight * scale);

            if (x > maxX) x = maxX;
            if (y > maxY) y = maxY;
        }
    }

    public void stopGame() {
        running = false;
    }
}