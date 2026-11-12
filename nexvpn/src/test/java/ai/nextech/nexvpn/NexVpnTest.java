package ai.nextech.nexvpn;

import org.junit.Test;
import static org.junit.Assert.*;

public class NexVpnTest {

    @Test
    public void testFormatSpeed() {
        assertEquals("500 B/s", NexVpn.formatSpeed(500));
        assertEquals("2.0 KB/s", NexVpn.formatSpeed(2048));
        assertEquals("3.0 MB/s", NexVpn.formatSpeed(1048576L * 3));
        assertEquals("2.0 GB/s", NexVpn.formatSpeed(1073741824L * 2));
    }

    @Test
    public void testFormatBytes() {
        assertEquals("500 B", NexVpn.formatBytes(500));
        assertEquals("2.0 KB", NexVpn.formatBytes(2048));
        assertEquals("5.0 MB", NexVpn.formatBytes(1048576L * 5));
        assertEquals("4.0 GB", NexVpn.formatBytes(1073741824L * 4));
    }

    @Test
    public void testVpnStateValues() {
        assertEquals(NexVpn.VpnState.DISCONNECTED, NexVpn.VpnState.valueOf("DISCONNECTED"));
        assertEquals(NexVpn.VpnState.CONNECTING, NexVpn.VpnState.valueOf("CONNECTING"));
        assertEquals(NexVpn.VpnState.CONNECTED, NexVpn.VpnState.valueOf("CONNECTED"));
        assertEquals(NexVpn.VpnState.DISCONNECTING, NexVpn.VpnState.valueOf("DISCONNECTING"));
        assertEquals(NexVpn.VpnState.ERROR, NexVpn.VpnState.valueOf("ERROR"));
    }
}
