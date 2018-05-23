package android.listener;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

/**
 * 监听Home键按下
 */
public class HomeListener {
    private final Activity activity;
    private final HomeReceiver receiver;
    private HomeCallBack homeCallBack;

    public HomeListener(Activity activity, HomeCallBack homeCallBack) {
        this.homeCallBack = homeCallBack;
        receiver = new HomeReceiver();
        this.activity = activity;
        IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        activity.registerReceiver(receiver, homeFilter);
    }

    /**
     * 返回桌面监听
     */
    private class HomeReceiver extends BroadcastReceiver {
        private final String SYSTEM_DIALOG_REASON_KEY = "reason";
        private final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
        private final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!TextUtils.isEmpty(action) && action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);

                if (reason == null)
                    return;

                // Home键
                if (reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY)) {
                    homeCallBack.onHome();
                }

                // 最近任务列表键
                if (reason.equals(SYSTEM_DIALOG_REASON_RECENT_APPS)) {
                    homeCallBack.onRecent();
                }
            }
        }
    }

    public interface HomeCallBack {
        public void onHome();

        public void onRecent();
    }
}
