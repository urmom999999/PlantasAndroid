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

public class ActualizarActivity extends AppCompatActivity {

    private EditText editTextOldNombre, editTextNewNombre, editTextNewTipo, editTextNewYear;
    private Button btnActualizar, btnAtras;
    private static final String SERVER_BASE_URL = "http://192.168.0.60:5000/api/juegos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actualizar);

        // Inicializar vistas
        editTextOldNombre = findViewById(R.id.oldnombre);
        editTextNewNombre = findViewById(R.id.actnombre);
        editTextNewTipo = findViewById(R.id.acttipo);
        editTextNewYear = findViewById(R.id.actyear);
        btnActualizar = findViewById(R.id.btnActualizar);
        btnAtras = findViewById(R.id.btnAtras);

        // Configurar botón Actualizar
        btnActualizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actualizarJuego();
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

    private void actualizarJuego() {
        // Obtener datos de los EditText
        String oldNombre = editTextOldNombre.getText().toString().trim();
        String newNombre = editTextNewNombre.getText().toString().trim();
        String newTipo = editTextNewTipo.getText().toString().trim();
        String newYearStr = editTextNewYear.getText().toString().trim();

        // Validar que el nombre viejo no esté vacío
        if (oldNombre.isEmpty()) {
            Toast.makeText(this, "Por favor, escriba el nombre del juego a actualizar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar que al menos un campo nuevo esté lleno
        if (newNombre.isEmpty() && newTipo.isEmpty() && newYearStr.isEmpty()) {
            Toast.makeText(this, "Por favor, complete al menos un campo nuevo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar que el año sea un número válido si se proporciona
        Integer newYear = null;
        if (!newYearStr.isEmpty()) {
            try {
                newYear = Integer.parseInt(newYearStr);
                if (newYear < 1900 || newYear > 2030) {
                    Toast.makeText(this, "Por favor, ingrese un año válido (1900-2030)", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Por favor, ingrese un año válido", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Mostrar mensaje de confirmación
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        Integer finalNewYear = newYear;
        builder.setTitle("Confirmar actualización")
                .setMessage("¿Está seguro de que desea actualizar el juego:\n\"" + oldNombre + "\"?")
                .setPositiveButton("Sí, actualizar", (dialog, which) -> {
                    // Si confirma, proceder con la actualización
                    ejecutarActualizacion(oldNombre, newNombre, newTipo, finalNewYear);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    // No hacer nada, solo cerrar el diálogo
                })
                .show();
    }

    private void ejecutarActualizacion(String oldNombre, String newNombre, String newTipo, Integer newYear) {
        // Mostrar mensaje de carga
        btnActualizar.setEnabled(false);
        btnActualizar.setText("Actualizando...");

        // Ejecutar en hilo separado
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    // Codificar el nombre viejo para la URL
                    String oldNombreCodificado = java.net.URLEncoder.encode(oldNombre, "UTF-8");
                    String urlActualizar = SERVER_BASE_URL + "/" + oldNombreCodificado;

                    // Crear el objeto JSON con los nuevos datos
                    JSONObject nuevosDatos = new JSONObject();
                    if (!newNombre.isEmpty()) {
                        nuevosDatos.put("nombre", newNombre);
                    }
                    if (!newTipo.isEmpty()) {
                        nuevosDatos.put("tipo", newTipo);
                    }
                    if (newYear != null) {
                        nuevosDatos.put("year", newYear);
                    }

                    // Configurar conexión HTTP PUT
                    URL url = new URL(urlActualizar);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("PUT");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    // Enviar los datos JSON
                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(nuevosDatos.toString().getBytes());
                    outputStream.flush();
                    outputStream.close();

                    // Obtener respuesta
                    int responseCode = connection.getResponseCode();

                    // Leer la respuesta
                    java.io.BufferedReader reader;
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
                    } else {
                        reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getErrorStream()));
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
                            btnActualizar.setEnabled(true);
                            btnActualizar.setText("Actualizar");

                            try {
                                JSONObject jsonObject = new JSONObject(jsonResponse);

                                if (responseCode == HttpURLConnection.HTTP_OK) {
                                    // Éxito - limpiar campos
                                    editTextOldNombre.setText("");
                                    editTextNewNombre.setText("");
                                    editTextNewTipo.setText("");
                                    editTextNewYear.setText("");

                                    String mensaje = jsonObject.getString("mensaje");
                                    Toast.makeText(ActualizarActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                                } else {
                                    // Error
                                    String error = jsonObject.getString("error");
                                    Toast.makeText(ActualizarActivity.this, error, Toast.LENGTH_LONG).show();
                                }

                            } catch (Exception e) {
                                // Si no se puede parsear como JSON, mostrar respuesta cruda
                                if (responseCode == HttpURLConnection.HTTP_OK) {
                                    Toast.makeText(ActualizarActivity.this, "Juego actualizado exitosamente", Toast.LENGTH_SHORT).show();
                                    // Limpiar campos
                                    editTextOldNombre.setText("");
                                    editTextNewNombre.setText("");
                                    editTextNewTipo.setText("");
                                    editTextNewYear.setText("");
                                } else {
                                    Toast.makeText(ActualizarActivity.this, "Error: " + jsonResponse, Toast.LENGTH_LONG).show();
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
                            btnActualizar.setEnabled(true);
                            btnActualizar.setText("Actualizar");
                            Toast.makeText(ActualizarActivity.this, "Error de conexión: " + e.getMessage(), Toast.LENGTH_LONG).show();
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