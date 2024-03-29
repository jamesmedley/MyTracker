package com.TrackManInc.mytracker;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.AlignmentSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.TrackManInc.mytracker.Filters.DecimalDigitsInputFilter;
import com.TrackManInc.mytracker.Filters.TextInputFilter;
import com.TrackManInc.mytracker.Prevalent.Prevalent;
import com.budiyev.android.codescanner.AutoFocusMode;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.budiyev.android.codescanner.ErrorCallback;
import com.budiyev.android.codescanner.ScanMode;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.Result;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddFoodActivity extends AppCompatActivity {


    private String scannedCodeValue = null;
    private String previousScannedCodeValue = "none";
    private static final int CAMERA_REQUEST_CODE = 101;
    private CodeScanner mCodeScanner;
    private org.jsoup.nodes.Document document = null;
    private String foodName,carbAmount,proteinAmount,fatAmount,saltAmount,fiberAmount,dateHtml,quantity,servingSize;

    private EditText foodNameET,carbsET,proteinET,fatsET,saltET,fiberET,dateET,quantityET,servingSizeET;
    private DatePickerDialog.OnDateSetListener setListener;
    private Button saveButton;//,deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_add_food);
        setupUIView();
        initDatePicker();
        setupPermissionsCamera();
        setupCodeScanner();
    }

    private void setupUIView() {
        carbsET = findViewById(R.id.carbs);
        carbsET.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(3)});
        proteinET = findViewById(R.id.protein);
        proteinET.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(3)});
        fatsET = findViewById(R.id.fats);
        fatsET.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(3)});
        saltET = findViewById(R.id.salt);
        saltET.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(3)});
        fiberET = findViewById(R.id.fiber);
        fiberET.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(3)});
        dateET = findViewById(R.id.date);
        dateET.setFocusable(false);
        dateET.setKeyListener(null);
        foodNameET = findViewById(R.id.food_name);
        foodNameET.setFilters(new InputFilter[]{new TextInputFilter()});
        saveButton = findViewById(R.id.save_input_button);
        quantityET = findViewById(R.id.quantity);
        quantityET.setText("1");
        servingSizeET = findViewById(R.id.serving_size);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveInput();
            }
        });
        setupETListeners();
    }

    public void saveInput(){
        if(checkNoInput("Carbohydrates",carbsET)|| checkNoInput("Protein",proteinET)||
            checkNoInput("Fats",fatsET)|| checkNoInput("Salt",saltET)||
            checkNoInput("Fiber",fiberET)|| checkNoInput("Date",dateET)||
            checkNoInput("Quantity",quantityET)|| checkNoInput("Serving size",servingSizeET)){
            return;
        }
        final DatabaseReference RootRef = FirebaseDatabase.getInstance().getReference();
        HashMap<String,Object> userDataMap = new HashMap<>();
        userDataMap.put("carbs",carbAmount);
        userDataMap.put("protein",proteinAmount);
        userDataMap.put("fat",fatAmount);
        userDataMap.put("salt",saltAmount);
        userDataMap.put("fiber",fiberAmount);
        userDataMap.put("serving",servingSize);
        quantity = quantityET.getText().toString();
        userDataMap.put("quantity",quantity);
        foodName = removeUnwantedCharacters(foodName);
        RootRef.child("User Foods").child(Prevalent.currentOnlineUser.getName()).child(dateHtml).child(foodName).updateChildren(userDataMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            toastMessage("Food item added.");
                            finish();
                        }else{
                            toastMessage("Network Error: Please try again after some time...");
                        }
                    }
                });
        finish();
    }

    private String removeUnwantedCharacters(String foodName) {
        String s = "";
        for (char c:foodName.toCharArray()){
            if(c=='.'||c=='#'||c=='$'||c=='['||c==']'){
                s+="_";
                continue;
            }
            s+=String.valueOf(c);
        }
        return s;
    }

    private boolean checkNoInput(String nutrient,EditText editText) {
        if(editText.getText().toString().equals("") || editText.getText().toString().equals("?")
                || editText.getText().toString().equals("g")|| editText.getText().toString().equals("?g")){
            toastMessage("Enter a value for "+nutrient);
            return true;
        }
        return false;
    }
    private void initDatePicker(){
        Calendar calender = Calendar.getInstance();
        final int year = calender.get(Calendar.YEAR);
        final int month = calender.get(Calendar.MONTH);
        final int day = calender.get(Calendar.DAY_OF_MONTH);
        setListener = new DatePickerDialog.OnDateSetListener(){
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month = month+1;
                String date;
                String day = String.valueOf(dayOfMonth);
                String m = String.valueOf(month);
                if(dayOfMonth<10){
                    day = "0"+dayOfMonth;
                }
                if(month<10){
                    m = "0"+month;
                }
                date = day+"/"+m+"/"+year;
                dateHtml = year+"/"+m+"/"+day;
                dateET.setText(date);
            }
        };
        dateET.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(AddFoodActivity.this, R.style.DialogTheme,setListener,
                        year,month,day);
                datePickerDialog.getDatePicker().setMaxDate(new Date().getTime());
                datePickerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
                datePickerDialog.show();
            }
        });
    }
    private void setupCodeScanner(){
        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(this, scannerView);
        mCodeScanner.setCamera(mCodeScanner.CAMERA_BACK);
        mCodeScanner.setFormats(mCodeScanner.ALL_FORMATS);
        mCodeScanner.setAutoFocusMode(AutoFocusMode.SAFE);
        mCodeScanner.setScanMode(ScanMode.CONTINUOUS);
        mCodeScanner.setAutoFocusEnabled(true);
        mCodeScanner.setFlashEnabled(false);

        mCodeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(final Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scannedCodeValue = result.getText();
                        if(!Objects.equals(scannedCodeValue, previousScannedCodeValue)){
                            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                            progressBar.setVisibility(View.VISIBLE);
                            description_webscrape dw = new description_webscrape();
                            dw.execute();
                        }
                        previousScannedCodeValue = scannedCodeValue;
                    }
                });
            }
        });
        mCodeScanner.setErrorCallback(new ErrorCallback() {
            @Override
            public void onError(@NonNull Throwable thrown) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("Main","Camera initialization error: "+thrown.getMessage());
                    }
                });
            }
        });
        scannerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCodeScanner.startPreview();
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        mCodeScanner.startPreview();
    }

    @Override
    protected void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }
    private void setupPermissionsCamera(){
        int permission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA);

        if(permission!= PackageManager.PERMISSION_GRANTED){
            makeRequest();
        }
    }
    private void makeRequest(){
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA},CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    toastMessage( "You need the camera permission to be able to use the scanner");
                } else {
                    //successful
                }
        }
    }
    private void toastMessage(String message){
        Spannable centeredText = new SpannableString(message);
        centeredText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),0,message.length()-1,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        Toast.makeText(getApplicationContext(), centeredText, Toast.LENGTH_SHORT).show();
    }
    private void toastFailMessage(){
        if(foodNameET.getText().toString().equals("") || document==null) {
            String text = "Could not find scanned code. Check connection to internet or enter manually by touching";
            Spannable centeredText = new SpannableString(text);
            centeredText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),0,text.length()-1,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            Toast.makeText(getApplicationContext(), centeredText, Toast.LENGTH_SHORT).show();
        }
    }
    private class description_webscrape extends AsyncTask<Void,Void,Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                document = Jsoup.connect(("https://world.openfoodfacts.org/product/"+scannedCodeValue)).get();;
                if(document!=null) {
                    foodName = document.getElementsByTag("h1").first().text();
                    Elements elements = document.getElementsByTag("table");
                    carbAmount = getFoodDataValue("Carbohydrates ",elements.text());
                    proteinAmount = getFoodDataValue("Proteins ",elements.text());
                    fatAmount = getFoodDataValue("Fat ",elements.text());
                    saltAmount = getFoodDataValue("Salt ",elements.text());
                    fiberAmount = getFoodDataValue("Fiber ",elements.text());
                    //TODO:Fix amount scraper
                    servingSize = getFoodDataValue("Serving size:",elements.text());
                    //System.out.println(document.getElementById("div#panel_serving_size.panel_inline_radius").text());
                    if(servingSize.equals("?")){
                        servingSize = "100";
                    }
                }
            } catch (IOException e) {
                //failed
            }
            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            //if(!foodNameEditText.getText().toString().equals(foodName)) {
            foodNameET.setText(foodName);
            carbsET.setText(carbAmount+"g");
            proteinET.setText(proteinAmount+"g");
            fatsET.setText(fatAmount+"g");
            saltET.setText(saltAmount+"g");
            fiberET.setText(fiberAmount+"g");
            servingSizeET.setText(servingSize+"g");
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.INVISIBLE);
            //}
            if(foodNameET.getText().toString().equals("")){
                toastFailMessage();
            }
        }
    }
    private String getFoodDataValue(String foodData,String extractedDataString){
        String result = "?";
        Matcher matcher = Pattern.compile("("+foodData+"\\d+\\p{Punct}?\\d*)").matcher(extractedDataString);
        if (matcher.find()) {// if it matched the pattern
            result = matcher.group(0);// the group captured by the regex
            if(result!=null) {
                result = result.substring(foodData.length());
            }
        }
        return result;
    }
    private void setupETListeners() {
        foodNameET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                foodName = foodNameET.getText().toString();
            }
        });
        carbsET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                StringBuilder temp = new StringBuilder(carbsET.getText().toString());
                char[] charTemp = temp.toString().toCharArray();
                temp = new StringBuilder();
                for (char c : charTemp) {
                    if (!Character.isDigit(c) && c!='.') {
                        break;
                    }
                    temp.append(c);
                }
                carbAmount = temp.toString();
            }
        });
        proteinET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                StringBuilder temp = new StringBuilder(proteinET.getText().toString());
                char[] charTemp = temp.toString().toCharArray();
                temp = new StringBuilder();
                for (char c : charTemp) {
                    if (!Character.isDigit(c)&& c!='.') {
                        break;
                    }
                    temp.append(c);
                }
                proteinAmount = temp.toString();
            }
        });
        fatsET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                StringBuilder temp = new StringBuilder(fatsET.getText().toString());
                char[] charTemp = temp.toString().toCharArray();
                temp = new StringBuilder();
                for (char c : charTemp) {
                    if (!Character.isDigit(c)&& c!='.') {
                        break;
                    }
                    temp.append(c);
                }
                fatAmount = temp.toString();
            }
        });
        saltET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                StringBuilder temp = new StringBuilder(saltET.getText().toString());
                char[] charTemp = temp.toString().toCharArray();
                temp = new StringBuilder();
                for (char c : charTemp) {
                    if (!Character.isDigit(c)&& c!='.') {
                        break;
                    }
                    temp.append(c);
                }
                saltAmount = temp.toString();
            }
        });
        fiberET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                StringBuilder temp = new StringBuilder(fiberET.getText().toString());
                char[] charTemp = temp.toString().toCharArray();
                temp = new StringBuilder();
                for (char c : charTemp) {
                    if (!Character.isDigit(c)&& c!='.') {
                        break;
                    }
                    temp.append(c);
                }
                fiberAmount = temp.toString();
            }
        });
        servingSizeET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                StringBuilder temp = new StringBuilder(servingSizeET.getText().toString());
                char[] charTemp = temp.toString().toCharArray();
                temp = new StringBuilder();
                for (char c : charTemp) {
                    if (!Character.isDigit(c)&& c!='.') {
                        break;
                    }
                    temp.append(c);
                }
                servingSize = temp.toString();
            }
        });
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }


}