package com.tt.droidplay;

import android.util.Base64;
import android.util.Log;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * Class that handles per-client work in the server.
 *
 * @author Tuomas Tikka
 */
public class ClientWorker implements Runnable {

    private static final String TAG = "ClientWorker";

    private Socket socket;

    private int id;

    public ClientWorker(Socket socket, int id) {
        this.socket = socket;
        this.id = id;
    }

    @Override
    public void run() {
        Log.d(TAG, "[" + id + "] client connection from " + socket.getInetAddress().getHostAddress());
        try {
            Log.d(TAG, "[" + id + "] socket recv buffer size = " + socket.getReceiveBufferSize());
            Log.d(TAG, "[" + id + "] socket send buffer size = " + socket.getSendBufferSize());
            HttpHead head = parseHead(socket.getInputStream());
            if (head == null) {
                handle400(socket, true);
                return;
            }
            boolean close = closeClientSocket(head);
            handleDownload(socket, head, close);
        } catch (IOException e) {
            Log.w(TAG, "[" + id + "] could not read from client: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void closeClientSocket(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
                Log.d(TAG, "[" + id + "] client socket closed");
            } catch (Exception e) {
                Log.w(TAG, "[" + id + "] could not close client socket: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }

    private static boolean isHeadTerminated(String line) {
        return (line == null || line.isEmpty());
    }

    public HttpHead parseHead(InputStream in) {
        try {
            HttpHead head = new HttpHead();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            int i = 0;
            while (!isHeadTerminated(line = br.readLine())) {
                if (i == 0) {
                    StringTokenizer st = new StringTokenizer(line, " ");
                    if (st.countTokens() == 3) {
                        head.setMethod(st.nextToken().trim());
                        head.setUri(st.nextToken().trim());
                        head.setProtocol(st.nextToken().trim());
                    }
                } else {
                    int j = line.indexOf(":");
                    if (j != -1) {
                        String name = line.substring(0, j);
                        String value = line.substring(j + 2);
                        head.getHeaders().put(name, value);
                    }
                }
                i++;
            }
            return (head);
        } catch (Exception e) {
            return (null);
        }
    }

    private void handle400(Socket socket, boolean close) {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(socket.getOutputStream());
            pw.println("HTTP/1.1 400 Bad Request");
            pw.println("Date: " + getDateHeader());
            pw.println("Server: " + getServerHeader());
            pw.println("Content-Length: 0");
            if (close) {
                pw.println("Connection: close");
            } else {
                pw.println("Connection: keep-alive");
            }
            pw.println();
        } catch (IOException e) {
            Log.e(TAG, "[" + id + "] could not respond to client (HTTP 400): " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            if (pw != null) {
                pw.close();
            }
            if (close) {
                closeClientSocket(socket);
            }
        }
    }

    private void handle403(Socket socket, boolean close) {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(socket.getOutputStream());
            pw.println("HTTP/1.1 403 Forbidden");
            pw.println("Date: " + getDateHeader());
            pw.println("Server: " + getServerHeader());
            pw.println("Content-Length: 0");
            if (close) {
                pw.println("Connection: close");
            } else {
                pw.println("Connection: keep-alive");
            }
            pw.println();
        } catch (IOException e) {
            Log.w(TAG, "[" + id + "] could not respond to client (HTTP 403): " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            if (pw != null) {
                pw.close();
            }
            if (close) {
                closeClientSocket(socket);
            }
        }
    }

    private void handle404(Socket socket, boolean close) {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(socket.getOutputStream());
            pw.println("HTTP/1.1 404 Not Found");
            pw.println("Date: " + getDateHeader());
            pw.println("Server: " + getServerHeader());
            pw.println("Content-Length: 0");
            if (close) {
                pw.println("Connection: close");
            } else {
                pw.println("Connection: keep-alive");
            }
            pw.println();
        } catch (IOException e) {
            Log.w(TAG, "[" + id + "] could not respond to client (HTTP 404): " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            if (pw != null) {
                pw.close();
            }
            if (close) {
                closeClientSocket(socket);
            }
        }
    }

    private void handleDownload(Socket socket, HttpHead head, boolean close) {
        String path;
        try {
            path = new String(Base64.decode(head.getUri(), Base64.NO_WRAP | Base64.URL_SAFE), "UTF-8");
            Log.d(TAG, "[" + id + "] parsed download path: " + path);
        } catch (Exception e) {
            Log.w(TAG, "[" + id + "] could not parse uri from request: " + e.getMessage());
            e.printStackTrace(System.err);
            handle400(socket, close);
            return;
        }
        File file = new File(path);
        if (!file.exists()) {
            Log.w(TAG, "[" + id + "] file not found: " + path);
            handle404(socket, close);
            return;
        }
        if (!file.canRead()) {
            Log.w(TAG, "[" + id + "] cannot read file: " + path);
            handle403(socket, close);
            return;
        }
        Log.d(TAG, "[" + id + "] found file size " + file.length());
        long start = -1;
        long end = -1;
        String range = head.getHeaders().get("Range");
        if (range != null) {
            long[] l = parseRangeRequestHeader(range, file.length());
            if (l != null) {
                start = l[0];
                end = l[1];
            } else {
                Log.w(TAG, "[" + id + "] invalid range: " + range);
            }
        }
        if (start != -1 && end != -1) {
            Log.d(TAG, "[" + id + "] detected range download: start = " + start + ", end = " + end);
            BufferedOutputStream out = null;
            try {
                out = new BufferedOutputStream(socket.getOutputStream());
                out.write("HTTP/1.1 206 Partial Content\n".getBytes("UTF-8"));
                setResponseHeader("Date", getDateHeader(), out);
                setResponseHeader("Last-Modified", getDateHeader(file.lastModified()), out);
                setResponseHeader("Server", getServerHeader(), out);
                setResponseHeader("Accept-Ranges", "bytes", out);
                setResponseHeader("Content-Length", "" + (end - start + 1), out);
                setResponseHeader("Content-Range", "bytes " + start + "-" + end + "/" + file.length(), out);
                setResponseHeader("Content-Type", "video/mp4", out);
                if (close) {
                    setResponseHeader("Connection", "close", out);
                } else {
                    setResponseHeader("Connection", "keep-alive", out);
                }
                setResponseHeader("Cache-Control", "private, max-age=0", out);
                out.write("\n".getBytes("UTF-8"));
                FileChannel fc = new FileInputStream(file).getChannel();
                ByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, start, end - start + 1);
                WritableByteChannel wbc = Channels.newChannel(out);
                wbc.write(buffer);
                fc.close();
            } catch (Exception e) {
                Log.w(TAG, "[" + id + "] error streaming data to client (HTTP 206): " + e.getMessage());
                e.printStackTrace(System.err);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Exception e) {
                        Log.w(TAG, "[" + id + "] could not close output stream (HTTP 206): " + e.getMessage());
                        e.printStackTrace(System.err);
                    }
                }
                if (close) {
                    closeClientSocket(socket);
                }
            }
            Log.d(TAG, "[" + id + "] range download complete");
        } else {
            Log.d(TAG, "[" + id + "] detected full download");
            BufferedOutputStream out = null;
            BufferedInputStream in = null;
            try {
                out = new BufferedOutputStream(socket.getOutputStream());
                out.write("HTTP/1.1 200 OK\n".getBytes("UTF-8"));
                setResponseHeader("Date", getDateHeader(), out);
                setResponseHeader("Last-Modified", getDateHeader(file.lastModified()), out);
                setResponseHeader("Server", getServerHeader(), out);
                setResponseHeader("Accept-Ranges", "bytes", out);
                setResponseHeader("Content-Length", "" + file.length(), out);
                setResponseHeader("Content-Type", "video/mp4", out);
                if (close) {
                    setResponseHeader("Connection", "close", out);
                } else {
                    setResponseHeader("Connection", "keep-alive", out);
                }
                setResponseHeader("Cache-Control", "private, max-age=0", out);
                out.write("\n".getBytes("UTF-8"));
                FileChannel fc = new FileInputStream(file).getChannel();
                ByteBuffer buffer = ByteBuffer.allocate(32 * 1024);
                WritableByteChannel wbc = Channels.newChannel(out);
                while (fc.read(buffer) != -1) {
                    buffer.flip();
                    wbc.write(buffer);
                    buffer.clear();
                }
                fc.close();
            } catch (Exception e) {
                Log.w(TAG, "[" + id + "] error streaming data to client (HTTP 200): " + e.getMessage());
                e.printStackTrace(System.err);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e) {
                        Log.w(TAG, "[" + id + "] could not close input stream (HTTP 200): " + e.getMessage());
                        e.printStackTrace(System.err);
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (Exception e) {
                        Log.w(TAG, "[" + id + "] could not close output stream (HTTP 200): " + e.getMessage());
                        e.printStackTrace(System.err);
                    }
                }
                if (close) {
                    closeClientSocket(socket);
                }
            }
            Log.d(TAG, "[" + id + "] full download complete");
        }
    }

