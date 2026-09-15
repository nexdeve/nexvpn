package ai.nextech.nexvpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.OpenVPNManagement;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;

/**
 * NexVPN Foreground Service — manages the OpenVPN tunnel lifecycle.
 * Uses ics-openvpn as the underlying VPN engine.
 */
public class NexVpnService extends VpnService implements VpnStatus.StateListener {

    // ─── Constants ────────────────────────────────────────────────────────────

    static final String ACTION_CONNECT    = "ai.nextech.nexvpn.ACTION_CONNECT";
    static final String ACTION_DISCONNECT = "ai.nextech.nexvpn.ACTION_DISCONNECT";
    static final String EXTRA_OVPN_CONFIG = "nexvpn_ovpn_config";
    static final String EXTRA_USERNAME    = "nexvpn_username";
    static final String EXTRA_PASSWORD    = "nexvpn_password";

    private static final String CHANNEL_ID   = "nexvpn_channel";
    private static final int    NOTIF_ID     = 8001;
    private static final long   SPEED_INTERVAL_MS = 1000L;

    // ─── Fields ───────────────────────────────────────────────────────────────

    private final IBinder binder = new LocalBinder();
    private InternalVpnListener internalListener;
    private Handler mainHandler;

    private Timer speedTimer;
    private long lastDownloadBytes = 0;
    private long lastUploadBytes   = 0;

    private VpnProfile activeProfile;

    // ─── Binder ───────────────────────────────────────────────────────────────

    public class LocalBinder extends Binder {
        NexVpnService getService() {
            return NexVpnService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        VpnStatus.addStateListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (action == null) return START_NOT_STICKY;

        switch (action) {
            case ACTION_CONNECT:
                String config   = intent.getStringExtra(EXTRA_OVPN_CONFIG);
                String username = intent.getStringExtra(EXTRA_USERNAME);
                String password = intent.getStringExtra(EXTRA_PASSWORD);
                startForeground(NOTIF_ID, buildNotification("Connecting…"));
                connectVpn(config, username, password);
                break;

            case ACTION_DISCONNECT:
                disconnectVpn();
                stopForeground(true);
                stopSelf();
                break;
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        VpnStatus.removeStateListener(this);
        stopSpeedTimer();
    }

    // ─── VPN Connection Logic ─────────────────────────────────────────────────

    private void connectVpn(String ovpnConfig, String username, String password) {
        try {
            ConfigParser parser = new ConfigParser();
            parser.parseConfig(new java.io.StringReader(ovpnConfig));

            activeProfile = parser.convertProfile();
            activeProfile.mName = "NexVPN";

            if (username != null && !username.isEmpty()) {
                activeProfile.mUsername = username;
                activeProfile.mPassword = password;
            }

            ProfileManager.setTemporaryProfile(this, activeProfile);

            Intent launchIntent = new Intent(this, LaunchVPN.class);
            launchIntent.putExtra(LaunchVPN.EXTRA_KEY, activeProfile.getUUID().toString());
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);

            startSpeedTimer();

        } catch (Exception e) {
            notifyError("Failed to parse VPN config: " + e.getMessage());
        }
    }

    private void disconnectVpn() {
        stopSpeedTimer();
        OpenVPNManagement management = OpenVPNService.getManagement();
        if (management != null) {
            management.stopVPN(false);
        }
    }

    // ─── Speed Monitoring ─────────────────────────────────────────────────────

    private void startSpeedTimer() {
        speedTimer = new Timer();
        speedTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long[] stats = VpnStatus.getTrafficStats();
                if (stats == null) return;

                long totalDown = stats[0];
                long totalUp   = stats[1];

                long downSpeed = (totalDown - lastDownloadBytes);
                long upSpeed   = (totalUp   - lastUploadBytes);

                lastDownloadBytes = totalDown;
                lastUploadBytes   = totalUp;

                final long fd = totalDown, fu = totalUp, ds = downSpeed, us = upSpeed;
                mainHandler.post(() -> {
                    if (internalListener != null) {
                        internalListener.onSpeedUpdate(fd, fu, ds, us);
                    }
                });
            }
        }, SPEED_INTERVAL_MS, SPEED_INTERVAL_MS);
    }

    private void stopSpeedTimer() {
        if (speedTimer != null) {
            speedTimer.cancel();
            speedTimer = null;
        }
    }

    // ─── VpnStatus.StateListener ──────────────────────────────────────────────

    @Override
    public void updateState(String state, String logmessage, int localizedResId,
                            VpnStatus.ConnectionStatus level) {
        mainHandler.post(() -> {
            if (internalListener == null) return;

            switch (state) {
                case "CONNECTED":
                    updateNotification("Connected — NexVPN");
                    internalListener.onConnected();
                    break;
                case "NOPROCESS":
                case "EXITING":
                    internalListener.onStopped();
                    break;
                case "AUTH_FAILED":
                    internalListener.onError("Authentication failed. Check username/password.");
                    break;
                default:
                    internalListener.onStatusUpdate(state);
                    break;
            }
        });
    }

    @Override
    public void setConnectedVPN(String uuid) { /* no-op */ }

    // ─── Notification ─────────────────────────────────────────────────────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "NexVPN Status",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows VPN connection status");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String message) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("NexVPN")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String message) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification(message));
    }

    private void notifyError(String message) {
        mainHandler.post(() -> {
            if (internalListener != null) internalListener.onError(message);
        });
    }

    // ─── Internal Listener ────────────────────────────────────────────────────

    void setInternalListener(InternalVpnListener listener) {
        this.internalListener = listener;
    }

    interface InternalVpnListener {
        void onConnected();
        void onStopped();
        void onStatusUpdate(String status);
        void onError(String error);
        void onSpeedUpdate(long downloadBytes, long uploadBytes,
                           long downloadSpeed, long uploadSpeed);
    }
}
