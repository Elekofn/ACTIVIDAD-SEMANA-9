package com.example.perfil

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    // Dirección del servicio Flask en EC2 (IP pública + puerto abierto en el Security Group)
    private val URL_BASE = "http://34.205.110.30:8000"

    // Cliente HTTP: se crea una sola vez y se reutiliza para todas las peticiones
    private val cliente = OkHttpClient()

    // Referencias a las vistas del layout
    private lateinit var etLogin: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnEntrar: Button
    private lateinit var tvEstado: TextView
    private lateinit var btnPerfil: Button
    private lateinit var tvNombre: TextView
    private lateinit var tvCorreo: TextView
    private lateinit var tvRut: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Conectar variables con los elementos del XML
        etLogin = findViewById(R.id.etLogin)
        etPassword = findViewById(R.id.etPassword)
        btnEntrar = findViewById(R.id.btnEntrar)
        tvEstado = findViewById(R.id.tvEstado)
        btnPerfil = findViewById(R.id.btnPerfil)
        tvNombre = findViewById(R.id.tvNombre)
        tvCorreo = findViewById(R.id.tvCorreo)
        tvRut = findViewById(R.id.tvRut)

        // Acción de cada botón
        btnEntrar.setOnClickListener { hacerLogin() }
        btnPerfil.setOnClickListener { obtenerPerfil() }
    }

    // POST /login: envía {"login": ..., "password": ...} y procesa la respuesta
    private fun hacerLogin() {
        val login = etLogin.text.toString().trim()
        val password = etPassword.text.toString()

        if (login.isEmpty() || password.isEmpty()) {
            tvEstado.text = "Completa usuario y contraseña"
            return
        }

        // Construir el cuerpo JSON
        val json = JSONObject()
        json.put("login", login)
        json.put("password", password)
        val cuerpo = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$URL_BASE/login")
            .post(cuerpo)
            .build()

        tvEstado.text = "Conectando..."
        btnEntrar.isEnabled = false

        // enqueue: la petición se ejecuta en un hilo de fondo (no bloquea la UI)
        cliente.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                // Sin conexión, IP incorrecta, puerto cerrado, timeout...
                runOnUiThread {
                    tvEstado.text = "Error de conexión: ${e.message}"
                    btnEntrar.isEnabled = true
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val codigo = it.code
                    val texto = it.body?.string() ?: ""

                    // Volver al hilo principal para modificar la interfaz
                    runOnUiThread {
                        btnEntrar.isEnabled = true
                        try {
                            val respuesta = JSONObject(texto)
                            val mensaje = respuesta.optString("mensaje")

                            if (codigo == 200 && respuesta.optBoolean("valido")) {
                                tvEstado.text = "✔ $mensaje"
                                btnPerfil.isEnabled = true
                            } else {
                                tvEstado.text = "✘ $mensaje (código $codigo)"
                                btnPerfil.isEnabled = false
                                limpiarPerfil()
                            }
                        } catch (e: Exception) {
                            tvEstado.text = "Respuesta inválida del servidor (código $codigo)"
                        }
                    }
                }
            }
        })
    }

    // GET /usuario: pide los datos del usuario y los muestra en los TextView
    private fun obtenerPerfil() {
        val request = Request.Builder()
            .url("$URL_BASE/usuario")
            .get()
            .build()

        cliente.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    tvEstado.text = "Error de conexión: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val codigo = it.code
                    val texto = it.body?.string() ?: ""

                    runOnUiThread {
                        if (codigo == 200) {
                            try {
                                val usuario = JSONObject(texto)
                                tvNombre.text = "Nombre: ${usuario.getString("nombre")}"
                                tvCorreo.text = "Correo: ${usuario.getString("correo")}"
                                tvRut.text = "RUT: ${usuario.getString("rut")}"
                            } catch (e: Exception) {
                                tvEstado.text = "Error al leer los datos del usuario"
                            }
                        } else {
                            tvEstado.text = "Error del servidor (código $codigo)"
                        }
                    }
                }
            }
        })
    }

    // Deja los datos del perfil en blanco (por ejemplo, tras un login fallido)
    private fun limpiarPerfil() {
        tvNombre.text = "Nombre: -"
        tvCorreo.text = "Correo: -"
        tvRut.text = "RUT: -"
    }
}