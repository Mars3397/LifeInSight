package de.yanneckreiss.mlkittutorial;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import de.yanneckreiss.cameraxtutorial.R;

public class ChatRoom extends AppCompatActivity {


    private static final int RQ_SPEECH_REC = 102;
    RecyclerView recyclerView;
    EditText message_text_text;
    ImageView send_btn;
    List<Message> messageList = new ArrayList<>();
    MessageAdapter messageAdapter;
    Button REC_btn;
    Button BigButton;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    private TextToSpeech tts;

    private String recognizedText;

    StringBuilder context = new StringBuilder();

    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        getSupportActionBar().hide();

        String detectText = getIntent().getStringExtra("detectText");
        callAPI("請幫我排版並潤飾文字，要潤飾文字拜託，寫出潤飾後的文字就好不要念出問題，用繁體中文：\n" + detectText);

        context.append(detectText);
        context.append("\n");

//        messageList.add(new Message(detectText.toString(), Message.SEND_BY_BOT));




//        record_btn = findViewById(R.id.record_button);

        //====================================
        message_text_text = findViewById(R.id.message_text_text);
        send_btn = findViewById(R.id.send_btn);
        recyclerView = findViewById(R.id.recyclerView);

//        REC_btn = findViewById(R.id.button);

        // Create Layout behaves and set it in recyclerView
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        //====================================

        //====================================
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        //====================================

        if(!isConnected(ChatRoom.this)) {
            buildDialog(ChatRoom.this).show();
        }

        REC_btn = findViewById(R.id.button);
        REC_btn.setOnClickListener(view -> askSpeechInput());

        BigButton = findViewById(R.id.buttonBig);
        BigButton.setOnClickListener(view -> {
            if (BigButton.getText() == "大字") {
                messageAdapter.textSize = 40;
                BigButton.setText("小字");
                BigButton.setContentDescription("字體縮小");
            } else {
                messageAdapter.textSize = 22;
                BigButton.setText("大字");
                BigButton.setContentDescription("字體放大");
            }
            messageAdapter.notifyDataSetChanged();
        });

        message_text_text.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(s.toString().trim().length()==0){
                    send_btn.setEnabled(false);
                    // Toast.makeText(MainActivity.this, "Type your message", Toast.LENGTH_SHORT).show();
                } else {
                    send_btn.setEnabled(true);
                    send_btn.setOnClickListener(view -> {
                        String question = message_text_text.getText().toString().trim();
                        addToChat(question,Message.SEND_BY_ME);
                        message_text_text.setText("");

                        context.append("我說：" + question);
                        context.append("\n");
                        callAPI(context.toString() + "根據上面的對話，做最後面那件事情，不要再複誦一次問題，用繁體中文\n");
                    });
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

            }
        });



    } // OnCreate Method End Here ================

    private void askSpeechInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition is not available", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");
            startActivityForResult(intent, RQ_SPEECH_REC);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RQ_SPEECH_REC && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                recognizedText = results.get(0);

                String question = recognizedText;
                addToChat(question,Message.SEND_BY_ME);
                context.append("我說：" + question);
                context.append("\n");
                callAPI(context.toString() + "根據上面的對話，做最後面那件事情，不要再複誦一次問題，用繁體中文\n");

            }
        }
    }


    void addToChat (String message, String sendBy){
        runOnUiThread(() -> {
            messageList.add(new Message(message, sendBy));
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
        });
    } // addToChat End Here =====================

    void addResponse(String response){
        messageList.remove(messageList.size()-1);
        addToChat(response, Message.SEND_BY_BOT);
        context.append("你說：\n" + response);
        context.append("\n");
    } // addResponse End Here =======

    void callAPI(String question){
        // okhttp
        messageList.add(new Message("正在為您努力處理中 請稍候 ...", Message.SEND_BY_BOT));

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "gpt-3.5-turbo");
            JSONArray messagesArray = new JSONArray();
            JSONObject messageObject = new JSONObject();
            messageObject.put("role", "user");
            messageObject.put("content", question);
            messagesArray.put(messageObject);
            jsonBody.put("messages", messagesArray);
//            jsonBody.put("stream", true);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody requestBody = RequestBody.create(jsonBody.toString(),JSON);
        Request request = new Request.Builder()
                .url(API.API_URL)
                .header("Authorization","Bearer "+API.API)
                .post(requestBody)
                .build();


//        OkHttpClient okHttpClient = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
//                    @NonNull
//                    @Override
//                    public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
//                        Request original = chain.request();
//                        Request request = original.newBuilder()
//                                .url(API.API_URL)
//                                .header("Authorization","Bearer "+API.API)
//                                .post(requestBody)
//                                .build();
//                        return chain.proceed(request);
//                    }
//                })
//                .connectTimeout(30, TimeUnit.MINUTES)
//                .readTimeout(30, TimeUnit.MINUTES)
//                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to"+e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()){
                    JSONObject jsonObject;
                    try {
                        assert response.body() != null;
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content");

//                        String responseBody = response.body().string();
//                        Log.d("Response", responseBody);

//                        jsonObject = new JSONObject(response.body().string());
//                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
//                        String result = jsonArray.getJSONObject(0).getJSONObject("delta").getString("content");
                        addResponse(result.trim());



                        tts = new TextToSpeech(getApplicationContext(), status -> {
                            if (status == TextToSpeech.SUCCESS) {
                                Locale chineseLocale = new Locale("zh", "CN");
                                tts.setLanguage(chineseLocale);
                                tts.setSpeechRate(2.0f);
                                tts.speak(result.trim(), TextToSpeech.QUEUE_ADD, null, null);
                            }
                        });



                        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        audioFocusChangeListener = focusChange -> {
                            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                                if (tts != null) {
                                    tts.stop();
                                }
                            }
                        };

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    assert response.body() != null;
                    addResponse("Failed to load response due to"+ response.body());
                }

            }
        });

    } // callAPI End Here =============

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (tts != null) {
                tts.stop();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean isConnected(Context context){
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info= manager.getActiveNetworkInfo();
        if(info!= null && info.isConnectedOrConnecting()){
            android.net.NetworkInfo wifi= manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            android.net.NetworkInfo mobile= manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            return (mobile != null && mobile.isConnectedOrConnecting()) || (wifi != null && wifi.isConnectedOrConnecting());
        } else
            return false;
    }

    public AlertDialog.Builder buildDialog(Context context){
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("No Internet Connection");
        builder.setMessage("Please check your internet connection.");
        builder.setPositiveButton("OK", (dialog, which) -> finishAffinity());
        return builder;
    }
} // Public Class End Here =========================