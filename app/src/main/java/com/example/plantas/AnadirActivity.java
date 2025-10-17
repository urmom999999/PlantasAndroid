package com.example.plantas;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AnadirActivity extends AppCompatActivity {

    private EditText editTextNombre, editTextTipo, editTextYear;
    private Button btnAgregar, btnAtras;
    private static final String SERVER_URL = "http://192.168.0.60:5000/api/juegos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anadir);

        // Inicializar vistas
        editTextNombre = findViewById(R.id.agregnombre);
        editTextTipo = findViewById(R.id.agregtipo);
        editTextYear = findViewById(R.id.agregaryear);
        btnAgregar = findViewById(R.id.btnAgregar);
        btnAtras = findViewById(R.id.btnAtras);

        // Configurar botón Agregar
        btnAgregar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                agregarJuego();
            }
        });

        // Configurar botón Atrás
        btnAtras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void agregarJuego() {
        // Obtener datos de los EditText
        String nombre = editTextNombre.getText().toString().trim();
        String tipo = editTextTipo.getText().toString().trim();
        String yearStr = editTextYear.getText().toString().trim();

        // Validar que todos los campos estén llenos
        if (nombre.isEmpty() || tipo.isEmpty() || yearStr.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar que el año sea un número válido
        int year;
            year = Integer.parseInt(yearStr);


        // Mostrar mensaje de carga
        btnAgregar.setEnabled(false);
        btnAgregar.setText("Agregando...");

        // Enviar datos al servidor en un hilo separado
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Crear el objeto JSON con los datos del juego
                    JSONObject juegoJson = new JSONObject();
                    juegoJson.put("nombre", nombre);
                    juegoJson.put("tipo", tipo);
                    juegoJson.put("year", year);

                    // Configurar la conexión HTTP
                    URL url = new URL(SERVER_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    // Enviar los datos JSON
                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(juegoJson.toString().getBytes());
                    outputStream.flush();
                    outputStream.close();

                    // Obtener la respuesta del servidor
                    int responseCode = connection.getResponseCode();

                    // Procesar la respuesta en el hilo principal
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnAgregar.setEnabled(true);
                            btnAgregar.setText("Añadir");

                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                // Éxito: limpiar campos y mostrar mensaje
                                editTextNombre.setText("");
                                editTextTipo.setText("");
                                editTextYear.setText("");
                                Toast.makeText(AnadirActivity.this, "¡Juego agregado exitosamente!", Toast.LENGTH_SHORT).show();
                            } else {
                                // Error
                                Toast.makeText(AnadirActivity.this, "Error al agregar juego. Código: " + responseCode, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    connection.disconnect();

                } catch (Exception e) {
                    e.printStackTrace();
                    // Manejar error en el hilo principal
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnAgregar.setEnabled(true);
                            btnAgregar.setText("Añadir");
                            Toast.makeText(AnadirActivity.this, "Error de conexión: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
}