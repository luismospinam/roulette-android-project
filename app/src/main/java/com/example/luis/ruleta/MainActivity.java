package com.example.luis.ruleta;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.example.luis.ruleta.logica.Ruleta;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    Button mButton;
    EditText mEdit;
    TextView output;
    CheckBox incluirHistoria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.out.println(getExternalCacheDir());

        String historialJugadas = "";
        try {
            historialJugadas = getStringFromFile(getExternalCacheDir() + "/jugadas-ruleta.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }

        mButton = (Button) findViewById(R.id.button);
        mEdit = (EditText) findViewById(R.id.numerosText);
        output = (TextView) findViewById(R.id.textView2);
        incluirHistoria = (CheckBox)  findViewById(R.id.checkBox);

        output.setVisibility(View.GONE);
        output.setMovementMethod(new ScrollingMovementMethod());

        String finalHistorialJugadas = historialJugadas;
        mButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        Log.v("EditText", mEdit.getText().toString());
                        mEdit.setVisibility(View.GONE);
                        mButton.setVisibility(View.GONE);
                        incluirHistoria.setVisibility(View.GONE);
                        output.setVisibility(View.VISIBLE);

                        String jugadas;
                        if(incluirHistoria.isChecked()){
                            jugadas = finalHistorialJugadas + mEdit.getText().toString();
                        }else{
                            jugadas = mEdit.getText().toString();
                        }
                        Ruleta ruleta = new Ruleta();
                        String resultado = ruleta.calcularResultado(jugadas);
                        output.setText(Html.fromHtml(resultado, Html.FROM_HTML_MODE_LEGACY));
                    }
                });
    }

    @Override
    public void onBackPressed() {
        mEdit.setVisibility(View.VISIBLE);
        mButton.setVisibility(View.VISIBLE);
        incluirHistoria.setVisibility(View.VISIBLE);
        output.setVisibility(View.GONE);
        output.setText("");
    }

    private String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append(",");
        }
        reader.close();
        return sb.toString();
    }

    private String getStringFromFile (String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        fin.close();
        return ret;
    }
}
