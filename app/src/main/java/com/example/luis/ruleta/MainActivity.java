package com.example.luis.ruleta;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.example.luis.ruleta.logica.Ruleta;
import com.example.luis.ruleta.logica.RuletaValidador;
import com.example.luis.ruleta.modelo.MensajeValidacion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private Button mButton;
    private EditText mEdit;
    private TextView output;
    private CheckBox incluirHistoria;
    private boolean archivoEncontrado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = findViewById(R.id.button);
        mEdit = findViewById(R.id.numerosText);
        output = findViewById(R.id.textView2);
        incluirHistoria = findViewById(R.id.checkBox);

        System.out.println(getExternalCacheDir());

        String historialJugadas = "";
        try {
            historialJugadas = getStringFromFile(getExternalCacheDir() + "/jugadas-ruleta.txt");
            archivoEncontrado = true;
        } catch (Exception e) {
            e.printStackTrace();
            archivoEncontrado = false;
        }

        if (!archivoEncontrado) {
            incluirHistoria.setVisibility(View.GONE);
        }

        output.setVisibility(View.GONE);
        output.setMovementMethod(new ScrollingMovementMethod());

        String finalHistorialJugadas = historialJugadas;
        mButton.setOnClickListener(view -> calcularEstadisticas(view, finalHistorialJugadas));
    }

    @Override
    public void onBackPressed() {
        if (archivoEncontrado) {
            incluirHistoria.setVisibility(View.VISIBLE);
        }
        mEdit.setVisibility(View.VISIBLE);
        mButton.setVisibility(View.VISIBLE);
        output.setVisibility(View.GONE);
        output.setText("");
    }

    private void calcularEstadisticas(View view, String finalHistorialJugadas) {
        mEdit.setVisibility(View.GONE);
        mButton.setVisibility(View.GONE);
        incluirHistoria.setVisibility(View.GONE);
        output.setVisibility(View.VISIBLE);

        String jugadasHoy = mEdit.getText().toString();
        MensajeValidacion validacionHoy = RuletaValidador.validarStringJugadas(jugadasHoy);
        if (validacionHoy.isValido()) {
            String jugadas = "";
            if (archivoEncontrado && incluirHistoria.isChecked()) {
                jugadas = finalHistorialJugadas + jugadasHoy;
            } else {
                jugadas = jugadasHoy;
            }

            if (!jugadas.equals("")) {
                Ruleta ruleta = new Ruleta();
                String resultado = ruleta.calcularResultado(jugadas);
                output.setText(Html.fromHtml(resultado, Html.FROM_HTML_MODE_LEGACY));
            } else {
                mostrarPopupMensaje("No hay jugadas para calcular.");
            }
        } else {
            mostrarPopupMensaje(validacionHoy.getMensaje());
        }
    }

    private void mostrarPopupMensaje(String mensaje) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(mensaje);
        alertDialogBuilder.setNegativeButton("OK", (dialog, which) -> {
            dialog.cancel();
            onBackPressed();
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
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

    private String getStringFromFile(String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        fin.close();
        return ret;
    }
}
