package com.example.tankbattle;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.List;
import java.util.Random;

public class EnemyTank {
    private float x, y;
    private float dx, dy;
    private float angle;
    private float speed;
    private int width, height;
    private Rect rect;
    private Random random;
    private long lastDirectionChangeTime;
    private static final long DIRECTION_CHANGE_INTERVAL = 1000;

    public EnemyTank(float x, float y) {
        this.x = x;
        this.y = y;
        this.random = new Random();
        this.speed = 2;
        this.width = 40;
        this.height = 40;
        this.rect = new Rect((int) (x - width / 2), (int) (y - height / 2), 
                            (int) (x + width / 2), (int) (y + height / 2));
        this.lastDirectionChangeTime = System.currentTimeMillis();
        // 随机初始方向
        changeDirection();
    }

    public void update(PlayerTank playerTank, List<Wall> walls) {
        // 更新位置
        x += dx;
        y += dy;
        // 更新碰撞矩形
        rect.set((int) (x - width / 2), (int) (y - height / 2), 
                (int) (x + width / 2), (int) (y + height / 2));

        // 检查碰撞墙壁
        for (Wall wall : walls) {
            if (Rect.intersects(rect, wall.getRect())) {
                // 碰撞到墙壁，改变方向
                x -= dx;
                y -= dy;
                rect.set((int) (x - width / 2), (int) (y - height / 2),
                        (int) (x + width / 2), (int) (y + height / 2));
                changeDirection();
                lastDirectionChangeTime = System.currentTimeMillis();
                break;
            }
        }

        // 定期改变方向
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDirectionChangeTime > DIRECTION_CHANGE_INTERVAL) {
            changeDirection();
            lastDirectionChangeTime = currentTime;
        }
    }

    private void changeDirection() {
        // 随机选择一个方向
        int direction = random.nextInt(4);
        switch (direction) {
            case 0: // 上
                dx = 0;
                dy = -speed;
                angle = 270;
                break;
            case 1: // 右
                dx = speed;
                dy = 0;
                angle = 0;
                break;
            case 2: // 下
                dx = 0;
                dy = speed;
                angle = 90;
                break;
            case 3: // 左
                dx = -speed;
                dy = 0;
                angle = 180;
                break;
        }
    }

    public Bullet shoot() {
        // 随机方向射击
        float shootAngle = random.nextInt(360);
        // 计算子弹起始位置
        float bulletX = x + (float) Math.cos(Math.toRadians(shootAngle)) * (height);
        float bulletY = y + (float) Math.sin(Math.toRadians(shootAngle)) * (height);
        return new Bullet(bulletX, bulletY, shootAngle, false);
    }

    public void draw(Canvas canvas, Paint paint) {
        // 保存当前画布状态
        canvas.save();
        // 旋转画布
        canvas.rotate(angle, x, y);
        // 绘制坦克主体
        paint.setColor(Color.RED);
        canvas.drawRect(x - width / 2, y - height / 2, x + width / 2, y + height / 2, paint);
        // 绘制坦克炮管
        paint.setColor(Color.YELLOW);
        canvas.drawRect(x - 5, y - height, x + 5, y + height / 2, paint);
        // 恢复画布状态
        canvas.restore();
    }

    public Rect getRect() {
        return rect;
    }
}
