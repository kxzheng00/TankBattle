package com.example.tankbattle;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class Wall {
    private int x, y;
    private int width, height;
    private boolean indestructible;
    private Rect rect;

    public Wall(int x, int y, int width, int height, boolean indestructible) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.indestructible = indestructible;
        this.rect = new Rect(x, y, x + width, y + height);
    }

    public void draw(Canvas canvas, Paint paint) {
        // 根据是否可摧毁设置不同颜色
        if (indestructible) {
            paint.setColor(Color.GRAY);
        } else {
            paint.setColor(Color.BLUE);
        }
        // 绘制墙壁
        canvas.drawRect(rect, paint);
    }

    public boolean isIndestructible() {
        return indestructible;
    }

    public Rect getRect() {
        return rect;
    }
}