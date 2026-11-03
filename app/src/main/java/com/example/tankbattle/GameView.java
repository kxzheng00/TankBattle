package com.example.tankbattle;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends View {
    /** 逻辑状态锁：游戏线程写，UI 线程读，双方都需持有 */
    private final Object lock = new Object();

    private Paint paint;
    private PlayerTank playerTank;
    private List<EnemyTank> enemyTanks;
    private List<Bullet> bullets;
    private List<Wall> walls;
    private List<Rect> grass;
    private Base base;
    private int score;
    private boolean gameOver;
    private Random random;

    /**
     * 距离下次生成敌人还要经过的逻辑步数。
     * 用步数而非时间戳，保证生成频率与刷新率无关。
     */
    private int stepsUntilEnemySpawn;
    private static final int ENEMY_SPAWN_STEPS = 120; // 120 步 = 2 秒（60 步/秒）

    private int gameWidth;
    private int gameHeight;

    private GameThread gameThread;

    public GameView(Context context) {
        super(context);
        this.gameWidth = 800; // 默认宽度
        this.gameHeight = 1200; // 默认高度
        init();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        synchronized (lock) {
            this.gameWidth = w;
            this.gameHeight = h;
            // 重新初始化游戏，使用新的尺寸
            init();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startLoop();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopLoop();
        super.onDetachedFromWindow();
    }

    private void startLoop() {
        if (gameThread != null && gameThread.isAlive()) {
            return;
        }
        gameThread = new GameThread(this);
        gameThread.start();
    }

    private void stopLoop() {
        if (gameThread != null) {
            GameThread thread = gameThread;
            thread.requestStop();
            gameThread = null;
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        playerTank = new PlayerTank(gameWidth / 2, gameHeight - 100);
        enemyTanks = new ArrayList<>();
        bullets = new ArrayList<>();
        walls = new ArrayList<>();
        grass = new ArrayList<>();
        base = new Base(gameWidth / 2 - 25, 50, 50, 50);
        score = 0;
        gameOver = false;
        random = new Random();
        stepsUntilEnemySpawn = ENEMY_SPAWN_STEPS;

        // 创建游戏地图
        createMap();
    }

    private void createMap() {
        // 创建边界墙
        for (int i = 0; i < gameWidth; i += 50) {
            walls.add(new Wall(i, 0, 50, 50, true));
            walls.add(new Wall(i, gameHeight - 50, 50, 50, true));
        }
        for (int i = 50; i < gameHeight - 50; i += 50) {
            walls.add(new Wall(0, i, 50, 50, true));
            walls.add(new Wall(gameWidth - 50, i, 50, 50, true));
        }

        // 创建内部墙
        for (int i = 0; i < 20; i++) {
            int x = randomCoordinate(50, gameWidth - 100);
            int y = randomCoordinate(100, gameHeight - 100);
            walls.add(new Wall(x, y, 50, 50, random.nextBoolean()));
        }

        // 创建草地
        for (int i = 0; i < 30; i++) {
            int x = randomCoordinate(50, gameWidth - 100);
            int y = randomCoordinate(100, gameHeight - 100);
            grass.add(new Rect(x, y, x + 100, y + 100));
        }
    }

    /**
     * 由 {@link GameThread} 以固定步长调用，推进一帧逻辑。
     */
    public void step() {
        synchronized (lock) {
            update();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制期间加锁，避免读到逻辑线程改到一半的状态
        synchronized (lock) {
            drawScene(canvas);
        }
    }

    private void drawScene(Canvas canvas) {
        // 绘制背景
        canvas.drawColor(Color.BLACK);

        // 绘制草地
        paint.setColor(Color.GREEN);
        for (Rect rect : grass) {
            canvas.drawRect(rect, paint);
        }

        // 绘制墙壁
        for (Wall wall : walls) {
            wall.draw(canvas, paint);
        }

        // 绘制基地
        base.draw(canvas, paint);

        // 绘制玩家坦克
        playerTank.draw(canvas, paint);

        // 绘制敌方坦克
        for (EnemyTank enemyTank : enemyTanks) {
            enemyTank.draw(canvas, paint);
        }

        // 绘制子弹
        for (Bullet bullet : bullets) {
            bullet.draw(canvas, paint);
        }

        // 绘制得分
        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        canvas.drawText(getContext().getString(R.string.score, score), 50, 100, paint);

        if (gameOver) {
            paint.setTextSize(80);
            String gameOverText = getContext().getString(R.string.game_over);
            canvas.drawText(gameOverText, gameWidth / 2 - paint.measureText(gameOverText) / 2, gameHeight / 2, paint);
        }
    }

    private void update() {
        if (gameOver) return;

        // 生成敌方坦克
        if (--stepsUntilEnemySpawn <= 0) {
            if (enemyTanks.size() < 5) {
                spawnEnemy();
            }
            stepsUntilEnemySpawn = ENEMY_SPAWN_STEPS;
        }

        // 更新玩家坦克
        playerTank.update(walls, gameWidth, gameHeight);

        // 更新敌方坦克
        for (int i = enemyTanks.size() - 1; i >= 0; i--) {
            EnemyTank enemyTank = enemyTanks.get(i);
            enemyTank.update(playerTank, walls);
            // 检查敌方坦克是否与玩家坦克碰撞
            if (Rect.intersects(playerTank.getRect(), enemyTank.getRect())) {
                gameOver = true;
            }
            // 敌方坦克射击
            if (bullets.size() < 20 && random.nextFloat() < 0.01) {
                bullets.add(enemyTank.shoot());
            }
        }

        // 更新子弹
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.update();
            if (bullet.isOutOfBounds(gameWidth, gameHeight)) {
                bullets.remove(i);
                continue;
            }

            boolean hitWall = false;
            for (int wallIndex = walls.size() - 1; wallIndex >= 0; wallIndex--) {
                Wall wall = walls.get(wallIndex);
                if (Rect.intersects(bullet.getRect(), wall.getRect())) {
                    if (!wall.isIndestructible()) {
                        walls.remove(wallIndex);
                    }
                    bullets.remove(i);
                    hitWall = true;
                    break;
                }
            }
            if (hitWall) continue;

            if (bullet.isPlayerBullet()) {
                for (int j = enemyTanks.size() - 1; j >= 0; j--) {
                    EnemyTank enemyTank = enemyTanks.get(j);
                    if (Rect.intersects(bullet.getRect(), enemyTank.getRect())) {
                        bullets.remove(i);
                        enemyTanks.remove(j);
                        score += 100;
                        break;
                    }
                }
            } else {
                if (Rect.intersects(bullet.getRect(), playerTank.getRect())
                        || Rect.intersects(bullet.getRect(), base.getRect())) {
                    bullets.remove(i);
                    gameOver = true;
                    break;
                }
            }
        }
    }

    private void spawnEnemy() {
        int x = randomCoordinate(70, gameWidth - 70);
        enemyTanks.add(new EnemyTank(x, 100));
    }

    private int randomCoordinate(int min, int max) {
        if (max <= min) return Math.max(0, (min + max) / 2);
        return random.nextInt(max - min + 1) + min;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        synchronized (lock) {
            if (gameOver) {
                // 游戏结束，点击重新开始
                init();
                return true;
            }

            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    // 控制玩家坦克
                    playerTank.setDirection(x, y);
                    // 射击
                    if (bullets.size() < 5) {
                        bullets.add(playerTank.shoot());
                    }
                    break;
            }
        }
        return true;
    }
}
