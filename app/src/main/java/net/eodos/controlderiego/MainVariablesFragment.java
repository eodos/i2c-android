package net.eodos.controlderiego;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
    public TextView countField;
    public TextView tempField;
    public EditText xField;
    public Button buttonRead;
    public Button buttonX;

    // JSON Node names
    private static final String TAG_COUNT = "COUNT";
    private static final String TAG_TEMP = "TEMP";
    private static final String TAG_X = "X";

    // Hashmap for ListView
    HashMap<String, String> data;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.main_variables_fragment, container, false);

        // Creamos el array de variables de lectura
        data = new HashMap<>(); // <string, string>

        //Referencia a controles de la interfaz
        countField = (TextView) view.findViewById(R.id.count);
        tempField = (TextView) view.findViewById(R.id.temperature);
        xField = (EditText) view.findViewById(R.id.x);
        buttonRead = (Button) view.findViewById(R.id.buttonRefresh);
        buttonX = (Button) view.findViewById(R.id.xSend);

        buttonRead.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                readVariables();
            }
        });

        buttonX.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                writeXVariable();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);
    }

    // When user clicks button, calls AsyncTask.
    // Before attempting to fetch the URL, makes sure that there is a network connection.
    public void readVariables() {
        String[] vars = {"COUNT","TEMP","X"};
        String URL = composeReadURL(vars);
        if (checkConnection()) {
            Log.d(DEBUG_TAG, "Connecting to: " + URL);
            new downloadVariablesTask().execute(URL);
        }
    }

    public void writeXVariable() {
        String var = "X";
        writeVariable(var, xField);
    }

    public void writeVariable(String var, EditText editText) {
        String value = editText.getText().toString();
        String URL = composeWriteURL(var, value);
        if (checkConnection()) {
            Log.d(DEBUG_TAG, "Connecting to: " + URL);
            new writeVariableTask().execute(URL);
        }
    }

    public String composeReadURL(String[] vars) {
        String URL = "http://192.168.2.200/cgi-bin/read.cgi?";
        for (int i=0; i<vars.length; i++) {
            if (i==0) {
                URL = URL + "var=" + vars[i];
            }
            else {
                URL = URL + "&var=" + vars[i];
            }
        }
        return URL;
    }

    public String composeWriteURL(String var, String value) {
        return "http://192.168.2.200/cgi-bin/write.cgi?"+var+"="+value;
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
                String count = (String) data.get(TAG_COUNT);
                String temperature = (String) data.get(TAG_TEMP);
                String x = (String) data.get(TAG_X);
                countField.setText(count);
                tempField.setText(temperature);
                xField.setText(x);
            }
        }
    }

    public class writeVariableTask extends AsyncTask<String, Void, HashMap> {
        @Override
        protected HashMap doInBackground(String... params) {
            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadUrl(params[0],false);
            } catch (IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        showUnableToConnectDialog();
                    }
                });
                return null;
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

            // Show toast
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    showSuccessToast();
                }
            });

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
            String count = serverJSON.getString(TAG_COUNT);
            String temp = serverJSON.getString(TAG_TEMP);
            String x = serverJSON.getString(TAG_X);

            // New temp HashMap
            HashMap<String, String> data = new HashMap<>();

            // Adding each child node to Hashmap key => value
            data.put(TAG_COUNT, count);
            data.put(TAG_TEMP, temp);
            data.put(TAG_X, x);

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