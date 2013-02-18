package upc.edu.btstresstest.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
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
		Log.d("bluetest", "Bluetooth adappter is " + mBluetoothAdapter);
		if (mBluetoothAdapter != null) {
			configureBT();
		} else {
			Log.d("bluetest", "Configure BT is not called");
		}
	}

	public boolean isBtEnabled() {
		return mBluetoothAdapter.isEnabled();
	}

	public boolean configureBT() {
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			act.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

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

		try {
			UUID uuid = UUID.fromString("00000000-deca-fade-deca-deafdecacaff");
			mmServerSocket = mBluetoothAdapter
					.listenUsingInsecureRfcommWithServiceRecord("DummyZephyr",
							uuid);

			Log.d("bluetest", "Socket listening in port");

		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			stopBTStress();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			stopBTStress();
			return false;
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

	public void startBTStress() {

		if (!isBtEnabled())
			configureBT();

		// connectionListener = new Handler();

		r = new Runnable() {

			@Override
			public void run() {
				// Keep listening until exception occurs or a socket is returned

				try {
					Log.d("bluetest", "StartBTStress serverSocket is "
							+ mmServerSocket);

					if (mmServerSocket == null) {
						configureBT();
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

	private void sendMessages(BluetoothSocket socket) throws IOException {

		Log.d("bluetest", "sendMessages");
		// connectionListener.removeCallbacks(r);
		// connectionListener = null;

		final OutputStream bOs = socket.getOutputStream();

		// Prepare timerTask, send 4 messages every 500ms
		TimerTask send = new TimerTask() {
			byte[] b = new byte[1000];

			@Override
			public void run() {
				try {
					new Random().nextBytes(b);
					Log.d("bluetest", "Before write");
					bOs.write(b);
					bOs.flush();
					Log.d("bluetest", "After write");
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
}
