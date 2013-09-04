package upc.edu.btstresstest.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import upc.edu.btstresstest.app.BTApp;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BTManager {

	private BluetoothAdapter mBluetoothAdapter = null;
	private Activity act = null;
	private BTApp app = null;
	private BluetoothServerSocket mmServerSocket;
	// private Handler connectionListener = null;
	private Thread connectionListener = null;
	private Timer timer = null;
	private Runnable r = null;
	public boolean paired = false;
	private Handler p1 = null;
	private Handler p2 = null;
	private BluetoothSocket socket = null;

	public final static int REQUEST_ENABLE_BT = 1;

	public BTManager(Activity act, Handler p1, Handler p2) {

		this.act = act;
		this.p1 = p1;
		this.p2 = p2;
		app = (BTApp) act.getApplication();
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		Log.d(app.TAG, "Bluetooth adappter is " + mBluetoothAdapter);
		if (mBluetoothAdapter != null) {
			activateBT();
		} else {
			Log.d(app.TAG, "Configure BT is not called");
		}
	}

	public boolean isBtEnabled() {
		return mBluetoothAdapter.isEnabled();
	}

	public boolean activateBT() {
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			act.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			return true;
		}
		return false;
	}

	public boolean configureBTServerSocket() {
		// Use a temporary object that is later assigned to mmServerSocket,
		// because mmServerSocket is final
		/*
		 * BluetoothServerSocket tmp = null; try { // MY_UUID is the app's UUID
		 * string, also used by the client code UUID id =
		 * UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); tmp =
		 * mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
		 * "DummyZephyr", id); } catch (IOException e) { } mmServerSocket = tmp;
		 */

		// Class<BluetoothAdapter> blue = BluetoothAdapter.class;
		// try {
		// Method m = blue.getDeclaredMethod("listenUsingInsecureRfcommOn",
		// int.class);
		// mmServerSocket = (BluetoothServerSocket) m.invoke(
		// mBluetoothAdapter, 1);

		if (isBtEnabled()) {
			try {
				UUID uuid = UUID
						.fromString("00000000-deca-fade-deca-deafdecacaff");
				mmServerSocket = mBluetoothAdapter
						.listenUsingInsecureRfcommWithServiceRecord(
								"DummyZephyr", uuid);

				Log.d(app.TAG, "Socket listening in port");

			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				stopBTStress();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				stopBTStress();
				return false;
			}
		}
		return true;

		// } catch (NoSuchMethodException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (IllegalArgumentException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (IllegalAccessException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (InvocationTargetException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	private void setPairedInfo() {

		paired = true;

		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
				.getBondedDevices();
		if (pairedDevices.size() > 0) {
			BluetoothDevice device = (BluetoothDevice) pairedDevices.toArray()[0];

			// handler p1
			Message m = new Message();
			Bundle b = new Bundle();
			b.putString("name", device.getName());
			b.putString("address", device.getAddress());
			m.setData(b);
			p1.sendMessage(m);
		}
	}

	public synchronized void startBTStress() {

		if (isBtEnabled()) {
			configureBTServerSocket();

			r = new Runnable() {

				@Override
				public void run() {
					// Keep listening until exception occurs or a socket is
					// returned

					try {
						Log.d(app.TAG, "StartBTStress serverSocket is "
								+ mmServerSocket);

						if (mmServerSocket == null) {
							configureBTServerSocket();
						}

						if (mmServerSocket != null)
							socket = mmServerSocket.accept();
						// If a connection was accepted
						if (socket != null) {
							// Do work to manage the connection (in a
							// separate thread)
							setPairedInfo();
							sendMessages(socket);
							// mmServerSocket.close();
						}
					} catch (IOException e) {
						stopBTStress();
						e.printStackTrace();
					}
				}
			};

			// connectionListener.post(r);

			connectionListener = new Thread(r);
			connectionListener.start();
		}
	}

	private void sendMessages(BluetoothSocket socket) throws IOException {

		Log.d(app.TAG, "sendMessages");
		// connectionListener.removeCallbacks(r);
		// connectionListener = null;

		final OutputStream bOs = socket.getOutputStream();

		// Prepare timerTask, send 4 messages every 500ms
		TimerTask send = new TimerTask() {

			@Override
			public void run() {

				int[] in = new int[] { 2, 43, 71, 118, 218, 7, 10, 23, 147,
						247, 230, 2, 2, 0, 0, 0, 0, 0, 128, 151, 0, 1, 0, 3, 0,
						194, 14, 29, 0, 0, 255, 255, 255, 32, 3, 132, 13, 0,
						255, 255, 0, 255, 255, 0, 0, 86, 0, 93, 0, 46, 0, 54,
						0, 244, 255, 252, 255, 249, 0, 19, 2, 254, 0, 12, 100,
						1, 166, 1, 171, 1, 8, 2, 0, 0, 54, 3 };
				
				int hr = randomHR();
				int br = randomBR();
				
				in[13] = hr;
				in[14] = hr << 8;
				
				in[28] = br;
				in[29] = br << 8;

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				for (int i : in) {
					try {
						dos.write(i);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				try {
					dos.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				final byte[] tosend = baos.toByteArray();

				try {
					bOs.write(tosend);
					app.messageCount++;
				} catch (Exception e) {
					e.printStackTrace();
					stopBTStress();
				}

				// handler p2
				Message m = new Message();
				p2.sendMessage(m);
			}
		};

		timer = new Timer();
		timer.schedule(send, 0, 1000);
	}

	public void stopBTStress() {

		if (timer != null)
			timer = null;

		try {
			if (mmServerSocket != null)
				mmServerSocket.close();

			mmServerSocket = null;

			if (socket != null)
				socket.close();
			socket = null;

		} catch (IOException e) {
			e.printStackTrace();
		}

		r = null;

		connectionListener = null;
		app.messageCount = 0;

		paired = false;
	}

	int[] arrayhr = new int[] { 82, 84, 86, 90, 94, 92, 90, 92, 95, 90, 89, 85,
			80, 77, 74, 69, 70, 74, 80, 72, 82, 80, 85, 87, 80, 85, 87, 85, 82,
			95, 100, 110, 105, 100, 95, 90, 85, 84 };
	int[] arraybr = new int[] { 200, 220, 230, 250, 260, 250, 240, 230, 220,
			200, 210, 215, 225, 230, 240, 250, 255, 260, 255, 250, 245, 230,
			225, 220, 230, 240, 250, 260, 265, 260, 255, 250, 245, 240, 230,
			220, 210, 206, 205, 202 };

	private Random rnd = new Random();

	private int randomHR() {
		return arrayhr[rnd.nextInt(arrayhr.length)];
	}

	private int randomBR() {
		return arraybr[rnd.nextInt(arraybr.length)];
	}
}
