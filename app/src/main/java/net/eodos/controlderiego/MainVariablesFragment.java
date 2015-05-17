package net.eodos.controlderiego;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class MainVariablesFragment extends Fragment {

    private static final String DEBUG_TAG = "HttpDebug";

    public TextView tempField;
    public TextView rainField;
    public TextView humidityField;
    public TextView forecastField;
    public TextView timestampField;
    public TextView actuatorField;
    public Button buttonGet;
    public Button buttonRequest;

    // JSON Node names
    private static final String TAG_TEMP = "TEMP";
    private static final String TAG_RAIN = "RAIN";
    private static final String TAG_HUMIDITY = "HUMIDITY";
    private static final String TAG_FORECAST = "FORECAST";
    private static final String TAG_TIMESTAMP = "TIMESTAMP";
    private static final String TAG_ACTUATOR = "ACTUATOR";

    // Hashmap for ListView
    HashMap<String, String> data;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.main_variables_fragment, container, false);

        // Creamos el array de variables de lectura
        data = new HashMap<>(); // <string, string>

        //Referencia a controles de la interfaz
        tempField = (TextView) view.findViewById(R.id.temperature);
        rainField = (TextView) view.findViewById(R.id.rain);
        humidityField = (TextView) view.findViewById(R.id.humidity);
        forecastField = (TextView) view.findViewById(R.id.forecast);
        timestampField = (TextView) view.findViewById(R.id.timestamp);
        actuatorField = (TextView) view.findViewById(R.id.actuator);
        buttonGet = (Button) view.findViewById(R.id.buttonGet);
        buttonRequest = (Button) view.findViewById(R.id.buttonRequest);

        buttonGet.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                readLogVariables();
            }
        });

        buttonRequest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openSemaphore();

                // Wait 500 ms
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        readLogVariables();
                    }
                }, 500);


            }
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) { super.onSaveInstanceState(outState); }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);
        readLogVariables();
    }

    // When user clicks button, calls AsyncTask.
    // Before attempting to fetch the URL, makes sure that there is a network connection.
    public void readLogVariables() {
        String URL = "http://192.168.2.200/cgi-bin/read_log.cgi";
        if (checkConnection()) {
            Log.d(DEBUG_TAG, "Connecting to: " + URL);
            new downloadVariablesTask().execute(URL);
        }
    }

    public void openSemaphore() {
        String URL = "http://192.168.2.200/cgi-bin/open_semaphore.cgi";
        if (checkConnection()) {
            Log.d(DEBUG_TAG, "Connecting to: " + URL);
            new downloadVariablesTask().execute(URL);
        }
    }

    public boolean checkConnection() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            showWifiDialog();
            return false;
        }
    }

    // Uses AsyncTask to create a task away from the main UI thread. This task takes a
    // URL string and uses it to create an HttpUrlConnection. Once the connection
    // has been established, the AsyncTask downloads the contents of the webpage as
    // an InputStream. Finally, the InputStream is converted into a string, which is
    // displayed in the UI by the AsyncTask's onPostExecute method.
    public class downloadVariablesTask extends AsyncTask<String, Void, HashMap> {
        @Override
        protected HashMap doInBackground(String... params) {
            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadUrl(params[0],true);
            } catch (IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        showUnableToConnectDialog();
                    }
                });
                return null;
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(HashMap data) {
            if (data != null) {
                // Show toast
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        showSuccessToast();
                    }
                });

                String temperature = (String) data.get(TAG_TEMP);
                String rain = (String) data.get(TAG_RAIN);
                String humidity = (String) data.get(TAG_HUMIDITY);
                String forecast = (String) data.get(TAG_FORECAST);
                String timestamp = (String) data.get(TAG_TIMESTAMP);
                String actuator = (String) data.get(TAG_ACTUATOR);
                tempField.setText(temperature);
                rainField.setText(rain);
                humidityField.setText(humidity);
                forecastField.setText(forecast);
                actuatorField.setText(actuator);
                timestampField.setText(timestamp);
            }
        }
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string./*
    public HashMap downloadUrl(String myurl, boolean read) throws IOException {
        InputStream is = null;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(3000);
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(DEBUG_TAG, "The response is: " + response);

            is = conn.getInputStream();

            if (read) {
                // Convert the InputStream into a HashMap
                return parseJSON(is);
            }

            else {
                return null;
            }

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public HashMap parseJSON(InputStream stream) throws IOException {

        try {
            // Convert InputStream to a String
            BufferedReader r = new BufferedReader(new InputStreamReader(stream));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                result.append(line);
            }

            // Convert the String to a HashMap object
            JSONObject serverJSON = new JSONObject(result.toString());
            String temp = serverJSON.getString(TAG_TEMP) + " " + (char)186 + "C";
            String rain = serverJSON.getString(TAG_RAIN);
            if (Integer.parseInt(rain) == 0) rain = "No";
            else if (Integer.parseInt(rain) == 1) rain = "Si";
            String humidity = serverJSON.getString(TAG_HUMIDITY) + "% HR";
            String forecast = serverJSON.getString(TAG_FORECAST);
            if (Integer.parseInt(forecast) == 0) forecast = "Hoy";
            else if (Integer.parseInt(forecast) == 1) forecast = "Ma" + (char)241 + "ana";
            else if (Integer.parseInt(forecast) == 2) forecast = "Dentro de dos d" + (char)237 + "as";
            else if (Integer.parseInt(forecast) == 3) forecast = "Dentro de tres d" + (char)237 + "as";
            else if (Integer.parseInt(forecast) == -1) forecast = "No hay precipitaciones previstas en tres d" + (char)237 + "as";
            String actuator = serverJSON.getString(TAG_ACTUATOR);
            if (Integer.parseInt(actuator) == 0) actuator = "No";
            else if (Integer.parseInt(actuator) == 1) actuator = "S" + (char)237;
            String timestamp = serverJSON.getString(TAG_TIMESTAMP);

            // New temp HashMap
            HashMap<String, String> data = new HashMap<>();

            // Adding each child node to Hashmap key => value
            data.put(TAG_TEMP, temp);
            data.put(TAG_RAIN, rain);
            data.put(TAG_HUMIDITY, humidity);
            data.put(TAG_FORECAST, forecast);
            data.put(TAG_ACTUATOR, actuator);
            data.put(TAG_TIMESTAMP, timestamp);

            // Return the Hashmap
            return data;

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void showWifiDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.missingWifi))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void showUnableToConnectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.unabletoconnect))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showSuccessToast() {
        Context context = getActivity().getApplicationContext();
        CharSequence text = context.getString(R.string.success);
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}