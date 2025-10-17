package com.example.plantas;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ListarActivity extends AppCompatActivity {

    private TextView textViewDatos;
    private Button btnFiltrarNombre, btnFiltrarTipo, btnFiltrarFecha;
    private static final String SERVER_BASE_URL = "http://192.168.0.60:5000/api/juegos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listar);

        // Inicializar vistas
        textViewDatos = findViewById(R.id.textView2);
        btnFiltrarNombre = findViewById(R.id.btnFiltrarNombre);
        btnFiltrarTipo = findViewById(R.id.btnFiltrarTipo);
        btnFiltrarFecha = findViewById(R.id.btnFiltrarFecha);
        Button btnAtras = findViewById(R.id.btnAtras);

        // Cargar todos los datos al iniciar (sin ordenar)
        cargarDatos(SERVER_BASE_URL, "Todos los juegos:");

        // Configurar botones
        btnFiltrarNombre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cargarDatos(SERVER_BASE_URL + "/ordenar/nombre", "Ordenado por NOMBRE (A-Z):");
            }
        });

        btnFiltrarTipo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cargarDatos(SERVER_BASE_URL + "/ordenar/tipo", "Ordenado por tipo:");
            }
        });

        btnFiltrarFecha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cargarDatos(SERVER_BASE_URL + "/ordenar/year", "Ordenado por año (Ascendente):");
            }
        });

        btnAtras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void cargarDatos(final String urlString, final String titulo) {
        textViewDatos.setText("Cargando datos...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    int responseCode = connection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        final String jsonResponse = response.toString();

                        // Actualizar UI en el hilo principal
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mostrarDatos(jsonResponse, titulo);
                            }
                        });
                    } else {
                        throw new Exception("Error HTTP: " + responseCode);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textViewDatos.setText("Error al cargar datos: " + e.getMessage() +
                                    "\n\nAsegúrate de que:\n" +
                                    "1. El servidor esté ejecutándose\n" +
                                    "2. La IP sea correcta\n" +
                                    "3. Estés en la misma red WiFi");
                        }
                    });
                }
            }
        }).start();
    }

    private void mostrarDatos(String jsonResponse, String titulo) {
        try {
            JSONArray juegosArray = new JSONArray(jsonResponse);
            StringBuilder datos = new StringBuilder(titulo + "\n\n");

            if (juegosArray.length() == 0) {
                datos.append("No se encontraron juegos");
            } else {
                for (int i = 0; i < juegosArray.length(); i++) {
                    JSONObject juego = juegosArray.getJSONObject(i);
                    String nombre = juego.optString("nombre", "Desconocido");
                    String tipo = juego.optString("tipo", "Desconocido");
                    int year = juego.optInt("year", 0);

                    datos.append("Juego ").append(i + 1).append(":\n")
                            .append("   Nombre: ").append(nombre).append("\n")
                            .append("   Tipo: ").append(tipo).append("\n")
                            .append("   Año: ").append(year).append("\n\n");
                }

                datos.append("Total: ").append(juegosArray.length()).append(" juegos");
            }

            textViewDatos.setText(datos.toString());

        } catch (Exception e) {
            textViewDatos.setText("Error al procesar datos: " + e.getMessage() +
                    "\n\nRespuesta del servidor:\n" + jsonResponse);
        }
    }
}