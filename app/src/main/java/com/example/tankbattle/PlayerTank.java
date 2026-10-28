package com.example.tankbattle;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class PlayerTank {
    private float x, y;
    private float dx, dy;
    private float angle;
    private float speed;
    private int width, height;
    private Rect rect;

    public PlayerTank(float x, float y) {
        this.x = x;
        this.y = y;
        this.angle = 0;
        this.speed = 5;
        this.width = 40;
        this.height = 40;
        this.rect = new Rect((int) (x - width / 2), (int) (y - height / 2), 
                            (int) (x + width / 2), (int) (y + height / 2));
    }

    public void update() {
        // 更新位置
        x += dx;
        y += dy;
        // 更新碰撞矩形
        rect.set((int) (x - width / 2), (int) (y - height / 2), 
                (int) (x + width / 2), (int) (y + height / 2));
    }

    public void draw(Canvas canvas, Paint paint) {
        // 保存当前画布状态
        canvas.save();
        // 旋转画布
        canvas.rotate(angle, x, y);
        // 绘制坦克主体
        paint.setColor(Color.GREEN);
        canvas.drawRect(x - width / 2, y - height / 2, x + width / 2, y + height / 2, paint);
        // 绘制坦克炮管
        paint.setColor(Color.RED);
        canvas.drawRect(x - 5, y - height, x + 5, y + height / 2, paint);
        // 恢复画布状态
        canvas.restore();
    }

    public void setDirection(float targetX, float targetY) {
        // 计算角度
        angle = (float) Math.toDegrees(Math.atan2(targetY - y, targetX - x));
        // 计算移动方向
        dx = (float) Math.cos(Math.toRadians(angle)) * speed;
        dy = (float) Math.sin(Math.toRadians(angle)) * speed;
    }

    public Bullet shoot() {
        // 计算子弹起始位置（炮管末端）
        float bulletX = x + (float) Math.cos(Math.toRadians(angle)) * (height);
        float bulletY = y + (float) Math.sin(Math.toRadians(angle)) * (height);
        return new Bullet(bulletX, bulletY, angle);
    }

    public Rect getRect() {
        return rect;
    }
}