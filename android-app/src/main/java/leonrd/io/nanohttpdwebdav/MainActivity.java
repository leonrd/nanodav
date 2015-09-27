package leonrd.io.nanohttpdwebdav;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import fi.iki.elonen.NanoHTTPD;
import io.leonrd.webdav.WebDavServer;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "NanoHttpdWebDav";

    private String address;
    private int port = 8080;
    private WebDavServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        address = getLocalIpAddress();
    }

    @Override
    protected void onResume() {
        super.onResume();

//        port = getAvailablePort();
        startServer(port);

        TextView urlView = (TextView) findViewById(R.id.url);
        urlView.setText("Now listening on http://" + address + ":" + port + "/");
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopServer();
    }

    private void startServer(int port) {
        // run the server
        try {
            server = new WebDavServer(null, port, Environment.getExternalStorageDirectory(), false);
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            Log.d(TAG, "Now listening on http://" + address + ":" + port + "/");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopServer() {
        // stop server
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if ((inetAddress instanceof Inet4Address) && !inetAddress.isLoopbackAddress()) {
                        // I don’t know how NanoHTTPD, bonjour etc feel about ipv6 addresses
                        // So to be on the safe side, we filter to ipv4 only
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }
        return null;
    }

    private int getAvailablePort() {
        // obtain a free port number by binding to a socket and then unbinding again so the port is available to us
        int port = 0;
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            // Store the chosen port.
            port = socket.getLocalPort();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return port;
    }
}
