package com.jh.websocketclient.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.jh.websocketclient.R;
import com.jh.websocketclient.websocket.MyWebSocketClient;

public class WebSocketService extends Service {

    private JWebSocketClientBinder mBinder = new JWebSocketClientBinder();
    private final static int GRAY_SERVICE_ID = 1001;

    // 灰色保活
    public static class GrayInnerService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(GRAY_SERVICE_ID, new Notification());
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    PowerManager.WakeLock wakeLock;// 锁屏唤醒

    // 获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
    @SuppressLint("InvalidWakeLockTag")
    private void acquireWakeLock() {
        if (null == wakeLock) {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "PostLocationService");
            if (null != wakeLock) {
                wakeLock.acquire();
            }
        }
    }

    //用于Activity和service通讯
    public class JWebSocketClientBinder extends Binder {
        public WebSocketService getService() {
            return WebSocketService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        
        // 开启websocket
       openMyWebSocket();
        // 开启心跳检测
       MyWebSocketClient.getInstance(this).openHeart();

        //设置service为前台服务，提高优先级
        if (Build.VERSION.SDK_INT < 18) {
            //Android4.3以下 ，隐藏Notification上的图标
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else if (Build.VERSION.SDK_INT > 18 && Build.VERSION.SDK_INT < 25) {
            //Android4.3 - Android7.0，隐藏Notification上的图标
            Intent innerIntent = new Intent(this, GrayInnerService.class);
            startService(innerIntent);
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else {
            //Android7.0以上app启动后通知栏会出现一条"正在运行"的通知
//            startForeground(GRAY_SERVICE_ID, new Notification());
            if (Build.VERSION.SDK_INT >= 26) {
                String channelId = "MyChannelId";
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                NotificationChannel channel = new NotificationChannel(channelId, "name", NotificationManager.IMPORTANCE_MIN);
                manager.createNotificationChannel(channel);

                Notification.Builder builder = new Notification.Builder(this)
                        .setChannelId(channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setPriority(Notification.PRIORITY_MIN)
                        .setAutoCancel(true);
                Notification notification = new Notification.InboxStyle(builder).build();

                startForeground(1, notification);
            }
        }

        acquireWakeLock();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        MyWebSocketClient.getInstance(this).closeConnect();
        super.onDestroy();
    }

    public WebSocketService() {
    }


    private void openMyWebSocket() {
        // 开启WebSocket客户端
//        try {
//            ReadyState readyState = MyWebSocketClient.getInstance(this).getReadyState();
//            Log.e(MyWebSocketClient.TAG, "openMyWebSocket: getReadyState() = " + readyState);
//            if (readyState.equals(ReadyState.NOT_YET_CONNECTED)) {
//                Log.e(MyWebSocketClient.TAG, "openMyWebSocket: ---开启WebSocket客户端---");
//                MyWebSocketClient.getInstance(this).connect();
//            } else if (readyState.equals(ReadyState.CLOSED)) {
//                MyWebSocketClient.getInstance(this).reconnectWebSocket();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.e(MyWebSocketClient.TAG, "openMyWebSocket: ..开启WebSocket出现异常...");
//        }
        MyWebSocketClient.getInstance(this).toConnect();
    }



    /**
     * 发送消息
     *
     * @param msg
     */
    public void sendMsg(String msg) {
        if (null != MyWebSocketClient.getInstance(this)) {
            Log.e("WebSocketService", "发送的消息：" + msg);
            MyWebSocketClient.getInstance(this).send(msg);
        }
    }





}
