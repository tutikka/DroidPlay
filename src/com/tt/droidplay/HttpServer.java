package com.tt.droidplay;

import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
			e.printStackTrace(System.err);
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
				e.printStackTrace(System.err);
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

		private int id = 0;

		@Override
		public void run() {
			try {
				Log.d(TAG, "listening for client request");
				while (true) {
					esClient.submit(new ClientWorker(serverSocket.accept(), id++));
				}
			} catch (Exception e) {
				Log.e(TAG, "error listening for client requests: " + e.getMessage());
				e.printStackTrace(System.err);
			}
		}
		
	}
	
}
