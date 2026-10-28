package com.example.tankbattle;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class Bullet {
    private float x, y;
    private float dx, dy;
    private float speed;
    private int radius;
    private Rect rect;

    public Bullet(float x, float y, float angle) {
        this.x = x;
        this.y = y;
        this.speed = 10;
        this.radius = 5;
        // 计算速度分量
        this.dx = (float) Math.cos(Math.toRadians(angle)) * speed;
        this.dy = (float) Math.sin(Math.toRadians(angle)) * speed;
        // 初始化碰撞矩形
        this.rect = new Rect((int) (x - radius), (int) (y - radius), 
                            (int) (x + radius), (int) (y + radius));
    }

    public void update() {
        // 更新位置
        x += dx;
        y += dy;
        // 更新碰撞矩形
        rect.set((int) (x - radius), (int) (y - radius), 
                (int) (x + radius), (int) (y + radius));
    }

    public void draw(Canvas canvas, Paint paint) {
        // 绘制子弹
        paint.setColor(Color.YELLOW);
        canvas.drawCircle(x, y, radius, paint);
    }

    public boolean isOutOfBounds(int width, int height) {
        return x < 0 || x > width || y < 0 || y > height;
    }

    public Rect getRect() {
        return rect;
    }
}