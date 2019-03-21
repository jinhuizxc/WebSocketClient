package com.jh.websocketclient.websocket;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.jh.websocketclient.MainActivity;
import com.jh.websocketclient.R;
import com.jh.websocketclient.util.Util;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;

/**
 * WebSocket版本:
 * implementation 'org.java-websocket:Java-WebSocket:1.4.0'
 * <p>
 * 服务器开启监听:
 * 用户登录建立连接
 * <p>
 * 什么条件下执行心跳：
 * 当onopen也就是连接上时，我们便开始start计时，如果在定时时间范围内，onmessage获取到了后端的消息，我们就重置倒计时，
 * 距离上次从后端获取到消息超过60秒之后，执行心跳检测，看是不是断连了，这个检测时间可以自己根据自身情况设定。
 * <p>
 * 服务器未开启,websocket请求建立重连，请求不到或者达到某个次数就终止请求，不让异常发生！
 * <p>
 * onClose: 长链接关闭    // 登录状态时才重连
 */
public class MyWebSocketClient extends WebSocketClient {

    public static final String TAG = "MyWebSocketClient";

    private static final int TYPE_RECEIVE = 0;//接受的消息
    private static final int TYPE_SEND = 1;//发送的消息
    private static final int STATUS_UNREAD = 0;//未读消息
    private static final int STATUS_READED = 1;//已读消息
    private static final int MSG_HEART = 1;   // 心跳
    private static final String serverUriStr = Util.ws;

    //单例选择懒汉模式
    private Context mContext;

    private static MyWebSocketClient client;

    //1. 私有构造方法
    private MyWebSocketClient(Context context) {
        /*ws://服务器ip:8282*/
        super(URI.create(serverUriStr));
        this.mContext = context;
    }

    //2.公开方法,返回单例对象
    public static MyWebSocketClient getInstance(Context context) {
        //懒汉: 考虑线程安全问题
        if (client == null) {
            synchronized (MyWebSocketClient.class) {
                if (client == null) {
                    client = new MyWebSocketClient(context);
                    Log.e(TAG, "getInstance: *****创建单例对象*****");
                }
            }
        }
        return client;
    }


    //    -------------------------------------websocket心跳检测------------------------------------------------
    private static final long HEART_BEAT_RATE = 10 * 1000;// 每隔10秒进行一次对长连接的心跳检测
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            Log.e("WebSocketService", "心跳包检测websocket连接状态");
            if (client != null) {
                if (client.isClosed()) {
                    reconnectWs();  // 重连
                }
            } else {
                //如果client已为空，重新初始化连接
                client = null;
                getInstance(mContext);
            }
            //每隔一定的时间，对长连接进行一次心跳检测
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    /**
     * 开启重连
     */
    private void reconnectWs() {
        mHandler.removeCallbacks(heartBeatRunnable);
        new Thread() {
            @Override
            public void run() {
                try {
                    Log.e("WebSocketService", "开启重连");
                    client.reconnectBlocking();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    // 长链接开启
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.e(TAG, "onOpen: 长链接开启");
    }

    // 消息通道收到消息
    @Override
    public void onMessage(final String message) {
        Log.e(TAG, "onMessage: 消息通道收到消息" + message);
        Log.e("WebSocketService", "收到的消息：" + message);
        Intent intent = new Intent();
        intent.setAction("com.xch.servicecallback.content");
        intent.putExtra("message", message);
        mContext.sendBroadcast(intent);

        // 检查锁屏状态，如果锁屏先点亮屏幕
        checkLockAndShowNotification(message);

    }


    // 通过字节接收消息
    @Override
    public void onMessage(ByteBuffer bytes) {
        super.onMessage(bytes);
    }

    // 长链接关闭
    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.e(TAG, "onClose: 长链接关闭");
    }

    // 链接发生错误
    @Override
    public void onError(Exception ex) {
        Log.e(TAG, "onError: 链接发生错误" + ex.toString());
    }

    // 开启心跳检测
    public void openHeart() {
        mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);
    }

//    /**
//     * 初始化websocket连接
//     */
//    private void initSocketClient() {
//        URI uri = URI.create(Util.ws);
//        client = new JWebSocketClient(uri) {
//            @Override
//            public void onMessage(String message) {
//                Log.e("WebSocketService", "收到的消息：" + message);
//
//                Intent intent = new Intent();
//                intent.setAction("com.xch.servicecallback.content");
//                intent.putExtra("message", message);
//                sendBroadcast(intent);
//
//                // 检查锁屏状态，如果锁屏先点亮屏幕
//                checkLockAndShowNotification(message);
//            }
//
//            @Override
//            public void onOpen(ServerHandshake handshakedata) {
//                super.onOpen(handshakedata);
//                Log.e("WebSocketService", "websocket连接成功");
//            }
//        };
//        client.connect();
//    }

    /**
     * 断开连接
     */
    public void closeConnect() {
        try {
            if (null != client) {
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client = null;
        }
    }

    /**
     * 连接websocket
     */
    public void toConnect() {
        new Thread() {
            @Override
            public void run() {
                try {
                    // connectBlocking多出一个等待操作，会先连接再发送，否则未连接发送会报错
                    client.connectBlocking();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    //    -----------------------------------消息通知--------------------------------------------------------

    /**
     * 检查锁屏状态，如果锁屏先点亮屏幕
     *
     * @param content
     */
    private void checkLockAndShowNotification(String content) {
        // 管理锁屏的一个服务
        KeyguardManager km = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode()) {//锁屏
            //获取电源管理器对象
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            if (!pm.isScreenOn()) {
                @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
                wl.acquire();  //点亮屏幕
                wl.release();  //任务结束后释放
            }
            sendNotification(content);
        } else {
            sendNotification(content);
        }
    }

    /**
     * 发送通知
     *
     * @param content
     */
    private void sendNotification(String content) {
        Intent intent = new Intent();
        intent.setClass(mContext, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager notifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(mContext)
                .setAutoCancel(true)
                // 设置该通知优先级
                .setPriority(Notification.PRIORITY_MAX)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle("服务器")
                .setContentText(content)
                .setVisibility(VISIBILITY_PUBLIC)
                .setWhen(System.currentTimeMillis())
                // 向通知添加声音、闪灯和振动效果
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_ALL | Notification.DEFAULT_SOUND)
                .setContentIntent(pendingIntent)
                .build();
        notifyManager.notify(1, notification);//id要保证唯一
    }

}

