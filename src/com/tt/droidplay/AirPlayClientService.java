package com.tt.droidplay;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jmdns.ServiceInfo;

/**
 * Class used to communicate to the AirPlay-enabled device.
 * 
 * @author Tuomas Tikka
 */
public class AirPlayClientService {

	// executor service for asynchronous tasks to the service
	private ExecutorService es;
	
	// callback back to UI
	private AirPlayClientCallback callback;
	
	/**
	 * Initialize the service.
	 * 
	 * @param callback The callback back to the UI.
	 */
	public AirPlayClientService(AirPlayClientCallback callback) {
		this.callback = callback;
		es = Executors.newSingleThreadExecutor();
	}
	
	/**
	 * Shut down the service.
	 */
	public void shutdown() {
		if (es != null) {
			es.shutdown();
		}
	}
	
	/**
	 * Send an image file to the service (display the image).
	 * 
	 * @param file The image file
	 * @param serviceInfo The service to use
	 * @param transition The image transition to use
	 * @throws Exception If there are any problems with the parameters
	 */
	public void putImage(File file, ServiceInfo serviceInfo, String transition) throws Exception {
		if (serviceInfo == null) {
			throw new Exception("Not connected to AirPlay service");
		}
		es.submit(new PutImageTask(file, serviceInfo, transition));
	}
	
	/**
	 * Send a video link to the service (start playing).
	 * 
	 * @param location The link to the video
	 * @param serviceInfo The service to use
	 * @throws Exception If there are any problems with the parameters
	 */
	public void playVideo(URL location, ServiceInfo serviceInfo) throws Exception {
		if (serviceInfo == null) {
			throw new Exception("Not connected to AirPlay service");
		}
		es.submit(new PlayVideoTask(location, serviceInfo));
	}
	
	/**
	 * Send a request to stop playing the current video.
	 * 
	 * @param serviceInfo The service to use
	 * @throws Exception If there are any problems with the parameters
	 */
	public void stopVideo(ServiceInfo serviceInfo) throws Exception {
		if (serviceInfo == null) {
			throw new Exception("Not connected to AirPlay service");
		}
		es.submit(new StopVideoTask(serviceInfo));
	}
	
	//
	// Private
	//
	
	private class PutImageTask implements Runnable {

		private File file;
		
		private ServiceInfo serviceInfo;
		
		private String transition;
		
		public PutImageTask(File file, ServiceInfo serviceInfo, String transition) {
			this.file = file;
			this.serviceInfo = serviceInfo;
			this.transition = transition;
		}
		
		@Override
		public void run() {
			try {
				URL url = new URL(serviceInfo.getURL() + "/photo");
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setDoInput(true);
				conn.setDoOutput(true);
				conn.setConnectTimeout(15 * 1000);
				conn.setReadTimeout(15 * 1000);
				conn.setRequestMethod("PUT");
				conn.setRequestProperty("Content-Length", "" + file.length());
				conn.setRequestProperty("X-Apple-AssetKey", UUID.randomUUID().toString());
				conn.setRequestProperty("X-Apple-Session-ID", UUID.randomUUID().toString());
				conn.setRequestProperty("X-Apple-Transition", transition);
				conn.setRequestProperty("User-Agent", "MediaControl/1.0");
				BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
				byte[] buffer = new byte[32 * 1024];
				int i;
				while ((i = in.read(buffer)) != -1) {
					out.write(buffer, 0, i);
				}
				in.close();
				out.close();
				int status = conn.getResponseCode();
				if (status == 200) {
					callback.onPutImageSuccess(file);
				} else {
					callback.onPutImageError(file, "AirPlay service responded HTTP " + status);
				}
			} catch (Exception e) {
				callback.onPutImageError(file, e.getMessage());
			}
		}
		
	}
	
	private class PlayVideoTask implements Runnable {

		private URL location;
		
		private ServiceInfo serviceInfo;
		
		public PlayVideoTask(URL location, ServiceInfo serviceInfo) {
			this.location = location;
			this.serviceInfo = serviceInfo;
		}
		
		@Override
		public void run() {
			try {
				StringBuilder content = new StringBuilder();
				content.append("Content-Location: ");
				content.append(location.toString());
				content.append("\n");
				content.append("Start-Position: 0\n");
				URL url = new URL(serviceInfo.getURL() + "/play");
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setDoInput(true);
				conn.setDoOutput(true);
				conn.setConnectTimeout(15 * 1000);
				conn.setReadTimeout(15 * 1000);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Length", "" + content.length());
				conn.setRequestProperty("Content-Type", "text/parameters");
				conn.setRequestProperty("X-Apple-AssetKey", UUID.randomUUID().toString());
				conn.setRequestProperty("X-Apple-Session-ID", UUID.randomUUID().toString());
				conn.setRequestProperty("User-Agent", "MediaControl/1.0");
				BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
				out.write(content.toString().getBytes());
				out.close();
				int status = conn.getResponseCode();
				if (status == 200) {
					callback.onPlayVideoSuccess(location);
				} else {
					callback.onPlayVideoError(location, "AirPlay service responded HTTP " + status);
				}
			} catch (Exception e) {
				callback.onPlayVideoError(location, e.getMessage());
			}
		}
		
	}
	
	private class StopVideoTask implements Runnable {

		private ServiceInfo serviceInfo;
		
		public StopVideoTask(ServiceInfo serviceInfo) {
			this.serviceInfo = serviceInfo;
		}
		
		@Override
		public void run() {
			try {
				URL url = new URL(serviceInfo.getURL() + "/stop");
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(15 * 1000);
				conn.setReadTimeout(15 * 1000);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Length", "0");
				conn.setRequestProperty("User-Agent", "MediaControl/1.0");
				int status = conn.getResponseCode();
				if (status == 200) {
					callback.onStopVideoSuccess();
				} else {
					callback.onStopVideoError("AirPlay service responded HTTP " + status);
				}
			} catch (Exception e) {
				callback.onStopVideoError(e.getMessage());
			}
		}
		
	}
	
}
