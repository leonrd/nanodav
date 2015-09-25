package io.leonrd;

import fi.iki.elonen.util.ServerRunner;

import java.io.File;

public class WebDavServerApp {

    public static void main(String[] args) {
        // Defaults
        int port = 8080;

        String host = null; // bind to all interfaces by default
        File rootDir = null;
        boolean quiet = false;

        // Parse command-line, with short and long versions of the options.
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("--host")) {
                host = args[i + 1];
            } else if (args[i].equalsIgnoreCase("-p") || args[i].equalsIgnoreCase("--port")) {
                port = Integer.parseInt(args[i + 1]);
            } else if (args[i].equalsIgnoreCase("-q") || args[i].equalsIgnoreCase("--quiet")) {
                quiet = true;
            } else if (args[i].equalsIgnoreCase("-d") || args[i].equalsIgnoreCase("--dir")) {
                rootDir = new File(args[i + 1]).getAbsoluteFile();
            }
        }

        ServerRunner.executeInstance(new WebDavServer(host, port, rootDir, quiet));
    }
}
