package upc.edu.btstresstest.activity;

import upc.edu.btstresstest.R;
import upc.edu.btstresstest.app.BTApp;
import upc.edu.btstresstest.util.BTManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

	private BTApp app = null;
	private BTManager btManager = null;
	public final static int REQUEST_ENABLE_BT = 1;
	
	private Handler printer1 = null;
	private Handler printer2 = null;
	
	private TextView name = null;
	private TextView addrs = null;
	private TextView packets = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		app = (BTApp) getApplication();
		configureText();
		configureButtonListener();
		configureHandlers();
		
		btManager = new BTManager(this, printer1, printer2);
	}

	private void configureText() {
		
		name = (TextView) findViewById(R.id.textView_name_input);
		name.setText("Not listening for input connections");
		addrs = (TextView) findViewById(R.id.textView_address_input);
		packets = (TextView) findViewById(R.id.textView_packets_input);
	}

	private void configureHandlers() {
		this.printer1 = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				String nameString = msg.getData().getString("name");
				String addrsString = msg.getData().getString("address");

				name.setText(nameString);
				addrs.setText(addrsString);
			}
		};
		
		this.printer2 = new Handler() {			
			@Override
			public void handleMessage(Message msg) {
				packets.setText(app.messageCount+"");
			}
		};
	}

	private void configureButtonListener() {
		final ToggleButton toggleButton = (ToggleButton)findViewById(R.id.toggleButton1);
		
		toggleButton.setOnClickListener(new OnClickListener() {

	        @Override
	        public void onClick(View arg0) {
	            if(toggleButton.isChecked()){
	            	btManager.startBTStress();
	            	packets.setText("0");
	        		name.setText("---");
	        		addrs.setText("---");
	            }
	            else{
	            	btManager.stopBTStress();
	            	name.setText("Not listening for input connections");
	            }
	        }
	    });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
				if(resultCode == RESULT_OK){
					Log.d(app.TAG, "Bluetooth activated");
				}
				else{
					Log.d(app.TAG, "Bluetooth activation rejected, closing...");
					finish();
				}
					
			break;
		default:				
				//btManager.paired = false;
			break;
		}
	}
}
