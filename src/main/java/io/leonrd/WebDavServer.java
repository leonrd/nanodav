package io.leonrd;

import fi.iki.elonen.NanoHTTPD;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

public class WebDavServer extends NanoHTTPD {

    @Override
    public Response serve(IHTTPSession session) {

        String uri = session.getUri();
        Method method = session.getMethod();
        Map<String, String> header = session.getHeaders();
        Map<String, String> parms = session.getParms();

        if (!this.quiet) {
            System.out.println(session.getMethod() + " '" + uri + "' ");

            Iterator<String> e = header.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
                System.out.println("  HDR: '" + value + "' = '" + header.get(value) + "'");
            }
            e = parms.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
                System.out.println("  PRM: '" + value + "' = '" + parms.get(value) + "'");
            }
        }

        String msg = "<html><body><h1>Hello server</h1>\n";
        if (parms.get("username") == null) {
            msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
        } else {
            msg += "<p>Hello, " + parms.get("username") + "!</p>";
        }
        return newFixedLengthResponse(msg + "</body></html>\n");
    }

    private final boolean quiet;

    protected File rootDir;

    public WebDavServer(int port, File rootDir) {
        this(null, port, rootDir, false);
    }

    public WebDavServer(int port, File rootDir, boolean quiet) {
        this(null, port, rootDir, quiet);
    }

    public WebDavServer(String host, int port, File rootDir, boolean quiet) {
        super(host, port);
        this.quiet = quiet;
        this.rootDir = rootDir;

        if (this.rootDir == null) {
            this.rootDir = new File("").getAbsoluteFile();
        }

        init();
    }

    /**
     * Used to initialize and customize the server.
     */
    public void init() {
    }
}
