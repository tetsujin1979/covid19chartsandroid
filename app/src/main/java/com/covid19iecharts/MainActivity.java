package com.covid19iecharts;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunctionException;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;
import com.amazonaws.regions.Regions;
import com.google.android.material.switchmaterial.SwitchMaterial;


/**
 * Main activity class for Covid 19 Charts App
 */
public class MainActivity extends AppCompatActivity {

    private Vibrator vibrator;

    private final static VibrationEffect BUTTON_CLICK_VIBRATION =
            VibrationEffect.createOneShot(250, HapticFeedbackConstants.KEYBOARD_PRESS);

    private final static VibrationEffect SUCCESS_VIBRATION =
            VibrationEffect.createOneShot(125, HapticFeedbackConstants.CONFIRM);

    private Button processCases;
    private Button processDeaths;
    private Button processHospitalisations;
    private Button processSwabs;
    private Button processVaccinations;

    private SwitchMaterial toggleButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        setContentView(R.layout.activity_main);

        processCases = findViewById(R.id.processCases);
        processCases.setOnClickListener(v -> buttonClicked("cases"));

        processDeaths = findViewById(R.id.processDeaths);
        processDeaths.setOnClickListener(v -> buttonClicked("deaths"));

        processHospitalisations = findViewById(R.id.processHospitalisations);
        processHospitalisations.setOnClickListener(v -> buttonClicked("hospitalisations"));

        processSwabs = findViewById(R.id.processSwabs);
        processSwabs.setOnClickListener(v -> buttonClicked("swabs"));

        processVaccinations = findViewById(R.id.processVaccinations);
        processVaccinations.setOnClickListener(v -> buttonClicked("vaccinations"));

        toggleButtons = findViewById(R.id.toggleButtons);
        toggleButtons.setChecked(false);
        toggleButtons.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> setButtonsEnabled(isChecked));

    }

    /**
     * Reset the buttons and the toggle to the initial state
     */
    private void buttonClicked(String metric) {

        vibrator.cancel();
        vibrator.vibrate(BUTTON_CLICK_VIBRATION);
        toggleButtons.setEnabled(false);
        invokeLambda(metric);
        setButtonsEnabled(false);

    }

    /**
     * Set the state of all buttons
     * @param enabled the state to set all the buttons
     */
    private void setButtonsEnabled(boolean enabled) {

        processCases.setEnabled(enabled);
        processDeaths.setEnabled(enabled);
        processHospitalisations.setEnabled(enabled);
        processSwabs.setEnabled(enabled);
        processVaccinations.setEnabled(enabled);

    }

    /**
     * Invoke the AWS lambda
     * @param metric    the metric to process
     */
    private void invokeLambda(String metric) {

        Context context = this.getApplicationContext();
        CognitoCachingCredentialsProvider cognitoProvider = new CognitoCachingCredentialsProvider(
                context, "identity-pool-id", <AWS Region, e.g. REGION.EU_WEST_1>);

        LambdaInvokerFactory factory = LambdaInvokerFactory.builder()
                .context(context)
                .region(<AWS Region, e.g. REGION.EU_WEST_1>)
                .credentialsProvider(cognitoProvider)
                .build();

        Covid19ChartsInterface covid19ChartsClient = factory.build(Covid19ChartsInterface.class);

        LambdaInput request = new LambdaInput();
        request.setMetric(metric);

        new AsyncTask<LambdaInput, Void, LambdaOutput>() {
            @Override
            protected LambdaOutput doInBackground(LambdaInput... params) {
                // invoke "echo" method. In case it fails, it will throw a
                // LambdaFunctionException.
                try {
                    return covid19ChartsClient.Covid19IECharts(params[0]);
                } catch (LambdaFunctionException lfe) {
                    Log.e("Tag", "Failed to invoke lambda", lfe);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(LambdaOutput lambdaOutput) {

                toggleButtons.setEnabled(true);
                toggleButtons.setChecked(false);
                vibrator.cancel();
                vibrator.vibrate(SUCCESS_VIBRATION);
                if (lambdaOutput == null) {
                    return;
                }

                // Do a toast
                Toast.makeText(MainActivity.this, String.valueOf(lambdaOutput.getResult()), Toast.LENGTH_LONG).show();
            }
        }.execute(request);
    }
}
