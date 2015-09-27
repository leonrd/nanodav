package io.leonrd.nanodav;

/*
 * #%L
 * nanodav
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd, 2015 Leonard Chioveanu
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import fi.iki.elonen.util.ServerRunner;

import java.io.File;

public class WebDavServerApp {

    /**
     * The distribution licence
     */
    private static final String LICENCE = "Copyright (c) 2012-2013 by Paul S. Hawke, 2001,2005-2013 by Jarno Elonen, 2010 by Konstantinos Togias, 2015 Leonard Chioveanu\n" + "\n"
            + "Redistribution and use in source and binary forms, with or without\n" + "modification, are permitted provided that the following conditions\n" + "are met:\n"
            + "\n" + "Redistributions of source code must retain the above copyright notice,\n" + "this list of conditions and the following disclaimer. Redistributions in\n"
            + "binary form must reproduce the above copyright notice, this list of\n" + "conditions and the following disclaimer in the documentation and/or other\n"
            + "materials provided with the distribution. The name of the author may not\n" + "be used to endorse or promote products derived from this software without\n"
            + "specific prior written permission. \n" + " \n" + "THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR\n"
            + "IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES\n" + "OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.\n"
            + "IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,\n" + "INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT\n"
            + "NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,\n" + "DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY\n"
            + "THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n" + "(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE\n"
            + "OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.";

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
            } else if (args[i].equalsIgnoreCase("--licence")) {
                System.out.println(WebDavServerApp.LICENCE + "\n");
            }
        }

        ServerRunner.executeInstance(new WebDavServer(host, port, rootDir, quiet));
    }
}
