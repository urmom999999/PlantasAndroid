package com.example.plantas;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BorrarActivity extends AppCompatActivity {

    private EditText editTextNombre;
    private Button btnEliminar, btnAtras;
    private static final String SERVER_BASE_URL = "http://192.168.0.60:5000/api/juegos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_borrar);

        // Inicializar vistas
        editTextNombre = findViewById(R.id.eliminartext);
        btnEliminar = findViewById(R.id.btnEliminar);
        btnAtras = findViewById(R.id.btnAtras);

        // Configurar botón Eliminar
        btnEliminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eliminarJuego();
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

    private void eliminarJuego() {
        // Obtener el nombre del juego a eliminar
        String nombre = editTextNombre.getText().toString().trim();

        // Validar que el campo no esté vacío
        if (nombre.isEmpty()) {
            Toast.makeText(this, "Por favor, escriba el nombre del juego a eliminar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar mensaje de confirmación
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Confirmar eliminación")
                .setMessage("¿Está seguro de que desea eliminar el juego:\n\"" + nombre + "\"?")
                .setPositiveButton("Sí, eliminar", (dialog, which) -> {
                    // Si confirma, proceder con la eliminación
                    ejecutarEliminacion(nombre);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    // No hacer nada, solo cerrar el diálogo
                })
                .show();
    }

    private void ejecutarEliminacion(String nombre) {
        // Mostrar mensaje de carga
        btnEliminar.setEnabled(false);
        btnEliminar.setText("Eliminando...");

        // Ejecutar en hilo separado
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    // Codificar el nombre para la URL (por si tiene espacios o caracteres especiales)
                    String nombreCodificado = java.net.URLEncoder.encode(nombre, "UTF-8");
                    String urlEliminar = SERVER_BASE_URL + "/" + nombreCodificado;

                    // Configurar conexión HTTP DELETE
                    URL url = new URL(urlEliminar);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("DELETE");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    // Obtener respuesta
                    int responseCode = connection.getResponseCode();

                    // Leer la respuesta
                    BufferedReader reader;
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } else {
                        reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    }

                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    final String jsonResponse = response.toString();

                    // Procesar respuesta en el hilo principal
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnEliminar.setEnabled(true);
                            btnEliminar.setText("Eliminar");

                            try {
                                JSONObject jsonObject = new JSONObject(jsonResponse);

                                if (responseCode == HttpURLConnection.HTTP_OK) {
                                    // Éxito
                                    editTextNombre.setText("");
                                    String mensaje = jsonObject.getString("mensaje");
                                    Toast.makeText(BorrarActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                                } else {
                                    // Error
                                    String error = jsonObject.getString("error");
                                    Toast.makeText(BorrarActivity.this, error, Toast.LENGTH_LONG).show();
                                }

                            } catch (Exception e) {
                                // Si no se puede parsear como JSON, mostrar respuesta cruda
                                if (responseCode == HttpURLConnection.HTTP_OK) {
                                    Toast.makeText(BorrarActivity.this, "Juego eliminado exitosamente", Toast.LENGTH_SHORT).show();
                                    editTextNombre.setText("");
                                } else {
                                    Toast.makeText(BorrarActivity.this, "Error: " + jsonResponse, Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    // Manejar error en el hilo principal
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnEliminar.setEnabled(true);
                            btnEliminar.setText("Eliminar");
                            Toast.makeText(BorrarActivity.this, "Error de conexión: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }
}