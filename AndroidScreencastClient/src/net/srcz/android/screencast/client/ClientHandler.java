package net.srcz.android.screencast.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

import android.app.Instrumentation;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.hardware.input.IInputManager;

public class ClientHandler {
	final Instrumentation i = new Instrumentation();
	
	IBinder imBinder = ServiceManager.getService("input");
	final IInputManager im = IInputManager.Stub.asInterface(imBinder);
	
	private static final int INPUT_MANAGER_INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2;
	
    Socket s;

	public ClientHandler(Socket s) throws IOException, RemoteException {
		this.s = s;
		Thread tSend = new Thread() {
			public void run() {
				sendFrameBuffer();
			}
		};
		
		Thread tHandleCmd = new Thread() {
			public void run() {
				handleCmd();
			}
		};
		tSend.start();
		tHandleCmd.start();
		
		try {
			tSend.join();
			tHandleCmd.join();
		} catch (InterruptedException e) {
		}
	}
	
	private void sendFrameBuffer() {
		try {
			Process p = Runtime.getRuntime().exec("/system/bin/cat /dev/graphics/fb0");
			InputStream is = p.getInputStream();
			System.out.println("Starting sending framebuffer");
			OutputStream os = s.getOutputStream();
			byte[] buff = new byte[336*512*2];
			while(true) {
				//FileInputStream fos = new FileInputStream("/dev/graphics/fb0");
				int nb = is.read(buff);
				if(nb < -1)
					break;
				//fos.close();
				System.out.println("val "+nb);
				os.write(buff,0,nb);
				Thread.sleep(10);
			}
			is.close();
			System.out.println("End of sending thread");
			
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void handleCmd() {
		try {
			InputStream is = s.getInputStream();
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			while(true) {
	    		String line = r.readLine();
	    		if(line == null) {
	    			r.close();
	    			s.close();
	    			break;
	    		}
	    		if(Main.debug)
	    			System.out.println("Received : "+line);
	    		try {
	    			handleCommand(line);
	    		} catch(Exception ex) {
	    			ex.printStackTrace();
	    		}
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void handleCommand(String line) throws RemoteException {
		System.out.println("handleCommand: " + line);
		
		String[] paramList = line.split("/");
		String type = paramList[0];
		if(type.equals("quit")) {
			System.exit(0);
			return;
		}
		if(type.equals("pointer")) {
			MotionEvent motionEvent = getMotionEvent(paramList);
			motionEvent.setSource(InputDevice.SOURCE_MOUSE);
			//i.sendPointerSync(getMotionEvent(paramList));
			im.injectInputEvent(motionEvent, INPUT_MANAGER_INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
			return;
		}
		if(type.equals("key")) {
			//i.sendKeySync(getKeyEvent(paramList));
			im.injectInputEvent(getKeyEvent(paramList), INPUT_MANAGER_INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
			return;
		}
		if(type.equals("trackball")) {
			MotionEvent motionEvent = getMotionEvent(paramList);
			motionEvent.setSource(InputDevice.SOURCE_TRACKBALL);
			//i.sendTrackballEventSync(getMotionEvent(paramList));
			im.injectInputEvent(motionEvent, INPUT_MANAGER_INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
			return;
		}
		
		throw new RuntimeException("Invalid type : "+type);

	}
	
    private static MotionEvent getMotionEvent(String[] args) {
    	int i = 3;
    	System.out.println("getMotionEvent: " + Arrays.asList(args));
    	
    	long downTime = SystemClock.uptimeMillis(); //Long.parseLong(args[i++]);
    	long eventTime = SystemClock.uptimeMillis(); //Long.parseLong(args[i++]);
    	int action = Integer.parseInt(args[i++]);
    	float x = Float.parseFloat(args[i++]);
    	float y = Float.parseFloat(args[i++]);
    	int metaState = Integer.parseInt(args[i++]);
        return MotionEvent.obtain(downTime, eventTime, action, x, y, metaState);
    }

    private static KeyEvent getKeyEvent(String[] args) {
    	int action = Integer.parseInt(args[1]);
    	int code = Integer.parseInt(args[2]);
    	return new KeyEvent(action, code);
    }
}
