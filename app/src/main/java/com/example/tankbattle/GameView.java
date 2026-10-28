package com.example.tankbattle;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends View {
    private Context context;
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
    private long lastEnemySpawnTime;
    private static final long ENEMY_SPAWN_INTERVAL = 2000;
    private int gameWidth;
    private int gameHeight;

    public GameView(Context context) {
        super(context);
        this.context = context;
        this.gameWidth = 800; // 默认宽度
        this.gameHeight = 1200; // 默认高度
        init();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.gameWidth = w;
        this.gameHeight = h;
        // 重新初始化游戏，使用新的尺寸
        init();
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
        lastEnemySpawnTime = System.currentTimeMillis();

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
            int x = random.nextInt(gameWidth - 100) + 50;
            int y = random.nextInt(gameHeight - 200) + 100;
            walls.add(new Wall(x, y, 50, 50, random.nextBoolean()));
        }

        // 创建草地
        for (int i = 0; i < 30; i++) {
            int x = random.nextInt(gameWidth - 100) + 50;
            int y = random.nextInt(gameHeight - 200) + 100;
            grass.add(new Rect(x, y, x + 100, y + 100));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
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

        // 更新游戏逻辑
        update();
        // 重绘
        invalidate();
    }

    private void update() {
        if (gameOver) return;

        // 生成敌方坦克
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEnemySpawnTime > ENEMY_SPAWN_INTERVAL && enemyTanks.size() < 5) {
            spawnEnemy();
            lastEnemySpawnTime = currentTime;
        }

        // 更新玩家坦克
        playerTank.update();

        // 更新敌方坦克
        for (int i = enemyTanks.size() - 1; i >= 0; i--) {
            EnemyTank enemyTank = enemyTanks.get(i);
            enemyTank.update(playerTank, walls);
            // 检查敌方坦克是否与玩家坦克碰撞
            if (Rect.intersects(playerTank.getRect(), enemyTank.getRect())) {
                gameOver = true;
            }
            // 敌方坦克射击
            if (Math.random() < 0.01) { // 1%的概率射击
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

            // 检查子弹是否击中墙壁
            for (Wall wall : walls) {
                if (Rect.intersects(bullet.getRect(), wall.getRect())) {
                    bullets.remove(i);
                    if (!wall.isIndestructible()) {
                        walls.remove(wall);
                    }
                    break;
                }
            }

            // 检查子弹是否击中敌方坦克
            for (int j = enemyTanks.size() - 1; j >= 0; j--) {
                EnemyTank enemyTank = enemyTanks.get(j);
                if (Rect.intersects(bullet.getRect(), enemyTank.getRect())) {
                    bullets.remove(i);
                    enemyTanks.remove(j);
                    score += 100;
                    break;
                }
            }

            // 检查子弹是否击中玩家坦克
            if (Rect.intersects(bullet.getRect(), playerTank.getRect())) {
                bullets.remove(i);
                gameOver = true;
                break;
            }

            // 检查子弹是否击中基地
            if (Rect.intersects(bullet.getRect(), base.getRect())) {
                bullets.remove(i);
                gameOver = true;
                break;
            }
        }
    }

    private void spawnEnemy() {
        int x = random.nextInt(gameWidth - 100) + 50;
        enemyTanks.add(new EnemyTank(x, 100));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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
        return true;
    }
}