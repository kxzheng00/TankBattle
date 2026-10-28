package com.example.tankbattle;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class Base {
    private int x, y;
    private int width, height;
    private Rect rect;

    public Base(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rect = new Rect(x, y, x + width, y + height);
    }

    public void draw(Canvas canvas, Paint paint) {
        // 绘制基地
        paint.setColor(Color.RED);
        canvas.drawRect(rect, paint);
    }

    public Rect getRect() {
        return rect;
    }
}