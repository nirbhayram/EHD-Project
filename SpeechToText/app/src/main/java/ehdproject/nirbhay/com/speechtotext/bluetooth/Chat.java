package ehdproject.nirbhay.com.speechtotext.bluetooth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;


import ehdproject.nirbhay.com.speechtotext.R;
import me.aflak.bluetooth.Bluetooth;

public class Chat extends AppCompatActivity implements Bluetooth.CommunicationCallback, TextToSpeech.OnInitListener {
    private static final int REQ_CODE_SPEECH_INPUT = 21;
    private String name;
    private Bluetooth b;
    private EditText message;
    private Button send;
    private TextView text;
    private ScrollView scrollView;
    private boolean registered = false;
    private TextToSpeech tts;
    private int i;
    private boolean fnews = false;
    private int newsCounter = 0;
    private ProgressDialog progressDialog;
    private static final String JSON_URL = "https://newsapi.org/v1/articles?source=google-news&sortBy=top&apiKey=63bbee991c6b4a8d882d032608d14cea";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
        progressDialog = new ProgressDialog(this);
        tts = new TextToSpeech(this, this);

        text = (TextView) findViewById(R.id.text);
        message = (EditText) findViewById(R.id.message);
        send = (Button) findViewById(R.id.send);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        text.setMovementMethod(new ScrollingMovementMethod());
        send.setEnabled(false);

        b = new Bluetooth(this);
        b.enableBluetooth();

        b.setCommunicationCallback(this);

        int pos = getIntent().getExtras().getInt("pos");
        name = b.getPairedDevices().get(pos).getName();

        Display("Connecting...");
        b.connectToDevice(b.getPairedDevices().get(pos));

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder str = new StringBuilder();
                str.append(",");
                str.append(message.getText().toString());
                str.append("~");
                String msg = str.toString();
                message.setText("");
                b.send(msg.substring(0,Math.min(60,msg.length())));
                Display("You: " + msg);
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        registered = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (registered) {
            unregisterReceiver(mReceiver);
            registered = false;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    public void NewsCall(){
        if(!fnews)
            loadNewsList();
        else {
            progressDialog.setMessage("Processing...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            sendMesage(News.news[newsCounter]);
            newsCounter = (newsCounter + 1) % News.news.length;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.close:
                b.removeCommunicationCallback();
                b.disconnect();
                Intent intent = new Intent(this, Select.class);
                startActivity(intent);
                finish();
                return true;
            case R.id.action_say:
                promptSpeechInput();
                return true;
            case R.id.action_speak:
                speakOut();
                return true;
            case R.id.action_news:
                NewsCall();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadNewsList() {
        //getting the progressbar

        progressDialog.setMessage("Loading");
        progressDialog.setCancelable(false);
        progressDialog.show();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, JSON_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //hiding the progressbar after completion
                        progressDialog.dismiss();
                        Log.e("Nirbhay****", response + "****");
                        try {
                            JSONObject obj = new JSONObject(response);
                            Log.e("Nirbhay1***", obj.toString());

                            JSONArray jsonArray = (JSONArray) obj.getJSONArray("articles");
                            String s[] = new String[jsonArray.length()];
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject object = (JSONObject) jsonArray.get(i);
                                StringBuilder str = new StringBuilder();
                                str.append(",");
                                str.append(object.getString("title"));
                                str.append("~");
                                s[i] = str.toString();
                                //Toast.makeText(Chat.this, s[i], Toast.LENGTH_LONG).show();
                                //message.append(s[i]);
                            }
                            News.setNews(s);
                            progressDialog.setMessage("Processing...");
                            progressDialog.setCancelable(false);
                            progressDialog.show();
                            sendMesage(News.news[newsCounter]);
                            newsCounter = (newsCounter + 1) % News.news.length;
                            fnews = true;
                        } catch (JSONException e) {
                            Log.e("Nirbhay2***", e.toString());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.setMessage("Network Error!!!");
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                            }
                        }, 100*70);
                        //Log.e("Nirbhay3***", error.getMessage().toString());
                        Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

        //creating a request queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        //adding the string request to request queue
        requestQueue.add(stringRequest);
    }

    public void sendMesage(final String toSend){
        if (toSend.length()==0) {
            message.setText(News.news[newsCounter]);
            speakOut();
            message.setText("");
            progressDialog.dismiss();
            return;
        }
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Chat.this,toSend.substring(0,Math.min(60,toSend.length())),Toast.LENGTH_LONG).show();
                b.send(toSend.substring(0,Math.min(60,toSend.length())));
                sendMesage(toSend.substring(Math.min(60,toSend.length())));
            }
        }, 100*70);
    }

    /**
     * Showing google speech input dialog
     */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    message.setText(result.get(0));
                }
                break;
            }

        }
    }

    public void Display(final String s) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.append(s + "\n");
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    public void onConnect(BluetoothDevice device) {
        Display("Connected to " + device.getName() + " - " + device.getAddress());
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                send.setEnabled(true);
            }
        });
    }

    @Override
    public void onDisconnect(BluetoothDevice device, String message) {
        Display("Disconnected!");
        Display("Connecting again...");
        b.connectToDevice(device);
    }


    @Override
    public void onMessage(String message) {
        if(message.equals("#$%")){
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    NewsCall();
                }
            });
        }
        Display(message);
    }

    @Override
    public void onError(String message) {
        Display("Error: " + message);
    }

    @Override
    public void onConnectError(final BluetoothDevice device, String message) {
        Display("Error: " + message);
        Display("Trying again in 3 sec.");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        b.connectToDevice(device);
                    }
                }, 2000);
            }
        });
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Intent intent1 = new Intent(Chat.this, Select.class);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        if (registered) {
                            unregisterReceiver(mReceiver);
                            registered = false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        if (registered) {
                            unregisterReceiver(mReceiver);
                            registered = false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                }
            }
        }
    };

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                //btnSpeak.setEnabled(true);
                speakOut();
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    private void speakOut() {

        String text = message.getText().toString();

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
}
