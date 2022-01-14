package cz.pallamedia.smartauth;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import java.io.IOException;
import java.util.Random;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RequiresApi(api = Build.VERSION_CODES.M)
public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_SMS = 101;
    private static Activity activity;

    private final String SMS_API_URL = "https://pallamedia.cz/smsapi.php";
    private OkHttpClient client = new OkHttpClient();

    private static EditText field;
    private Button button;

    private String phoneNumber;
    private String authCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ContextCompat.checkSelfPermission(activity,
                Manifest.permission.READ_SMS)
                !=PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_SMS},
                    MY_PERMISSIONS_REQUEST_READ_SMS);
        }

        activity = this;

        field = (EditText) findViewById(R.id.field);
        button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePhoneNumber();
                generateAuthCode();
                sendSMS();
            }
        });
    }

    private void savePhoneNumber() {
        phoneNumber = field.getText().toString().trim();

        if (!phoneNumber.contains("+")) {
            phoneNumber = "+420" + phoneNumber;
        }
    }

    private void generateAuthCode() {
        Random random = new Random();
        authCode = String.valueOf(random.nextInt());
    }

    private void sendSMS() {
        Request request = new Request.Builder()
                .url(SMS_API_URL + "?phone=" + phoneNumber)
                .build();

        try {
            Response response = client.newCall(request).execute();
            String answer = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void fillInAuthCode(String messageBody) {
        field.setText(messageBody);
    }

    public static class SMSListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                Bundle bundle = intent.getExtras();
                SmsMessage[] message = null;
                String from;
                if (bundle != null) {
                    try {
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        message = new SmsMessage[pdus.length];
                        for (int i = 0; i < message.length; i++) {
                            message[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                            from = message[i].getOriginatingAddress();
                            String messageBody = message[i].getMessageBody();
                            fillInAuthCode(messageBody);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_SMS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                } else {
                    // permission denied
                }
                return;
            }
        }
    }
}