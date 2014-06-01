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
 * Class used to communicate to service.
 * 
 * @author Tuomas Tikka
 */
public class BeamService {

	// executor service for asynchronous tasks to the service
	private ExecutorService es;
	
	// callback back to UI
	private BeamCallback callback;
	
	/**
	 * Initialize the service.
	 * 
	 * @param callback The callback back to the UI.
	 */
	public BeamService(BeamCallback callback) {
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
	 * Beam a photo to the service asynchronously.
	 * 
	 * @param file The image file
	 * @param serviceInfo The service to use
	 * @throws Exception If there are any problems with the parameters
	 */
	public void beamPhoto(File file, ServiceInfo serviceInfo) throws Exception {
		if (serviceInfo == null) {
			throw new Exception("Not connected to AirPlay service");
		}
		es.submit(new BeamPhotoTask(file, serviceInfo));
	}
	
	//
	// Private
	//
	
	private class BeamPhotoTask implements Runnable {

		private File file;
		
		private ServiceInfo serviceInfo;
		
		public BeamPhotoTask(File file, ServiceInfo serviceInfo) {
			this.file = file;
			this.serviceInfo = serviceInfo;
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
					callback.onPhotoBeamSuccess(file);
				} else {
					callback.onPhotoBeamError(file, "AirPlay service responded HTTP " + status);
				}
			} catch (Exception e) {
				callback.onPhotoBeamError(file, e.getMessage());
			}
		}
		
	}
	
}
