package com.zj.im.chat.core;

import com.zj.im.chat.exceptions.ExceptionHandler;
import com.zj.im.chat.exceptions.LooperInterruptedException;
import com.zj.im.main.ChatBase;
import com.zj.im.net.socket.ReadSocketThread;
import com.zj.im.sender.SendingPool;

/**
 * Created by ZJJ
 *
 * @link the msg looper , it always running with SDK active.
 * <p>
 * improve it efficiency with a frequency controller.
 */
class MsgHandler extends Thread {

    private static MsgHandler msgHandler;

    public static MsgHandler init(String runningKey, Long sleepTime) {
        if (!checkRunning(true)) {
            msgHandler = new MsgHandler(runningKey, sleepTime);
        }
        return msgHandler;
    }

    static void setFrequency(Long time) {
        if (checkRunning()) msgHandler.frequencyConversion(time);
    }

    static boolean checkRunning() {
        return checkRunning(false);
    }

    private static boolean checkRunning(boolean ignoreQuit) {
        if (msgHandler == null) return false;
        boolean isRunning = msgHandler.isAlive() && (ignoreQuit || !msgHandler.mQuit) && !msgHandler.isInterrupted();
        if (!isRunning && !ignoreQuit) {
            ChatBase.INSTANCE.postError(new LooperInterruptedException("thread has been destroyed!"));
        }
        return isRunning;
    }

    private final String runningKey;
    private Long sleepTime;

    /**
     * the Loop started by construct
     */
    private MsgHandler(String runningKey, Long sleepTime) {
        super("msg_handler");
        this.runningKey = runningKey;
        this.sleepTime = sleepTime;
        start();
    }


    private void frequencyConversion(Long sleepTime) {
        synchronized (this) {
            this.sleepTime = sleepTime;
        }
    }

    private void onDestroy() {
        interrupt();
        synchronized (this) {
            mQuit = true;
            notify();
        }
    }

    private boolean mQuit;

    @Override
    public void run() {
        while (!mQuit && !ChatBase.INSTANCE.isFinishing(runningKey)) {
            try {
                if (isInterrupted()) return;
                try {
                    DataStore.INSTANCE.runningInBlock(runningKey);
                } catch (Exception e) {
                    ExceptionHandler.INSTANCE.postError(e);
                }
                try {
                    SendingPool.INSTANCE.runningInBlock(runningKey);
                } catch (Exception e) {
                    ExceptionHandler.INSTANCE.postError(e);
                }
                try {
                    ReadSocketThread.INSTANCE.runningInBlock(runningKey);
                } catch (Exception e) {
                    ExceptionHandler.INSTANCE.postError(e);
                }
                Thread.sleep(sleepTime);
            } catch (Exception e) {
                ExceptionHandler.INSTANCE.postError(e);
            }
        }
    }

    public static void shutdown() {
        if (msgHandler != null) {
            msgHandler.onDestroy();
        }
    }
}
