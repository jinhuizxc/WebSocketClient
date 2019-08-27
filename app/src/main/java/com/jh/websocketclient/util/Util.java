package com.jh.websocketclient.util;

import android.content.Context;
import android.widget.Toast;

/**
 * http://www.websocket-test.com/
 *
 * ws://echo.websocket.org
 * wss://echo.websocket.org
 *
 */
public class Util {

    public static final String ws = "ws://echo.websocket.org";//websocket测试地址

    public static void showToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }
}
