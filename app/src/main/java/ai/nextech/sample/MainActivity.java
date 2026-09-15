package ai.nextech.sample;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import ai.nextech.nexvpn.NexVpn;

public class MainActivity extends AppCompatActivity {

    private NexVpn nexVpn;
    private Button btnConnect;
    private TextView tvStatus, tvSpeed, tvUsage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ── Views ──────────────────────────────────────────────
        btnConnect = findViewById(R.id.btnConnect);
        tvStatus   = findViewById(R.id.tvStatus);
        tvSpeed    = findViewById(R.id.tvSpeed);
        tvUsage    = findViewById(R.id.tvUsage);

        // ── Init NexVPN ────────────────────────────────────────
        nexVpn = new NexVpn(this);

        // Request notification permission on Android 13+
        if (!nexVpn.hasNotificationPermission()) {
            nexVpn.requestNotificationPermission();
        }

        // Attach your .ovpn profile from assets
        nexVpn.attachFromAsset("server.ovpn", "vpn_user", "vpn_pass");

        // ── Listener ───────────────────────────────────────────
        nexVpn.setVpnListener(new NexVpn.VpnListener() {
            @Override
            public void onVpnConnected() {
                runOnUiThread(() -> {
                    tvStatus.setText("● Connected");
                    btnConnect.setText("Disconnect");
                });
            }

            @Override
            public void onVpnStopped() {
                runOnUiThread(() -> {
                    tvStatus.setText("● Disconnected");
                    btnConnect.setText("Connect");
                    tvSpeed.setText("↓ 0 B/s   ↑ 0 B/s");
                    tvUsage.setText("Usage: 0 B");
                });
            }

            @Override
            public void onStatusUpdate(String status) {
                runOnUiThread(() -> tvStatus.setText("⟳ " + status));
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> tvStatus.setText("✗ " + errorMessage));
            }

            @Override
            public void onSpeedUpdate(long downloadBytes, long uploadBytes,
                                      long downloadSpeed, long uploadSpeed) {
                runOnUiThread(() -> {
                    tvSpeed.setText("↓ " + NexVpn.formatSpeed(downloadSpeed)
                            + "   ↑ " + NexVpn.formatSpeed(uploadSpeed));
                    tvUsage.setText("Usage: " + NexVpn.formatBytes(downloadBytes + uploadBytes));
                });
            }
        });

        // ── Button ─────────────────────────────────────────────
        btnConnect.setOnClickListener(v -> {
            if (nexVpn.isConnected()) {
                nexVpn.stopVpn();
            } else {
                if (nexVpn.hasVpnPermission()) {
                    nexVpn.startVpn();
                } else {
                    nexVpn.requestVpnPermission();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (nexVpn != null) nexVpn.release();
    }
}
