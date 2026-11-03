package com.example.tankbattle;

/**
 * 独立的游戏循环线程。
 *
 * <p>以固定时间步长推进逻辑，使移动速度、射击频率与屏幕刷新率（60Hz / 90Hz / 120Hz）
 * 完全解耦。渲染仍由 UI 线程通过 invalidate() 完成。
 */
public class GameThread extends Thread {
    /** 逻辑帧步长：每秒 60 步 */
    public static final long STEP_NANOS = 1_000_000_000L / 60;
    /** 单次最多补算的时长，防止线程被挂起后一次性追帧导致卡顿 */
    private static final long MAX_FRAME_NANOS = 250_000_000L;

    private final GameView gameView;
    private volatile boolean running = true;

    public GameThread(GameView gameView) {
        super("TankBattle-GameLoop");
        this.gameView = gameView;
        setDaemon(true);
    }

    /** 请求停止循环，可重复调用 */
    public void requestStop() {
        running = false;
        interrupt();
    }

    @Override
    public void run() {
        long previous = System.nanoTime();
        long accumulator = 0L;

        while (running) {
            long now = System.nanoTime();
            long elapsed = now - previous;
            previous = now;

            // 夹住单帧时长，避免界面卡顿后疯狂补步
            if (elapsed > MAX_FRAME_NANOS) {
                elapsed = MAX_FRAME_NANOS;
            }
            accumulator += elapsed;

            // 以固定步长推进逻辑，剩余时间留到下一轮
            while (accumulator >= STEP_NANOS) {
                gameView.step();
                accumulator -= STEP_NANOS;
            }

            // 通知 UI 线程重绘
            gameView.postInvalidate();

            long sleepNanos = STEP_NANOS - accumulator;
            if (sleepNanos > 0) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
