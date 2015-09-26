package io.leonrd.webdav;

/*
 * #%L
 * NanoHttpd-WebDavServer
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

import fi.iki.elonen.NanoHTTPD;

import java.io.*;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;

public class WebDavServer extends NanoHTTPD {

    /**
     * Common mime type for dynamic content: binary
     */
    public static final String MIME_DEFAULT_BINARY = "application/octet-stream";

    /**
     * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
     */
    @SuppressWarnings("serial")
    private static final Map<String, String> MIME_TYPES = new HashMap<String, String>() {

        {
            put("css", "text/css");
            put("htm", "text/html");
            put("html", "text/html");
            put("xml", "text/xml");
            put("java", "text/x-java-source, text/java");
            put("md", "text/plain");
            put("txt", "text/plain");
            put("asc", "text/plain");
            put("gif", "image/gif");
            put("jpg", "image/jpeg");
            put("jpeg", "image/jpeg");
            put("png", "image/png");
            put("svg", "image/svg+xml");
            put("mp3", "audio/mpeg");
            put("m3u", "audio/mpeg-url");
            put("mp4", "video/mp4");
            put("ogv", "video/ogg");
            put("flv", "video/x-flv");
            put("mov", "video/quicktime");
            put("swf", "application/x-shockwave-flash");
            put("js", "application/javascript");
            put("pdf", "application/pdf");
            put("doc", "application/msword");
            put("ogg", "application/x-ogg");
            put("zip", "application/octet-stream");
            put("exe", "application/octet-stream");
            put("class", "application/octet-stream");
            put("m3u8", "application/vnd.apple.mpegurl");
            put("ts", " video/mp2t");
        }
    };

    private final static String ALLOWED_METHODS = "GET, DELETE, OPTIONS, HEAD, PROPFIND, MKCOL, COPY, MOVE";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.getDefault());

    private boolean quiet;
    protected File rootDir;

    public WebDavServer(String host, int port, File rootDir, boolean quiet) {
        super(host, port);
        this.quiet = quiet;
        this.rootDir = rootDir;

        if (this.rootDir == null) {
            this.rootDir = new File("").getAbsoluteFile();
        }

        init();
    }

    private String appendPathComponent(final String path, final String component) {
        if (path.endsWith("/")) {
            return path + component;
        } else {
            return path + "/" + component;
        }
    }

    private boolean canServeUri(String uri) {
        return new File(rootDir, uri).exists();
    }

    /**
     * URL-encodes everything between "/"-characters. Encodes spaces as '%20'
     * instead of '+'.
     */
    private String encodeUri(String uri) {
        String newUri = "";
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("/")) {
                newUri += "/";
            } else if (tok.equals(" ")) {
                newUri += "%20";
            } else {
                try {
                    newUri += URLEncoder.encode(tok, "UTF-8");
                } catch (UnsupportedEncodingException ignored) {
                }
            }
        }
        return newUri;
    }

    // Get MIME type from file name extension, if possible
    private String getMimeTypeForFile(String uri) {
        int dot = uri.lastIndexOf('.');
        String mime = null;
        if (dot >= 0) {
            mime = WebDavServer.MIME_TYPES.get(uri.substring(dot + 1).toLowerCase());
        }
        return mime == null ? WebDavServer.MIME_DEFAULT_BINARY : mime;
    }

    protected Response getBadRequestErrorResponse(String s) {
        return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "BAD REQUEST: " + s);
    }

    protected Response getNotFoundErrorResponse(String s) {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "NOT FOUND: " + s);
    }

    protected Response getForbiddenErrorResponse(String s) {
        return newFixedLengthResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: " + s);
    }

    protected Response getInternalErrorResponse(String s) {
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "INTERNAL ERROR: " + s);
    }

    public static Response newFixedLengthResponse(Response.IStatus status, String mimeType, String message) {
        Response response = NanoHTTPD.newFixedLengthResponse(status, mimeType, message);
        response.addHeader("Accept-Ranges", "bytes");
        return response;
    }

    /**
     * Used to initialize and customize the server.
     */
    public void init() {
    }

    @Override
    public Response serve(IHTTPSession session) {

        String uri = session.getUri();
        // Remove URL arguments
        uri = uri.trim().replace(File.separatorChar, '/');
        if (uri.indexOf('?') >= 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }

        // Prohibit getting out of current directory
        if (uri.contains("../")) {
            return getForbiddenErrorResponse("Won't serve ../ for security reasons.");
        }

        final Method method = session.getMethod();
        final Map<String, String> headers = Collections.unmodifiableMap(session.getHeaders());
        final Map<String, String> parms = session.getParms();

        if (!this.quiet) {
            System.out.println(session.getMethod() + " '" + uri + "' ");

            Iterator<String> e = headers.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
                System.out.println("  HDR: '" + value + "' = '" + headers.get(value) + "'");
            }
            e = parms.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
                System.out.println("  PRM: '" + value + "' = '" + parms.get(value) + "'");
            }
        }

        if (!rootDir.isDirectory()) {
            return getInternalErrorResponse("Given root path is not a directory.");
        }

        switch (method) {
            case OPTIONS: return handleOPTIONS(headers);
            case PROPFIND: return handlePROPFIND(uri, headers);
            case GET:case HEAD: return handleGET(uri, headers);
            case DELETE: return handleDELETE(uri, headers);
            case MKCOL: return handleMKCOL(uri);
            case COPY: return handleCOPYorMOVE(uri, headers, false);
            case MOVE: return handleCOPYorMOVE(uri, headers, true);
            default: return getForbiddenErrorResponse("");
        }
    }

    protected boolean isMacFinder(Map<String, String> headers) {
        final String userAgent = headers.get("User-Agent");
        return userAgent != null && (userAgent.startsWith("WebDAVFS/") || userAgent.startsWith("WebDAVLib/"));
    }

    protected Response handleOPTIONS(final Map<String, String> headers) {
        Response response = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "");

        if (isMacFinder(headers)) {
            response.addHeader("DAV", "1, 2");
        } else {
            response.addHeader("DAV", "1");
        }
        response.addHeader("Allow", ALLOWED_METHODS);

        return response;
    }

    protected Response handlePROPFIND(String uri, final Map<String, String> headers) {

        // Remove URL arguments
        uri = uri.trim().replace(File.separatorChar, '/');
        if (uri.indexOf('?') >= 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }

        // Prohibit getting out of current directory
        if (uri.contains("../")) {
            return getForbiddenErrorResponse("Won't serve ../ for security reasons.");
        }

        if (!canServeUri(uri)) {
            return getNotFoundErrorResponse("");
        }

        final int depth;

        String depthHeader = headers.get("depth");
        if (depthHeader != null && depthHeader.equalsIgnoreCase("0")) {
            depth = 0;
        } else if (depthHeader != null && depthHeader.equalsIgnoreCase("1")) {
            depth = 1;
        } else {
            return getBadRequestErrorResponse("Unsupported 'Depth' header: " + depthHeader);
        }

        final String absolutePath = appendPathComponent(rootDir.getAbsolutePath(), uri);

        final File file = new File(absolutePath);
        if (file.isDirectory() && depth > 0) {
            if (!file.canRead()) {
                return getInternalErrorResponse("Failed listing directory " + uri);
            }
        }

        final StringBuilder xmlStringBuilder = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                "<D:multistatus xmlns:D=\"DAV:\">");
        if (file.isDirectory()) {
            if (file.list() == null) {
                return getInternalErrorResponse("Failed listing directory " + uri);
            }
            appendCollectionResource(xmlStringBuilder, uri, file, depth);
        } else {
            appendFileResource(xmlStringBuilder, uri, file);
        }
        xmlStringBuilder.append("</D:multistatus>");

        final String content = xmlStringBuilder.toString();

        return newFixedLengthResponse(Response.Status.MULTI_STATUS, MIME_TYPES.get("xml"), content);
    }

    /**
     * Appends directory info as xml to a StringBuilder
     */
    protected void appendCollectionResource(final StringBuilder output, final String uri, final File directory, final int depth) {
        final String displayName = directory.getName();
        // TODO: if possible, properly handle creation date
        final String creationDate = DATE_FORMAT.format(new Date(directory.lastModified()));
        final String lastModified = DATE_FORMAT.format(new Date(directory.lastModified()));

        output.append("<D:response>" +
                "<D:href>" + uri + "</D:href>" +
                "<D:propstat>" +
                "<D:prop>" +
                "<D:displayname>" + displayName + "</D:displayname>" +
                "<D:creationdate>" + creationDate + "</D:creationdate>" +
                "<D:getlastmodified>" + lastModified + "</D:getlastmodified>" +
                "<D:resourcetype><D:collection/></D:resourcetype>" +
                "</D:prop>" +
                "<D:status>HTTP/1.1 200 OK</D:status>" +
                "</D:propstat>" +
                "</D:response>");

        if (depth > 0) {
            final File files[] = directory.listFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                String subUri = appendPathComponent(uri, encodeUri(file.getName()));

                if (file.isDirectory()) {
                    appendCollectionResource(output, subUri, file, depth - 1);
                } else {
                    appendFileResource(output, subUri, file);
                }
            }
        }
    }

    /**
     * Appends file info as xml to a StringBuilder
     */
    protected void appendFileResource(final StringBuilder output, final String uri, final File file) {
        final String displayName = file.getName();
        // TODO: if possible, properly handle creation date
        final String creationDate = DATE_FORMAT.format(new Date(file.lastModified()));
        final String lastModified = DATE_FORMAT.format(new Date(file.lastModified()));
        final long contentLength = file.length();

        output.append("<D:response>" +
                "<D:href>" + uri + "</D:href>" +
                "<D:propstat>" +
                "<D:prop>" +
                "<D:displayname>" + displayName + "</D:displayname>" +
                "<D:creationdate>" + creationDate + "</D:creationdate>" +
                "<D:getlastmodified>" + lastModified + "</D:getlastmodified>" +
                "<D:getcontentlength>" + contentLength + "</D:getcontentlength>" +
                "<D:resourcetype/>" +
                "</D:prop>" +
                "<D:status>HTTP/1.1 200 OK</D:status>" +
                "</D:propstat>" +
                "</D:response>");
    }

    protected Response handleGET(final String uri, final Map<String, String> headers) {

        if (!canServeUri(uri)) {
            return getNotFoundErrorResponse("");
        }

        final String absolutePath = appendPathComponent(rootDir.getAbsolutePath(), uri);
        final File file = new File(absolutePath);

        // Because HEAD requests are mapped to GET ones, we need to handle directories but it's OK to return nothing per http://webdav.org/specs/rfc4918.html#rfc.section.9.4
        if (file.isDirectory()) {
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "");
        }

        String mimeTypeForFile = getMimeTypeForFile(uri);

        return serveFile(uri, headers, file, mimeTypeForFile);
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI,
     * ignores all headers and HTTP parameters.
     */
    Response serveFile(String uri, Map<String, String> header, File file, String mime) {
        Response res;
        try {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // get if-range header. If present, it must match etag or else we
            // should ignore the range request
            String ifRange = header.get("if-range");
            boolean headerIfRangeMissingOrMatching = (ifRange == null || etag.equals(ifRange));

            String ifNoneMatch = header.get("if-none-match");
            boolean headerIfNoneMatchPresentAndMatching = ifNoneMatch != null && (ifNoneMatch.equals("*") || ifNoneMatch.equals(etag));

            // Change return code and add Content-Range header when skipping is
            // requested
            long fileLen = file.length();

            if (headerIfRangeMissingOrMatching && range != null && startFrom >= 0 && startFrom < fileLen) {
                // range request that matches current etag
                // and the startFrom of the range is satisfiable
                if (headerIfNoneMatchPresentAndMatching) {
                    // range request that matches current etag
                    // and the startFrom of the range is satisfiable
                    // would return range from file
                    // respond with not-modified
                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    FileInputStream fis = new FileInputStream(file);
                    fis.skip(startFrom);

                    res = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mime, fis, newLen);
                    res.addHeader("Accept-Ranges", "bytes");
                    res.addHeader("Content-Length", "" + newLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {

                if (headerIfRangeMissingOrMatching && range != null && startFrom >= fileLen) {
                    // return the size of the file
                    // 4xx responses are not trumped by if-none-match
                    res = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes */" + fileLen);
                    res.addHeader("ETag", etag);
                } else if (range == null && headerIfNoneMatchPresentAndMatching) {
                    // full-file-fetch request
                    // would return entire file
                    // respond with not-modified
                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else if (!headerIfRangeMissingOrMatching && headerIfNoneMatchPresentAndMatching) {
                    // range request that doesn't match current etag
                    // would return entire (different) file
                    // respond with not-modified

                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else {
                    // supply the file
                    res = newFixedFileResponse(file, mime);
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = getForbiddenErrorResponse("Reading file failed.");
        }

        return res;
    }

    protected Response handleDELETE(final String uri, final Map<String, String> headers) {

        String depthHeader = headers.get("depth");
        if (depthHeader != null && !depthHeader.equalsIgnoreCase("infinity")) {
            return getBadRequestErrorResponse("Unsupported 'Depth' header: " + depthHeader);
        }

        if (!canServeUri(uri)) {
            return getNotFoundErrorResponse("");
        }

        final String absolutePath = appendPathComponent(rootDir.getAbsolutePath(), uri);
        final File file = new File(absolutePath);

        if (!file.delete()) {
            return getInternalErrorResponse("Failed deleting " + uri);
        }

        return newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "");
    }

    protected Response handleMKCOL(final String uri) {

        final String absolutePath = appendPathComponent(rootDir.getAbsolutePath(), uri);
        final File file = new File(absolutePath);

        if (!file.mkdirs()) {
            return getInternalErrorResponse("Failed creating directory " + uri);
        }

        return newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "");
    }

    protected Response handleCOPYorMOVE(final String uri, final Map<String, String> headers, final boolean move) {

        if (!move) {
            String depthHeader = headers.get("depth");
            // TODO: Support "Depth: 0"
            if (depthHeader != null && !depthHeader.equalsIgnoreCase("infinity")) {
                return getBadRequestErrorResponse("Unsupported 'Depth' header: " + depthHeader);
            }
        }

        if (!canServeUri(uri)) {
            return getNotFoundErrorResponse("");
        }

        final String srcRelativePath = uri;
        final String srcAbsolutePath = appendPathComponent(rootDir.getAbsolutePath(), uri);

        String dstRelativePath = headers.get("destination");
        final String hostHeader = headers.get("host");
        if (dstRelativePath == null || hostHeader == null || !dstRelativePath.contains(hostHeader)) {
            return getBadRequestErrorResponse("Malformed 'Destination' header: " + dstRelativePath);
        }
        dstRelativePath = dstRelativePath.substring(dstRelativePath.indexOf(hostHeader) + hostHeader.length());
        final String dstAbsolutePath = appendPathComponent(rootDir.getAbsolutePath(), dstRelativePath);

        final File srcFile = new File(srcAbsolutePath);
        final File dstFile = new File(dstAbsolutePath);
        final File dstParent = dstFile.getParentFile();
        final boolean existing = dstFile.exists();
        if (!dstParent.exists() || !dstParent.isDirectory()) {
            return newFixedLengthResponse(Response.Status.CONFLICT, MIME_PLAINTEXT, "Invalid destination " + dstRelativePath);
        }

        final String overwriteHeader = headers.get("overwrite");
        if (existing &&
                (overwriteHeader == null
                        || (move && !overwriteHeader.equalsIgnoreCase("T"))
                        || (!move && overwriteHeader.equalsIgnoreCase("F")))) {

            return newFixedLengthResponse(Response.Status.PRECONDITION_FAILED, MIME_PLAINTEXT, "Destination " + dstRelativePath + " already exists");
        }

        if (existing) {
            dstFile.delete();
        }

        if (move) {
            if (!srcFile.renameTo(dstFile)) {
                return getForbiddenErrorResponse("Failed moving " + srcRelativePath + " to " + dstRelativePath);
            }
        }
        else {
            if (!copyFileOrDirectory(srcFile, dstFile)) {
                return getForbiddenErrorResponse("Failed copying " + srcRelativePath + " to " + dstRelativePath);
            }
        }

        return newFixedLengthResponse(existing ? Response.Status.NO_CONTENT : Response.Status.CREATED, MIME_PLAINTEXT, "");
    }

    public static boolean copyFileOrDirectory(final File srcFile, final File dstFile) {
        boolean result = true;
        try {
            if (srcFile.isDirectory()) {

                String files[] = srcFile.list();
                int filesLength = files.length;
                for (int i = 0; i < filesLength; i++) {
                    File src1 = new File(srcFile, files[i]);
                    copyFileOrDirectory(src1, dstFile);

                }
            } else {
                copyFile(srcFile, dstFile);
            }
        } catch (IOException e) {
            result = false;
            e.printStackTrace();
        }

        return result;
    }

    public static boolean copyFile(File srcFile, File dstFile) throws IOException {
        boolean result = true;
        if (!dstFile.getParentFile().exists())
            dstFile.getParentFile().mkdirs();

        if (!dstFile.exists()) {
            dstFile.createNewFile();
        }

        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            result = false;
            e.printStackTrace();
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
        return result;
    }

    protected Response handlePUT(final String uri) {
        return null;
    }

    protected Response handleLOCK(final String uri) {
        return null;
    }

    protected Response handleUNLOCK(final String uri) {
        return null;
    }

    private Response newFixedFileResponse(File file, String mime) throws FileNotFoundException {
        Response res;
        res = newFixedLengthResponse(Response.Status.OK, mime, new FileInputStream(file), (int) file.length());
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }
}
