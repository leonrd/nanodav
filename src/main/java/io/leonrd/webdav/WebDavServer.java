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
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

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
            put("xml", "application/xml");
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

    private boolean quiet;
    protected File rootDir;
    private DateFormat dateFormat;

    public WebDavServer(String host, int port, File rootDir, boolean quiet) {
        super(host, port);
        this.quiet = quiet;
        this.rootDir = rootDir;

        if (this.rootDir == null) {
            this.rootDir = new File("").getAbsoluteFile();
        }

        this.dateFormat = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.getDefault());
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        this.dateFormat.setLenient(false);

        init();
    }

    private String appendPathComponent(final String path, final String component) {
        if (path.endsWith("/") || component.startsWith("/")) {
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
        return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_HTML, "BAD REQUEST: " + s);
    }

    protected Response getNotFoundErrorResponse(String s) {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_HTML, "NOT FOUND: " + s);
    }

    protected Response getForbiddenErrorResponse(String s) {
        return newFixedLengthResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_HTML, "FORBIDDEN: " + s);
    }

    protected Response getMethodNotAllowed(String s) {
        return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_HTML, "METHOD NOT ALLOWED: " + s);

    }

    protected Response getInternalErrorResponse(String s) {
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, "INTERNAL ERROR: " + s);
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

        Response  response;

        switch (method) {
            case OPTIONS: response = handleOPTIONS(headers); break;
            case PROPFIND: response = handlePROPFIND(uri, headers); break;
            case GET:case HEAD: response = handleGET(uri, headers); break;
            case DELETE: response = handleDELETE(uri, headers); break;
            case MKCOL: response = handleMKCOL(uri); break;
            case COPY: response = handleCOPYorMOVE(uri, headers, false); break;
            case MOVE: response = handleCOPYorMOVE(uri, headers, true); break;
            case PUT: response = handlePUT(uri, session); break;
            case LOCK: response = handleLOCK(uri, headers, session); break;
            case UNLOCK: response = handleUNLOCK(uri, headers, session); break;
            default: response = getForbiddenErrorResponse(""); break;
        }

        if (!this.quiet) {
            System.out.println("  STATUS: " + response.getStatus());
        }

        return response;
    }

    protected boolean isMacFinder(Map<String, String> headers) {
        final String userAgent = headers.get("user-agent");
        return userAgent != null && (userAgent.startsWith("WebDAVFS/") || userAgent.startsWith("WebDAVLib/"));
    }

    protected Response handleOPTIONS(final Map<String, String> headers) {
        Response response = newFixedLengthResponse(Response.Status.OK, MIME_HTML, "");
        if (isMacFinder(headers)) {
            response.addHeader("DAV", "1, 2");
        } else {
            response.addHeader("DAV", "1");
        }
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
            return getNotFoundErrorResponse(uri + " does not exist.");
        }

        final int depth;

        String depthHeader = headers.get("depth");
        // TODO: Return 403 / propfind-finite-depth for "infinity" depth
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

        final StringBuilder xmlStringBuilder = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "<D:multistatus xmlns:D=\"DAV:\">\n");
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
        final String lastModified = dateFormat.format(new Date(directory.lastModified()));

        output.append("<D:response>" +
                "<D:href>" + uri + "</D:href>\n" +
                "<D:propstat>\n" +
                "<D:prop>\n" +
                "<D:displayname>" + displayName + "</D:displayname>\n" +
//                "<D:creationdate>" + creationDate + "</D:creationdate\n>" +
                "<D:getlastmodified>" + lastModified + "</D:getlastmodified>\n" +
                "<D:resourcetype><D:collection/></D:resourcetype>\n" +
                "</D:prop>\n" +
                "<D:status>HTTP/1.1 200 OK</D:status>\n" +
                "</D:propstat>\n" +
                "</D:response>\n");

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
        final String lastModified = dateFormat.format(new Date(file.lastModified()));
        final long contentLength = file.length();

        output.append("<D:response>\n" +
                "<D:href>" + uri + "</D:href>\n" +
                "<D:propstat>\n" +
                "<D:prop>\n" +
                "<D:displayname>" + displayName + "</D:displayname>\n" +
//                "<D:creationdate>" + creationDate + "</D:creationdate>\n" +
                "<D:getlastmodified>" + lastModified + "</D:getlastmodified>\n" +
                "<D:getcontentlength>" + contentLength + "</D:getcontentlength>\n" +
                "<D:resourcetype/>\n" +
                "</D:prop>\n" +
                "<D:status>HTTP/1.1 200 OK</D:status>\n" +
                "</D:propstat>\n" +
                "</D:response>\n");
    }

    protected Response handleGET(final String uri, final Map<String, String> headers) {
        if (!canServeUri(uri)) {
            return getNotFoundErrorResponse("");
        }

        final String absolutePath = appendPathComponent(rootDir.getAbsolutePath(), uri);
        final File file = new File(absolutePath);

        // Because HEAD requests are mapped to GET ones, we need to handle directories but it's OK to return nothing per http://webdav.org/specs/rfc4918.html#rfc.section.9.4
        if (file.isDirectory()) {
            return newFixedLengthResponse(Response.Status.OK, MIME_HTML, "");
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
                    res = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_HTML, "");
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

        return newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_HTML, "");
    }

    protected Response handleMKCOL(final String uri) {

        final String absolutePath = appendPathComponent(rootDir.getAbsolutePath(), uri);
        final File file = new File(absolutePath);

        if (!file.mkdirs()) {
            return getInternalErrorResponse("Failed creating directory " + uri);
        }

        return newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_HTML, "");
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
            return getNotFoundErrorResponse(uri + " does not exist.");
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
            return newFixedLengthResponse(Response.Status.CONFLICT, MIME_HTML, "Invalid destination " + dstRelativePath);
        }

        boolean overwrite = false;
        final String overwriteHeader = headers.get("overwrite");
        if (overwriteHeader == null
                || (move && !overwriteHeader.equalsIgnoreCase("T"))
                || (!move && overwriteHeader.equalsIgnoreCase("F"))) {
                overwrite = true;
        }

        if (existing && !overwrite) {
            return newFixedLengthResponse(Response.Status.PRECONDITION_FAILED, MIME_HTML, "Destination " + dstRelativePath + " already exists");
        }

        if (existing) {
            dstFile.delete();
        }

        try {
            if (move) {
                if (!srcFile.renameTo(dstFile)) {
                    return getForbiddenErrorResponse("Failed moving " + srcRelativePath + " to " + dstRelativePath);
                }
            }
            else {
                copyFileOrDirectory(srcFile, dstFile);
            }
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_HTML, e.getMessage());
        }

        return newFixedLengthResponse(existing ? Response.Status.NO_CONTENT : Response.Status.CREATED, MIME_HTML, "");
    }

    public static void copyFileOrDirectory(final File srcFile, final File dstFile) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;

        try {
            if (srcFile.isDirectory()) {
                if (!dstFile.exists()) {
                    dstFile.mkdirs();
                }

                String children[] = srcFile.list();
                int childrenLength = children.length;
                for (int i = 0; i < childrenLength; i++) {
                    File srcChild = new File(srcFile, children[i]);
                    File dstChild = new File(dstFile, children[i]);
                    copyFileOrDirectory(srcChild, dstChild);
                }
            } else {
                if (!dstFile.getParentFile().exists()) {
                    dstFile.getParentFile().mkdirs();
                }
                if (!dstFile.exists()) {
                    dstFile.createNewFile();
                }
                in = new FileInputStream(srcFile);
                out = new FileOutputStream(dstFile);

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    protected Response handlePUT(final String uri, final IHTTPSession session) {
        final String dstRelativePath = uri;
        final String dstAbsolutePath = appendPathComponent(rootDir.getAbsolutePath(), uri);
        final File dstFile = new File(dstAbsolutePath);
        final boolean existing = dstFile.exists();
        final File dstParent = dstFile.getParentFile();
        if (!dstParent.exists() || !dstParent.isDirectory()) {
            return newFixedLengthResponse(Response.Status.CONFLICT, MIME_HTML, "Missing intermediate collection(s) for " + dstRelativePath);
        }

        if (existing && dstFile.isDirectory()) {
            return getMethodNotAllowed("PUT not allowed on existing collection " + dstRelativePath);
        }
        try {
            Map<String, String> files = new HashMap<String, String>();
            session.parseBody(files);

            Set<String> keys = files.keySet();
            for(String key: keys){
                String tempLocation = files.get(key);
                File tempfile = new File(tempLocation);

                if (existing) {
                    dstFile.delete();
                }

                copyFileOrDirectory(tempfile, dstFile);
            }
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_HTML, e.getMessage());
        } catch (ResponseException e) {
            return newFixedLengthResponse(e.getStatus(), MIME_HTML, e.getMessage());
        }

        return newFixedLengthResponse(existing ? Response.Status.NO_CONTENT : Response.Status.CREATED, MIME_HTML, "");
    }

    protected Response handleLOCK(final String uri, final Map<String, String> headers, final IHTTPSession session) {
        if (!isMacFinder(headers)) {
            return getMethodNotAllowed("LOCK method only allowed for Mac Finder");
        }
        if (!canServeUri(uri)) {
            return getNotFoundErrorResponse(uri + " does not exist.");
        }

        final String depthHeader = headers.get("depth");
        final String timeoutHeader = headers.get("timeout");
        final String contentLengthHeader = headers.get("content-length");
        int contentLength = 0;
        if (contentLengthHeader != null) {
            contentLength = Integer.parseInt(contentLengthHeader);
        }

        String scope = null;
        String type = null;
        String owner = null;
        String token = null;
        boolean success = true;

        InputStream sessionInputStream = session.getInputStream();
        InputStream byteInputStream = null;
        try {

            // convert PullInputStream to ByteArrayInputStream
            byte[] data = new byte[contentLength];
            sessionInputStream.read(data);
            byteInputStream = new ByteArrayInputStream(data);

            final String ns = "DAV:";
            XmlPullParser parser = new KXmlParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(byteInputStream, null);

            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, ns, "lockinfo");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                if (name.equals("lockscope")) {
                    scope = readScope(parser, ns);
                } else if (name.equals("locktype")) {
                    type = readType(parser, ns);
                } else if (name.equals("owner")) {
                    owner = readOwner(parser, ns);
                } else {
                    skip(parser);
                }
            }

        } catch (XmlPullParserException e) {
            success = false;
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        } finally {
            try {
                if (byteInputStream != null) {
                    byteInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!success) {
            return getBadRequestErrorResponse("Invalid DAV properties");
        }

        if (!scope.equalsIgnoreCase("exclusive") || !type.equalsIgnoreCase("write") || !depthHeader.equalsIgnoreCase("0")) {
            return getForbiddenErrorResponse("Locking request " + scope + " " + type + " " + depthHeader + " " + uri +  " is not allowed");
        }

        token = "urn:uuid:" + UUID.randomUUID();

        String content = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "<D:prop xmlns:D=\"DAV:\">\n" +
                "<D:lockdiscovery>\n<D:activelock>\n" +
                "<D:locktype><D:" + type + "/></D:locktype>\n" +
                "<D:lockscope><D:" + scope + "/></D:lockscope>\n" +
                "<D:depth>" + depthHeader + "</D:depth>\n";
        if (owner != null) {
            content += "<D:owner><D:href>" + owner + "</D:href></D:owner>\n";
        }
        if (timeoutHeader != null) {
            content += "<D:timeout>" + timeoutHeader + "</D:timeout>\n";
        }
        content += "<D:locktoken><D:href>" + token + "</D:href></D:locktoken>\n" +
                "<D:lockroot><D:href>http://" + encodeUri(appendPathComponent(headers.get("host"), uri)) + "</D:href></D:lockroot>\n" +
                "</D:activelock>\n</D:lockdiscovery>\n" +
                "</D:prop>";

        return newFixedLengthResponse(Response.Status.OK, MIME_TYPES.get("xml"), content);
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private String readScope(XmlPullParser parser, String namespace) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, namespace, "lockscope");
        parser.nextTag();
        String scope = parser.getName();
        parser.nextTag();
        parser.nextTag();
        parser.require(XmlPullParser.END_TAG, namespace, "lockscope");
        return scope;
    }

    private String readType(XmlPullParser parser, String namespace) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, namespace, "locktype");
        parser.nextTag();
        String type = parser.getName();
        parser.nextTag();
        parser.nextTag();
        parser.require(XmlPullParser.END_TAG, namespace, "locktype");
        return type;
    }

    private String readOwner(XmlPullParser parser, String namespace) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, namespace, "owner");
        parser.nextTag();
        String owner = readHref(parser, namespace);
        parser.nextTag();
        parser.require(XmlPullParser.END_TAG, namespace, "owner");
        return owner;
    }

    private String readHref(XmlPullParser parser, String namespace) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, namespace, "href");
        String href = readText(parser);
        parser.require(XmlPullParser.END_TAG, namespace, "href");
        return href;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    protected Response handleUNLOCK(final String uri, final Map<String, String> headers, final IHTTPSession session) {
        if (!isMacFinder(headers)) {
            return getMethodNotAllowed("UNLOCK method only allowed for Mac Finder");
        }
        if (!canServeUri(uri)) {
            return getNotFoundErrorResponse(uri + " does not exist.");
        }

        String tokenHeader = headers.get("Lock-Token");
        if (tokenHeader == null) {
            return getBadRequestErrorResponse("Missing 'Lock-Token' header");
        }

        return newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_HTML, "");
    }

    private Response newFixedFileResponse(File file, String mime) throws FileNotFoundException {
        Response res;
        res = newFixedLengthResponse(Response.Status.OK, mime, new FileInputStream(file), (int) file.length());
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }
}
