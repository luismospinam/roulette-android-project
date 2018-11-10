package com.example.luis.ruleta;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.example.luis.ruleta.logica.GananciasCorrida;
import com.example.luis.ruleta.logica.Ruleta;
import com.example.luis.ruleta.logica.RuletaValidador;
import com.example.luis.ruleta.modelo.MensajeValidacion;
import com.example.luis.ruleta.utilitario.ArchivoUtilitario;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private boolean archivoEncontrado;

    private String historialJugadasTotal;
    private String historialJugadasFlaquita;
    private String historialJugadasGordita;

    private String numerosBolaFlaquita = "";
    private List<String> numerosHistoricosAJugarFlaquita = new ArrayList<>();
    private String numerosBolaGordita = "";
    private List<String>  numerosHistoricosAJugarGordita = new ArrayList<>();

    private String gananciasAcumuladas = "";

    private Button mButton;
    private EditText mEdit;
    private TextView output;
    private TextView numerosJugarTextView;
    private TextView gananciasTextView;
    private CheckBox incluirHistoria;
    private RadioButton radioButtonFlaquitas;
    private RadioButton radioButtonGorditas;
    private RadioButton radioButtonHistorial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = findViewById(R.id.button);
        mEdit = findViewById(R.id.numerosText);
        output = findViewById(R.id.textView2);
        incluirHistoria = findViewById(R.id.checkBox);
        radioButtonFlaquitas = findViewById(R.id.radio_flaquita);
        radioButtonGorditas = findViewById(R.id.radio_gordita);
        radioButtonHistorial = findViewById(R.id.radio_historial_total);
        numerosJugarTextView = findViewById(R.id.numeroJugar);
        gananciasTextView = findViewById(R.id.ganancias);

        radioButtonHistorial.setChecked(true);
        mEdit.addTextChangedListener(listenerTextEditNumerosCambio());

        System.out.println(getExternalCacheDir());

        try {
            historialJugadasTotal = ArchivoUtilitario.getStringFromFile(getExternalCacheDir() + "/jugadas-ruleta-historial-total.txt");
            historialJugadasFlaquita = ArchivoUtilitario.getStringFromFile(getExternalCacheDir() + "/jugadas-ruleta-flaquita.txt");
            historialJugadasGordita = ArchivoUtilitario.getStringFromFile(getExternalCacheDir() + "/jugadas-ruleta-gordita.txt");
            archivoEncontrado = true;
        } catch (Exception e) {
            e.printStackTrace();
            archivoEncontrado = false;
        }

        if (!archivoEncontrado) {
            incluirHistoria.setVisibility(View.GONE);
        } else {
            calcularNumerosJugarHistorial();
        }

        output.setVisibility(View.GONE);
        output.setMovementMethod(new ScrollingMovementMethod());

        mButton.setOnClickListener(view -> calcularEstadisticas(view));
    }

    private void calcularNumerosJugarHistorial() {
        Ruleta flaquita = new Ruleta();
        flaquita.calcularResultado(historialJugadasFlaquita);
        numerosHistoricosAJugarFlaquita = ArchivoUtilitario.orderenarListaNumerosString(flaquita.getNumerosJugar());

        Ruleta gordita = new Ruleta();
        gordita.calcularResultado(historialJugadasGordita);
        numerosHistoricosAJugarGordita = ArchivoUtilitario.orderenarListaNumerosString(gordita.getNumerosJugar());
    }

    private void calcularEstadisticas(View view) {
        mEdit.setVisibility(View.GONE);
        mButton.setVisibility(View.GONE);
        incluirHistoria.setVisibility(View.GONE);
        output.setVisibility(View.VISIBLE);
        radioButtonGorditas.setVisibility(View.GONE);
        radioButtonFlaquitas.setVisibility(View.GONE);
        radioButtonHistorial.setVisibility(View.GONE);
        numerosJugarTextView.setVisibility(View.GONE);
        gananciasTextView.setVisibility(View.GONE);

        String HistorialJugadas = obtenerHistorialSegunRadioButton();
        String jugadasHoy = mEdit.getText().toString();
        MensajeValidacion validacionHoy = RuletaValidador.validarStringJugadas(jugadasHoy);
        if (validacionHoy.isValido()) {
            String jugadas = "";
            if (archivoEncontrado && incluirHistoria.isChecked()) {
                jugadas = HistorialJugadas + jugadasHoy;
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

    private String obtenerHistorialSegunRadioButton() {
        String historial = "";

        if (radioButtonFlaquitas.isChecked()) {
            historial += historialJugadasFlaquita;
        } else if (radioButtonGorditas.isChecked()) {
            historial += historialJugadasGordita;
        } else if (radioButtonHistorial.isChecked()) {
            historial += historialJugadasGordita + historialJugadasFlaquita + historialJugadasTotal;
            if (!numerosBolaGordita.isEmpty()) {
                historial += numerosBolaGordita + ",";
            }
            if (!numerosBolaFlaquita.isEmpty()) {
                historial += numerosBolaFlaquita + ",";
            }
        }

        return historial;
    }

    @Override
    public void onBackPressed() {
        if (archivoEncontrado) {
            incluirHistoria.setVisibility(View.VISIBLE);
        }
        radioButtonGorditas.setVisibility(View.VISIBLE);
        radioButtonFlaquitas.setVisibility(View.VISIBLE);
        radioButtonHistorial.setVisibility(View.VISIBLE);
        mEdit.setVisibility(View.VISIBLE);
        mButton.setVisibility(View.VISIBLE);
        numerosJugarTextView.setVisibility(View.VISIBLE);
        gananciasTextView.setVisibility(View.VISIBLE);
        output.setVisibility(View.GONE);
        output.setText("");
    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.radio_flaquita:
                if (checked) {
                    mEdit.setText(this.numerosBolaFlaquita);
                    mEdit.setEnabled(true);
                    numerosJugarTextView.setText(numerosHistoricosAJugarFlaquita.toString());
                }
                break;
            case R.id.radio_gordita:
                if (checked) {
                    mEdit.setText(this.numerosBolaGordita);
                    mEdit.setEnabled(true);
                    numerosJugarTextView.setText(numerosHistoricosAJugarGordita.toString());
                }
                break;
            case R.id.radio_historial_total:
                if (checked) {
                    mEdit.setText("");
                    mEdit.setEnabled(false);
                    incluirHistoria.setChecked(true);
                    numerosJugarTextView.setText("");
                }
                break;
        }
    }

    private TextWatcher listenerTextEditNumerosCambio() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                appendListaNumerosSegunRadioBox(s.toString());
                calcularGanancias(s.toString());
            }
        };
    }

    private void calcularGanancias(String s) {
        try {
            if(s != null && !s.isEmpty() && RuletaValidador.validarStringJugadas(s).isValido()) {
                if (radioButtonFlaquitas.isChecked()) {
                    int ganancias = GananciasCorrida.calcularGanancias(this.numerosHistoricosAJugarFlaquita, s);
                    this.gananciasTextView.setText("Ganancias de: " + ganancias);
                } else if (radioButtonGorditas.isChecked()) {
                    int ganancias = GananciasCorrida.calcularGanancias(this.numerosHistoricosAJugarGordita, s);
                    this.gananciasTextView.setText("Ganancias de: " + ganancias);
                }
            } else{
                this.gananciasTextView.setText("");
            }
        }
         catch(Exception e){
             this.gananciasTextView.setText("Error al calcular las ganancias: " + e.getMessage());
        }
    }

    private void appendListaNumerosSegunRadioBox(String texto) {
        if (radioButtonFlaquitas.isChecked()) {
            this.numerosBolaFlaquita = texto;
        } else if (radioButtonGorditas.isChecked()) {
            this.numerosBolaGordita = texto;
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
}
