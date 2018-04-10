package com.ernesto.chatbot;

import android.app.Activity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.JsonElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

public class MainActivity extends Activity implements AIListener{

    private EditText editText;
    private TextView textView;
    private Button button;

    private AIService aiService;
    private AIDataService aiDataService;

    private RequestQueue mQueue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final AIConfiguration configuration = new AIConfiguration("a5a2ecbaf3bd4c0f89f631c7d0a44ea1",
                AIConfiguration.SupportedLanguages.Spanish, AIConfiguration.RecognitionEngine.System);

        aiDataService = new AIDataService(configuration);
        aiService = AIService.getService(this, configuration);
        aiService.setListener(this);

        button = (Button) findViewById(R.id.button);
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SendRequestTask(aiDataService).execute(editText.getText().toString());
            }
        });

        mQueue = VolleySingleton.getInstance(this).getRequestQueue();
    }

    public class SendRequestTask extends AsyncTask<String, String, AIResponse> {

        private AIDataService aiDataService;

        public SendRequestTask(AIDataService aiDataService) {
            this.aiDataService = aiDataService;
        }

        @Override
        protected AIResponse doInBackground(String... strings) {
            AIRequest aiRequest = new AIRequest();
            AIResponse aiResponse = null;

            try {
                aiRequest.setQuery(strings[0]);
                aiResponse = aiDataService.request(aiRequest);
            } catch (AIServiceException e) {
                e.printStackTrace();
            }

            return aiResponse;
        }

        @Override
        protected void onPostExecute(AIResponse aiResponse) {
            super.onPostExecute(aiResponse);
            Result result = aiResponse.getResult();
            textView.append("You: " + result.getResolvedQuery()+ "\r\n");

            String intentName = result.getMetadata().getIntentName();

            if(intentName.equals("Hora")) {
                Calendar now = Calendar.getInstance();
                textView.append("Yoda: " + result.getFulfillment().getSpeech() + "\r\n");
                textView.append(now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE) + "\r\n");
            } else if(intentName.equals("Clima")) {
                textView.append("Yoda: " + result.getFulfillment().getSpeech() + "\r\n");
                jsonWeather("https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo.places(1)%20where%20text%3D%22Mexico%2C%20mx%22)&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys", textView);
            }else if(intentName.equals("Donde esta")){

                final HashMap<String, JsonElement> params = result.getParameters();
                if (params != null && !params.isEmpty()) {
                    //Log.i("cOSA", "Parameters: ");
                    for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
                        //Log.i("COSA", String.format("%s: %s", entry.getKey(), entry.getValue().toString()));
                        if(entry.getKey().equals("given-name")){
                            if(entry.getValue().toString().contains("David") || entry.getValue().toString().contains("Escarcega")) {
                                textView.append("Yoda: Entrenando Padawans en Android esta" + "\r\n");
                            } else if(entry.getValue().toString().contains("Luke")) {
                                textView.append("Yoda: Cada vez mas cerca de la fuerza, el Joven Luke se encuentra" + "\r\n");
                            } else if(entry.getValue().toString().contains("Vader")) {
                                textView.append("Yoda: El lado oscuro de la fuerza, apoderado de el" + "\r\n");
                            } else {
                                textView.append("Yoda: No se donde se encuentra" + "\r\n");
                            }
                        }
                    }
                } else {
                    Log.i("Error", "Parameters esta vacio :(");
                }

            } else {
                textView.append("Yoda: " + result.getFulfillment().getSpeech() + "\r\n");
            }

        }
    }

    private void jsonWeather(String url, final TextView textV) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,
                url, null,
                // success
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONObject data = response.getJSONObject("query");
                            JSONObject results = data.getJSONObject("results");
                            JSONObject channel = results.getJSONObject("channel");

                            JSONObject atmosphere = channel.getJSONObject("atmosphere");

                            JSONObject item = channel.getJSONObject("item");
                            JSONObject condition = item.getJSONObject("condition");

                            textV.append("Humedad: "+ atmosphere.getString("humidity") + "\r\n");
                            textV.append("Presion: "+ atmosphere.getString("pressure") + "\r\n");
                            textV.append("Visibilidad: "+ atmosphere.getString("visibility") + "\r\n");
                            textV.append("Temperatura: "+ condition.getString("temp") + "\r\n");
                            textV.append("Condicion: "+ condition.getString("text") + "\r\n");
                            /*
                            JSONArray jsonArray = data.getJSONArray("results");

                            for(int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                JSONObject thumbnail = jsonObject.getJSONObject("thumbnail");

                                StringBuffer imageUrl =  new StringBuffer();
                                imageUrl.append(thumbnail.getString("path"));
                                imageUrl.append("/portrait_small");
                                imageUrl.append(".");
                                imageUrl.append(thumbnail.getString("extension"));

                                MarvelDude marveldude = new MarvelDude();
                                marveldude.name = jsonObject.getString("name");
                                marveldude.url = imageUrl.toString();
                                marveldude.id = jsonObject.getLong("id") + "";
                                marveldude.description = jsonObject.getString("description");

                                Log.i(marveldude.name,marveldude.description);

                                //adapter.add(jsonObject.getString("name"));
                                adapter.add(marveldude);
                            }
                            adapter.notifyDataSetChanged();
                            */

                        } catch (JSONException e){
                            e.printStackTrace();
                        }

                    }
                },
                // error
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        mQueue.add(request);
    }

    @Override
    public void onResult(AIResponse result) {

    }

    @Override
    public void onError(AIError error) {

    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }
}
