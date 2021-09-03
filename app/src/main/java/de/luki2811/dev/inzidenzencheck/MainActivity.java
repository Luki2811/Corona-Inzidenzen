package de.luki2811.dev.inzidenzencheck;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    int type = 0;

    final static String fileNameSettings = "settings.json";
    final static String fileNameDataKreise = "data_kreise.json";
    final static String fileNameDataBundeslaender = "data_bundeslaender.json";

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        URL urlLand = null, urlBund = null;

        if(availableConnection()){
            try {
                urlLand = new URL("https://services7.arcgis.com/mOBPykOjAyBO2ZKk/arcgis/rest/services/RKI_Landkreisdaten/FeatureServer/0/query?where=1%3D1&outFields=cases7_per_100k,county&returnGeometry=false&outSR=4326&f=json");
                urlBund = new URL("https://services7.arcgis.com/mOBPykOjAyBO2ZKk/arcgis/rest/services/Coronaf%C3%A4lle_in_den_Bundesl%C3%A4ndern/FeatureServer/0/query?where=1%3D1&outFields=cases7_bl_per_100k,LAN_ew_GEN&returnGeometry=false&outSR=4326&f=json");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            InternetRequest land = new InternetRequest(urlLand, this, fileNameDataKreise);
            land.start();
            InternetRequest bund = new InternetRequest(urlBund, this, fileNameDataBundeslaender);
            bund.start();
            try {
                bund.join();
                land.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else{
            Toast.makeText(this,getResources().getString(R.string.error_cant_load_data), Toast.LENGTH_LONG).show();
            TextView output = findViewById(R.id.textOutput);
            output.setText(getResources().getString(R.string.error_no_connection) + "\n" + getResources().getString(R.string.error_app_restart_to_update));
            Button sendButton = findViewById(R.id.button);
            sendButton.setEnabled(false);
        }

        File file = new File(getApplicationContext().getFilesDir(), fileNameSettings);
        Datein datei = new Datein(fileNameSettings);

        if(file.exists()){
            JSONObject jsonTEMP;
            boolean setOld = true;
            try {
                jsonTEMP = new JSONObject(datei.loadFromFile(this));
                setOld = jsonTEMP.getBoolean("automaticPlaceInput");
            } catch (JSONException e) {
                e.printStackTrace();

            }
            if(setOld){
                EditText inputText = findViewById(R.id.textInput);
                RadioButton radio_lk = findViewById(R.id.radioButton_landkreis);
                RadioButton radio_sk = findViewById(R.id.radioButton_stadtkreis);
                RadioButton radio_bl = findViewById(R.id.radioButton_bundesland);
                int oldType = -1;
                JSONObject json;
                String oldLocation = null;

                Datein dfile = new Datein(fileNameSettings);
                try {
                    json = new JSONObject(dfile.loadFromFile(this));
                    oldLocation = json.getString("oldLocation");
                    oldType = json.getInt("oldType");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(oldType == Corona.LANDKREIS){
                    radio_bl.setChecked(false);
                    radio_lk.setChecked(true);
                }else if(oldType == Corona.STADTKREIS){
                    radio_bl.setChecked(false);
                    radio_sk.setChecked(true);
                }else
                    radio_bl.setChecked(true);

                inputText.setText(oldLocation);
            }
            RadioButton radio_lk = findViewById(R.id.radioButton_landkreis);
            RadioButton radio_sk = findViewById(R.id.radioButton_stadtkreis);
            RadioButton radio_bl = findViewById(R.id.radioButton_bundesland);
            if(radio_bl.isChecked())
                type = Corona.BUNDESLAND;
            if(radio_lk.isChecked())
                type = Corona.LANDKREIS;
            if(radio_sk.isChecked())
                type = Corona.STADTKREIS;
            setAutoCompleteList(type);

        }
    }

    public void setType(int type) {
        this.type = type;
    }


    public void onClickRadioButtons(View view){
        int type = 0;
        RadioButton radio_lk = findViewById(R.id.radioButton_landkreis);
        RadioButton radio_sk = findViewById(R.id.radioButton_stadtkreis);
        RadioButton radio_bl = findViewById(R.id.radioButton_bundesland);
        if(radio_bl.isChecked())
            type = Corona.BUNDESLAND;
        if(radio_lk.isChecked())
            type = Corona.LANDKREIS;
        if(radio_sk.isChecked())
            type = Corona.STADTKREIS;
        setAutoCompleteList(type);
    }

    public void setAutoCompleteList(int type) {
        String[] list = null;
        System.out.println("setAuto: "+type);
        if(type == Corona.BUNDESLAND){
            Datein fileBund = new Datein(fileNameDataBundeslaender);
            try {
                JSONArray jsonArrayBund = new JSONObject(fileBund.loadFromFile(this)).getJSONArray("features");
                System.out.println(jsonArrayBund.toString());
                int length = jsonArrayBund.length();

                list = new String[length];
                for(int i = 0; i < jsonArrayBund.length(); i++){
                    list[i] = jsonArrayBund.getJSONObject(i).getJSONObject("attributes").getString("LAN_ew_GEN");
                }

                System.out.println(Arrays.toString(list));

                // Shows null positions
                for(int i = 0; i < length -1 ; i++){
                    if(list[i] == null){
                        System.err.println("BL null at "+ i);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if(type == Corona.LANDKREIS || type == Corona.STADTKREIS){
            Datein fileLand = new Datein(fileNameDataKreise);
            try {
                JSONArray jsonArrayLand = new JSONObject(fileLand.loadFromFile(this)).getJSONArray("features");

                int length = jsonArrayLand.length();

                list = new String[length];
                for (int i = 0; i < jsonArrayLand.length(); i++) {
                    String SkOrLk = jsonArrayLand.getJSONObject(i).getJSONObject("attributes").getString("county");
                    if(SkOrLk.contains("SK") && type == Corona.STADTKREIS)
                        list[i] = jsonArrayLand.getJSONObject(i).getJSONObject("attributes").getString("county").replaceAll("SK ","");
                    if(SkOrLk.contains("LK") && type == Corona.LANDKREIS)
                        list[i] = jsonArrayLand.getJSONObject(i).getJSONObject("attributes").getString("county").replaceAll("LK ","");
                }

                List<String> liste = new ArrayList<>();

                for(String s : list) {
                    if(s != null && s.length() > 0) {
                        liste.add(s);
                    }
                }
                list = new String[liste.size()];
                liste.toArray(list);
            }
            catch(JSONException e){
                e.printStackTrace();
            }
        }
        AutoCompleteTextView autoTextView = findViewById(R.id.textInput);
        System.out.print(Arrays.toString(list));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        autoTextView.setAdapter(adapter);
    }

    public boolean availableConnection(){
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Rundet den übergebenen Wert auf die Anzahl der übergebenen Nachkommastellen
     *
     * @param value ist der zu rundende Wert.
     * @param decimalPoints ist die Anzahl der Nachkommastellen, auf die gerundet werden soll.
     */
    public static double round(double value, int decimalPoints) {
        double d = Math.pow(10, decimalPoints);
        return Math.round(value * d) / d;
    }

    public void clickedButton(View view){

        TextView output = findViewById(R.id.textOutput);
        EditText inputText = findViewById(R.id.textInput);
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        TextView textView_inzidenz = findViewById(R.id.textInzidenz);

        String location = inputText.getText().toString();

        // Abkürzungen nach https://www.destatis.de/DE/Methoden/abkuerzung-bundeslaender-DE-EN.html

        if (location.equalsIgnoreCase("bw"))
            location = "Baden-Württemberg";
        if (location.equalsIgnoreCase("by"))
            location = "Bayern";
        if (location.equalsIgnoreCase("be"))
            location = "Berlin";
        if (location.equalsIgnoreCase("bb"))
            location = "Brandenburg";
        if (location.equalsIgnoreCase("hb"))
            location = "Bremen";
        if (location.equalsIgnoreCase("hh"))
            location = "Hamburg";
        if (location.equalsIgnoreCase("he"))
            location = "Hessen";
        if (location.equalsIgnoreCase("mv"))
            location = "Mecklenburg-Vorpommern";
        if (location.equalsIgnoreCase("ni"))
            location = "Niedersachsen";
        if (location.equalsIgnoreCase("nrw") || location.equalsIgnoreCase("nw"))
            location = "Nordrhein-Westfalen";
        if (location.equalsIgnoreCase("rp"))
            location = "Rheinland-Pfalz";
        if (location.equalsIgnoreCase("sl"))
            location = "Saarland";
        if (location.equalsIgnoreCase("sn"))
            location = "Sachsen";
        if (location.equalsIgnoreCase("st"))
            location = "Sachsen-Anhalt";
        if (location.equalsIgnoreCase("sh"))
            location = "Schleswig-Holstein";
        if (location.equalsIgnoreCase("th"))
            location = "Thüringen";


        int type = -1;

        if(radioGroup.getCheckedRadioButtonId() == R.id.radioButton_bundesland)
            type = Corona.BUNDESLAND;
        if(radioGroup.getCheckedRadioButtonId() == R.id.radioButton_landkreis)
            type = Corona.LANDKREIS;
        if(radioGroup.getCheckedRadioButtonId() == R.id.radioButton_stadtkreis)
            type = Corona.STADTKREIS;

        Corona corona = null;
        if(availableConnection()){
            Datein file = new Datein(fileNameSettings);
            try {
                corona = new Corona(location, type, this);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if(corona.getLocation() == null){
                if(type == Corona.LANDKREIS)
                    Toast.makeText(this, getResources().getString(R.string.error_cant_find_landkreis) , Toast.LENGTH_LONG).show();
                else if(type == Corona.STADTKREIS)
                    Toast.makeText(this, getResources().getString(R.string.error_cant_find_stadtkreis), Toast.LENGTH_LONG).show();
                else if(type == Corona.BUNDESLAND)
                    Toast.makeText(this, getResources().getString(R.string.error_cant_find_bundesland), Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(this,getResources().getString(R.string.error_analysis), Toast.LENGTH_LONG).show();
            }
            else if (corona != null) {
                // set settings
                JSONObject jsonInFile;
                try {
                    jsonInFile = new JSONObject(file.loadFromFile(this));
                    jsonInFile.put("oldLocation", location);
                    jsonInFile.put("oldType", type);
                    file.writeInFile(jsonInFile.toString(), this);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // set text
                output.setText(corona.getLocation() + " hat eine Coronainzidenz von:");
                textView_inzidenz.setText("" + corona.getIncidence() + "");
                // set color for each incidence
                if(corona.getIncidence() >= 200)
                    textView_inzidenz.setTextColor(this.getColor(R.color.DarkRed));
                else if(corona.getIncidence() >= 100 && corona.getIncidence() < 200)
                    textView_inzidenz.setTextColor(this.getColor(R.color.Red));
                else if(corona.getIncidence() >= 50 && corona.getIncidence() < 100)
                    textView_inzidenz.setTextColor(this.getColor(R.color.Orange));
                else if(corona.getIncidence() >= 25 && corona.getIncidence() < 50)
                    textView_inzidenz.setTextColor(this.getColor(R.color.Yellow));
                else if(corona.getIncidence() >= 10 && corona.getIncidence() < 25)
                    textView_inzidenz.setTextColor(this.getColor(R.color.Green));
                else if(corona.getIncidence() < 10)
                    textView_inzidenz.setTextColor(this.getColor(R.color.DarkGreen));
                else
                    textView_inzidenz.setTextColor(this.getColor(R.color.Gray));
            }
        }else{
            output.setText(getResources().getString(R.string.error_no_connection));
        }
    }
    public void openSettings(View view){
        Intent intent = new Intent(this, Quellen.class);
        startActivity(intent);
    }
}