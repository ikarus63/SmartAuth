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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.IOException;
import java.util.Random;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RequiresApi(api = Build.VERSION_CODES.M)
public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_SMS = 101;
    private static Activity activity;

    private final String SMS_API_URL = "https://pallamedia.cz/smsapi.php?auth";
    private OkHttpClient client = new OkHttpClient();

    private static EditText field;
    private static Button button;
    private static TextView textView;

    private String phoneNumber;
    private static String authCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context mContext = this;

        if(ContextCompat.checkSelfPermission((Activity)mContext,Manifest.permission.READ_SMS)!=PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.READ_CALENDAR},
                    MY_PERMISSIONS_REQUEST_READ_SMS);
        }

        activity = this;

        field = (EditText) findViewById(R.id.field);
        button = (Button) findViewById(R.id.button);
        textView = (TextView) findViewById(R.id.textView);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePhoneNumber();
                setButtonVisible(false);
                changeText("Odesílám SMS s vygenerovaným kódem...", true);
                generateAuthCode();
                sendSMS();
            }
        });
    }

    private static void changeText(String txt, boolean empty){
        textView.setText(txt);
        if(empty)
            field.setText("");
    }

    private static void setButtonText(String txt){
        button.setText(txt);
    }

    private static void setButtonVisible(boolean a){
        if(a){
            button.setVisibility(View.VISIBLE);
        }else{
            button.setVisibility(View.INVISIBLE);
        }
    }

    private static void setFieldActivated(boolean a){
        field.setActivated(a);
    }

    private void savePhoneNumber() {
        phoneNumber = field.getText().toString().trim();

        if (!phoneNumber.contains("+")) {
            phoneNumber = "+420" + phoneNumber;
        }
    }

    private void generateAuthCode() {
        Random random = new Random();
        authCode = String.valueOf(random.nextInt() + Integer.MAX_VALUE).replace("-", "");
    }

    private void sendSMS() {
        new SMSHttpRequest().execute();
    }

    class SMSHttpRequest extends AsyncTask<String, Void, String> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... urls) {

            Request request = new Request.Builder()
                    .url(SMS_API_URL + "&phone=" + phoneNumber + "&code=" + authCode)
                    .build();

            try {
                Response response = client.newCall(request).execute();
                return response.body().string();
            } catch (IOException e) {
                return e.toString();
            }

        }

        protected void onPostExecute(String result) {
            changeText("Zadejte kód z SMS:", true);
            setButtonVisible(true);
            setButtonText("Potvrdit");
            setButtonListener();
        }
    }

    private void setButtonListener() {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAuthCode();
            }
        });
    }

    private static void fillInAuthCode(String messageBody) {
            field.setText(messageBody);
            checkAuthCode();
    }

    public static void checkAuthCode(){
        if(field.getText().toString().trim().equalsIgnoreCase(authCode)){
            changeText("Autorizační kód úspěšně ověřen!", false);
            setButtonVisible(false);
            setFieldActivated(false);
        }else{
            changeText("Chybný autorizační kód!", false);
        }
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