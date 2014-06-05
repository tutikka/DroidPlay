package com.tt.droidplay;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Base64;
import android.util.Log;

/**
 * Class that implements a minimal HTTP server, used to stream videos from the device.
 * 
 * @author Tuomas Tikka
 */
public class HttpServer {

	private static final String TAG = "HttpServer";
	
	// the server socket
	private ServerSocket serverSocket;
	
	// the thread pool for the server
	private ExecutorService esServer = Executors.newSingleThreadExecutor();
	
	// the thread pool for the client request handlers
	private ExecutorService esClient = Executors.newFixedThreadPool(10);
	
	/**
	 * Initialize the server.
	 */
	public HttpServer() {
	}
	
	/**
	 * Start the server.
	 * 
	 * @param port The port number to listen on.
	 */
	public void startServer(int port) {
		try {
			serverSocket = new ServerSocket(port);
			Log.d(TAG, "started http server on port " + port);
		} catch (Exception e) {
			Log.e(TAG, "error starting http server: " + e.getMessage());
			return;
		}
		esServer.submit(new ServerWorker(serverSocket));
	}
	
	/**
	 * Stop the server.
	 */
	public void stopServer() {
		if (serverSocket != null) {
			try {
				serverSocket.close();
				serverSocket = null;
				Log.d(TAG, "stopped http server");
			} catch (Exception e) {
				Log.e(TAG, "error stopping http server: " + e.getMessage());
			}
		}
	}
	
	//
	// Private
	//
	
	private class ServerWorker implements Runnable {

		private ServerSocket serverSocket;
		
		public ServerWorker(ServerSocket serverSocket) {
			this.serverSocket = serverSocket;
		}
		
		@Override
		public void run() {
			try {
				Log.d(TAG, "listening for client request");
				while (true) {
					esClient.submit(new ClientWorker(serverSocket.accept()));
				}
			} catch (Exception e) {
				Log.e(TAG, "error listening for client requests: " + e.getMessage());
			}
		}
		
	}
	
	private class ClientWorker implements Runnable {
		
		private Socket socket;
		
		public ClientWorker(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			Log.d(TAG, "client connection from " + socket.getInetAddress().getHostAddress());
			try {
				HttpHead head = HttpHead.parse(socket.getInputStream());
				if (head == null) {
					handle400(socket);
					return;
				}
				handleDownload(socket, head);
			} catch (Exception e) {
				Log.e(TAG, "error reading client request: " + e.getMessage());
			}
		}

	}
	
	private void handle400(Socket socket) throws Exception {
		PrintWriter pw = new PrintWriter(socket.getOutputStream());
		pw.println("HTTP/1.1 400 Bad Request");
		pw.println();
		pw.close();
		socket.close();
	}
	
	private void handle403(Socket socket) throws Exception {
		PrintWriter pw = new PrintWriter(socket.getOutputStream());
		pw.println("HTTP/1.1 403 Forbidden");
		pw.println();
		pw.close();
		socket.close();
	}
	
	private void handle404(Socket socket) throws Exception {
		PrintWriter pw = new PrintWriter(socket.getOutputStream());
		pw.println("HTTP/1.1 404 Not Found");
		pw.println();
		pw.close();
		socket.close();
	}
	
	private void handleDownload(Socket socket, HttpHead head) throws Exception {
		String path = new String(Base64.decode(head.getUri(), Base64.NO_WRAP|Base64.URL_SAFE), "UTF-8");
        File file = new File(path);
        if (!file.exists()) {
        	Log.w(TAG, "file not found: " + path);
        	handle404(socket);
            return;
        }        
        if (!file.canRead()) {
        	Log.w(TAG, "cannot read file: " + path);
        	handle403(socket);
            return;
        }
        boolean isRange = false;
        long start = -1;
        long end = -1;
        String range = head.getHeaders().get("Range");
        if (range != null) {
            if (range.toLowerCase().startsWith("bytes=")) {
                StringTokenizer stringTokenizer = new StringTokenizer(range.substring(6), "-");
                if (stringTokenizer.countTokens() == 1) {
                    start = Long.parseLong(stringTokenizer.nextToken());
                    end = file.length() - 1;
                    isRange = true;
                    Log.d(TAG, "range request: " + start + "-" + end);
                } else if (stringTokenizer.countTokens() == 2) {
                    start = Long.parseLong(stringTokenizer.nextToken());
                    end = Long.parseLong(stringTokenizer.nextToken());
                    isRange = true;
                    Log.d(TAG, "range request: " + start + "-" + end);
                } else {
                    isRange = false;
                    Log.d(TAG, "invalid range: " + range);
                }
            } else {
            	Log.w(TAG, "invalid range: " + range);
            }
        }
        if (isRange) {
        	Log.d(TAG, "detected range download");
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
            out.write("HTTP/1.1 206 PARTIAL CONTENT\n".getBytes());
            out.write(("Content-Length: " + (end - start + 1) + "\n").getBytes());
            out.write(("Content-Range: bytes " + start + "-" + end + "/" + file.length() + "\n").getBytes());
            out.write(("Content-Disposition: attachment; filename=\"" + file.getName() + "\"\n").getBytes());
            out.write("Cache-Control: private, max-age=0\n".getBytes());
            out.write("Accept-Ranges: bytes\n".getBytes());
            out.write("Content-Type: video/mp4\n".getBytes());
            out.write("\n".getBytes());
            byte[] buffer = new byte[32768];
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(start);
            int r;
            long t = end - start + 1;
            while ((r = raf.read(buffer)) > 0) {
                if ((t -= r) > 0) {
                    out.write(buffer, 0, r);
                } else {
                    out.write(buffer, 0, (int) t + r);
                    break;
                }
            }
            raf.close();
            out.flush();
            out.close();
            Log.d(TAG, "range download complete");
        } else {
        	Log.d(TAG, "detected full download");
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
            out.write("HTTP/1.1 200 OK\n".getBytes());
            out.write(("Content-Length: " + file.length() + "\n").getBytes());
            out.write(("Content-Disposition: attachment; filename=\"" + file.getName() + "\"\n").getBytes());
            out.write("Cache-Control: private, max-age=0\n".getBytes());
            out.write("Content-Type: video/mp4\n".getBytes());
            out.write("\n".getBytes());
            final byte[] buffer = new byte[32768];
            final BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            int r;
            while ((r = in.read(buffer)) > 0) {
            	out.write(buffer, 0, r);
            }
            out.flush();
            out.close();
            in.close();
            Log.d(TAG, "full download complete");
        }
	}
	
}