    private boolean closeClientSocket(HttpHead head) {
        if (head == null) {
            return (true);
        }
        if (head.getHeaders() == null) {
            return (true);
        }
        boolean result = false;
        for (String name : head.getHeaders().keySet()) {
            String value = head.getHeaders().get(name);
            if ("connection".equals(name.toLowerCase())) {
                if ("close".equals(value.toLowerCase())) {
                    result = true;
                }
            }
        }
        Log.d(TAG, "[" + id + "] close client socket: " + result);
        return (result);
    }

    private long[] parseRangeRequestHeader(String range, long fileSize) {
        long start;
        long end;
        if (range != null) {
            if (range.toLowerCase().startsWith("bytes=")) {
                String s = range.substring(6);
                int i = s.indexOf("-");
                if (i == -1) {
                    return (null);
                } else if (i == 0) {
                    start = fileSize - Long.parseLong(s.substring(1));
                    end = fileSize - 1;
                    return (new long[]{start, end});
                } else if (i == s.length() - 1) {
                    start = Long.parseLong(s.substring(0, s.length() - 1));
                    end = fileSize - 1;
                    return (new long[]{start, end});
                } else {
                    start = Long.parseLong(s.substring(0, i));
                    end = Long.parseLong(s.substring(i + 1));
                    return (new long[]{start, end});
                }
            } else {
                return (null);
            }
        } else {
            return (null);
        }
    }

    private void setResponseHeader(String name, String value, OutputStream out) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(": ");
        sb.append(value);
        sb.append("\n");
        out.write(sb.toString().getBytes("UTF-8"));
        Log.d(TAG, "[" + id + "] > " + name + ": " + value);
    }

    private String getDateHeader(long milliseconds) {
        // Example: Thu, 26 Mar 2015 09:03:53 GMT
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return (simpleDateFormat.format(new Date(milliseconds)) + " GMT");
    }

    private String getDateHeader() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return (simpleDateFormat.format(new Date()) + " GMT");
    }

    private String getServerHeader() {
        return ("DroidPlay/0.2.4 (Android)");
    }

}
