package ai.nextech.nexvpn;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;

/**
 * NexVPN - A robust Android OpenVPN integration library
 * Built on ics-openvpn core architecture
 *
 * @version 1.0.0
 * @author NexTech
 */
public class NexVpn {

    private static final int VPN_PERMISSION_REQUEST_CODE = 7001;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 7002;

    private final Context context;
    private VpnListener vpnListener;
    private NexVpnService vpnService;
    private boolean isBound = false;

    private String ovpnConfig;
    private String username;
    private String password;

    // VPN State
    public enum VpnState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        ERROR
    }

    private VpnState currentState = VpnState.DISCONNECTED;

    // ─── Constructor ──────────────────────────────────────────────────────────

    public NexVpn(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    // ─── Profile Attachment ───────────────────────────────────────────────────

    /**
     * Load .ovpn profile from the assets folder
     *
     * @param assetFileName e.g. "us-server.ovpn"
     * @param username      VPN username (pass "" if not needed)
     * @param password      VPN password (pass "" if not needed)
     */
    public void attachFromAsset(@NonNull String assetFileName,
                                @NonNull String username,
                                @NonNull String password) {
        try {
            InputStream is = context.getAssets().open(assetFileName);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            this.ovpnConfig = new String(buffer);
            this.username = username;
            this.password = password;
        } catch (IOException e) {
            if (vpnListener != null) {
                vpnListener.onError("Failed to load asset: " + assetFileName + " — " + e.getMessage());
            }
        }
    }

    /**
     * Provide the .ovpn config directly as a String
     *
     * @param ovpnContent Full content of the .ovpn file
     * @param username    VPN username (pass "" if not needed)
     * @param password    VPN password (pass "" if not needed)
     */
    public void attachFromString(@NonNull String ovpnContent,
                                 @NonNull String username,
                                 @NonNull String password) {
        this.ovpnConfig = ovpnContent;
        this.username = username;
        this.password = password;
    }

    // ─── Listener ─────────────────────────────────────────────────────────────

    public void setVpnListener(@Nullable VpnListener listener) {
        this.vpnListener = listener;
    }

    // ─── Permissions ──────────────────────────────────────────────────────────

    public boolean hasVpnPermission() {
        return VpnService.prepare(context) == null;
    }

    public void requestVpnPermission() {
        if (context instanceof Activity) {
            Intent intent = VpnService.prepare(context);
            if (intent != null) {
                ((Activity) context).startActivityForResult(intent, VPN_PERMISSION_REQUEST_CODE);
            }
        }
    }

    public boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context instanceof Activity) {
            ActivityCompat.requestPermissions(
                    (Activity) context,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    // ─── VPN Control ──────────────────────────────────────────────────────────

    /**
     * Start the VPN connection.
     * Make sure you have called attachFromAsset() or attachFromString() first,
     * and that hasVpnPermission() returns true.
     */
    public void startVpn() {
        if (ovpnConfig == null || ovpnConfig.isEmpty()) {
            if (vpnListener != null) {
                vpnListener.onError("No VPN profile attached. Call attachFromAsset() or attachFromString() first.");
            }
            return;
        }

        if (!hasVpnPermission()) {
            if (vpnListener != null) {
                vpnListener.onError("VPN permission not granted. Call requestVpnPermission() first.");
            }
            return;
        }

        currentState = VpnState.CONNECTING;
        if (vpnListener != null) vpnListener.onStatusUpdate("CONNECTING");

        Intent serviceIntent = new Intent(context, NexVpnService.class);
        serviceIntent.putExtra(NexVpnService.EXTRA_OVPN_CONFIG, ovpnConfig);
        serviceIntent.putExtra(NexVpnService.EXTRA_USERNAME, username);
        serviceIntent.putExtra(NexVpnService.EXTRA_PASSWORD, password);
        serviceIntent.setAction(NexVpnService.ACTION_CONNECT);

        context.startService(serviceIntent);
        bindToService();
    }

    /**
     * Stop the active VPN connection.
     */
    public void stopVpn() {
        currentState = VpnState.DISCONNECTING;
        if (vpnListener != null) vpnListener.onStatusUpdate("DISCONNECTING");

        Intent serviceIntent = new Intent(context, NexVpnService.class);
        serviceIntent.setAction(NexVpnService.ACTION_DISCONNECT);
        context.startService(serviceIntent);

        if (isBound) {
            context.unbindService(serviceConnection);
            isBound = false;
        }
    }

    /**
     * Get the current VPN state
     */
    public VpnState getCurrentState() {
        return currentState;
    }

    /**
     * Check if VPN is currently connected
     */
    public boolean isConnected() {
        return currentState == VpnState.CONNECTED;
    }

    // ─── Service Binding ──────────────────────────────────────────────────────

    private void bindToService() {
        Intent intent = new Intent(context, NexVpnService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            NexVpnService.LocalBinder localBinder = (NexVpnService.LocalBinder) binder;
            vpnService = localBinder.getService();
            isBound = true;

            // Pass listener to service
            vpnService.setInternalListener(new NexVpnService.InternalVpnListener() {
                @Override
                public void onConnected() {
                    currentState = VpnState.CONNECTED;
                    if (vpnListener != null) vpnListener.onVpnConnected();
                }

                @Override
                public void onStopped() {
                    currentState = VpnState.DISCONNECTED;
                    if (vpnListener != null) vpnListener.onVpnStopped();
                }

                @Override
                public void onStatusUpdate(String status) {
                    if (vpnListener != null) vpnListener.onStatusUpdate(status);
                }

                @Override
                public void onError(String error) {
                    currentState = VpnState.ERROR;
                    if (vpnListener != null) vpnListener.onError(error);
                }

                @Override
                public void onSpeedUpdate(long downloadBytes, long uploadBytes,
                                          long downloadSpeed, long uploadSpeed) {
                    if (vpnListener != null) {
                        vpnListener.onSpeedUpdate(downloadBytes, uploadBytes,
                                downloadSpeed, uploadSpeed);
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            vpnService = null;
            isBound = false;
        }
    };

    // ─── Utility Methods ──────────────────────────────────────────────────────

    /**
     * Format bytes per second into human-readable speed string
     * e.g. formatSpeed(1048576) → "1.0 MB/s"
     */
    public static String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 1024) return bytesPerSecond + " B/s";
        if (bytesPerSecond < 1024 * 1024) return String.format("%.1f KB/s", bytesPerSecond / 1024.0);
        if (bytesPerSecond < 1024 * 1024 * 1024) return String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024));
        return String.format("%.1f GB/s", bytesPerSecond / (1024.0 * 1024 * 1024));
    }

    /**
     * Format total bytes into human-readable size string
     * e.g. formatBytes(1073741824) → "1.0 GB"
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Release resources. Call this in onDestroy().
     */
    public void release() {
        if (isBound) {
            try {
                context.unbindService(serviceConnection);
            } catch (Exception ignored) {}
            isBound = false;
        }
        vpnListener = null;
        vpnService = null;
    }

    // ─── Listener Interface ───────────────────────────────────────────────────

    public interface VpnListener {
        /** Called when VPN tunnel is fully established */
        void onVpnConnected();

        /** Called when VPN is disconnected */
        void onVpnStopped();

        /** Called during connection state transitions */
        void onStatusUpdate(String status);

        /** Called on connection or authentication error */
        void onError(String errorMessage);

        /**
         * Called periodically with live bandwidth statistics
         *
         * @param downloadBytes  Total bytes downloaded
         * @param uploadBytes    Total bytes uploaded
         * @param downloadSpeed  Current download speed (bytes/sec)
         * @param uploadSpeed    Current upload speed (bytes/sec)
         */
        void onSpeedUpdate(long downloadBytes, long uploadBytes,
                           long downloadSpeed, long uploadSpeed);
    }
}
