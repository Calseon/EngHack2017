package enghack17.myowninstrument;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final String TAG = "MainActivity";


    // Frontend UI
    private TextView mTextView;
    private TextView accelXData;
    private TextView accelYData;
    private TextView accelZData;
    private LineGraphView graph;
    private PulseDetect detectX;
    private PulseDetect detectY;
    private PulseDetect detectZ;

    private Spinner spinner;
    Hub hub;

    // Device Listener for the Myo -----------------------------------------------------------
    private DeviceListener mListener = new AbstractDeviceListener() {

        // Accelerometer data
        double oldAccelX = 0;
        double oldAccelY = 0;
        double oldAccelZ = 0;

        DataFilter dataFilter = new DataFilter();

        // onConnect() is called whenever a Myo has been connected.
        @Override
        public void onConnect(Myo myo, long timestamp) {
            // Set the text color of the text view to cyan when a Myo connects.
            mTextView.setTextColor(Color.CYAN);
            myo.unlock(Myo.UnlockType.HOLD);
        }
        // onDisconnect() is called whenever a Myo has been disconnected.
        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            // Set the text color of the text view to red when a Myo disconnects.
            mTextView.setTextColor(Color.RED);
        }
        // onArmSync() is called whenever Myo has recognized a Sync Gesture after someone has put it on their
        // arm. This lets Myo know which arm it's on and which way it's facing.
        @Override
        public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
            mTextView.setText(myo.getArm() == Arm.LEFT ? R.string.arm_left : R.string.arm_right);
        }
        // onArmUnsync() is called whenever Myo has detected that it was moved from a stable position on a person's arm after
        // it recognized the arm. Typically this happens when someone takes Myo off of their arm, but it can also happen
        // when Myo is moved around on the arm.
        @Override
        public void onArmUnsync(Myo myo, long timestamp) {
            mTextView.setText(R.string.hello_world);
        }
        // onUnlock() is called whenever a synced Myo has been unlocked. Under the standard locking
        // policy, that means poses will now be delivered to the listener.
        @Override
        public void onUnlock(Myo myo, long timestamp) {

        }
        // onLock() is called whenever a synced Myo has been locked. Under the standard locking
        // policy, that means poses will no longer be delivered to the listener.
        @Override
        public void onLock(Myo myo, long timestamp) {

        }
        // onOrientationData() is called whenever a Myo provides its current orientation,
        // represented as a quaternion.
        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            // Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
            float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
            float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
            float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));
            // Adjust roll and pitch for the orientation of the Myo on the arm.
            if (myo.getXDirection() == XDirection.TOWARD_ELBOW) {
                roll *= -1;
                pitch *= -1;
            }
            // Next, we apply a rotation to the text view using the roll, pitch, and yaw.
            mTextView.setRotation(roll);
            mTextView.setRotationX(pitch);
            mTextView.setRotationY(yaw);
        }
        // onPose() is called whenever a Myo provides a new pose.
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            // Handle the cases of the Pose enumeration, and change the text of the text view
            // based on the pose we receive.
            switch (pose) {
                case UNKNOWN:
                    mTextView.setText(getString(R.string.hello_world));
                    break;
                case REST:
                case DOUBLE_TAP:
                    int restTextId = R.string.hello_world;
                    switch (myo.getArm()) {
                        case LEFT:
                            restTextId = R.string.arm_left;
                            break;
                        case RIGHT:
                            restTextId = R.string.arm_right;
                            break;
                    }
                    mTextView.setText(getString(restTextId));
                    break;
                case FIST:
                    mTextView.setText(getString(R.string.pose_fist));
                    break;
                case WAVE_IN:
                    mTextView.setText(getString(R.string.pose_wavein));
                    break;
                case WAVE_OUT:
                    mTextView.setText(getString(R.string.pose_waveout));
                    break;
                case FINGERS_SPREAD:
                    mTextView.setText(getString(R.string.pose_fingersspread));
                    break;
            }

            // Stay permanently unlocked
            myo.unlock(Myo.UnlockType.HOLD);
        }

        @Override
        public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel)
        {
            double smoothedX = dataFilter.lowPass(oldAccelX,accel.x());
            double smoothedY = dataFilter.lowPass(oldAccelX,accel.y());
            double smoothedZ = dataFilter.lowPass(oldAccelX,accel.z());

            accelXData.setText("X: " + Double.toString(smoothedX));
            accelYData.setText("Y: " + Double.toString(smoothedY));
            accelZData.setText("Z: " + Double.toString(smoothedZ));
            float[] accelData = {(float)smoothedX, (float)smoothedY, (float)smoothedZ};
            graph.addPoint(accelData);

            detectX.AddNewPoint((smoothedX));
            detectY.AddNewPoint((smoothedY));
            detectZ.AddNewPoint((smoothedZ));

            if (spinner.getSelectedItemPosition() == 0) {
                if(detectX.pulseDetected())
                    PlayInstrument();
            } else if (spinner.getSelectedItemPosition() == 1) {
                if (detectX.pulseDetected() || detectY.pulseDetected())
                    PlayInstrument();
            }
            mTextView.setText("State: " + detectX.programState);
        }
    };
    // ----------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Initialize UI variables
        FrameLayout mainlayout = (FrameLayout)findViewById(R.id.layoutGraph);

        spinner = (Spinner) findViewById(R.id.spinnerInstruments);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
        R.array.instrument_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        mTextView = (TextView) findViewById(R.id.textViewMessage);
        accelXData = (TextView) findViewById(R.id.textViewAccelX);
        accelYData = (TextView) findViewById(R.id.textViewAccelY);
        accelZData = (TextView) findViewById(R.id.textViewAccelZ);
        graph = new LineGraphView(getApplicationContext(),
                400,
                Arrays.asList("X", "Y", "Z"));
        mainlayout.addView(graph);
        graph.setVisibility(View.VISIBLE);
        AcquirePerimissions();

        // Initialize Myo
        hub = Hub.getInstance();
        if (!hub.init(this)) {
            Log.e(TAG, "Could not initialize the Hub.");
            finish();
            return;
        }
        Hub.getInstance().setLockingPolicy(Hub.LockingPolicy.NONE);
        Hub.getInstance().setSendUsageData(false);

        // Next, register for DeviceListener callbacks.
        hub.addListener(mListener);

        detectX = new PulseDetect();
        detectY = new PulseDetect();
        detectZ = new PulseDetect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void ConnectToMyo(View v)
    {
        Intent intent = new Intent(this, ScanActivity.class);
        this.startActivity(intent);
    }

    private void AcquirePerimissions()
    {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener(){
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    public void PlayInstrument() {
        if (spinner.getSelectedItemPosition() == 0) {
            PlaySoundFile(R.raw.drum_sound);
        } else if (spinner.getSelectedItemPosition() == 1) {
            PlaySoundFile(R.raw.maracasound);
        }
    }

    public void PlayDrum(View v)
    {
        PlaySoundFile(R.raw.drum_sound);
    }

    public void PlayMaraca(View v)
    {
        PlaySoundFile(R.raw.maracasound);
    }


    private void PlaySoundFile(int R_SoundAssetID) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        AssetFileDescriptor afd = this.getResources().openRawResourceFd(R_SoundAssetID);

        try {
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
