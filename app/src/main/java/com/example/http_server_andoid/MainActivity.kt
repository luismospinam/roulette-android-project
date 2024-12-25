package com.example.http_server_andoid

import android.os.Bundle
import android.text.Html
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.http_server_andoid.logic.Ruleta
import com.example.http_server_andoid.logic.RuletaValidador
import com.example.http_server_andoid.model.MensajeValidacion
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.pipeline.PipelineContext


class MainActivity : AppCompatActivity() {
    companion object {
        const val PORT = 8099;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        createHttpServer()
    }

    private fun createHttpServer() {
        embeddedServer(Netty, PORT) {
            install(ContentNegotiation) {
                gson {}
            }
            routing {
                route("/") {
                    get(startHtmlPage())
                    post(processPostRequest())
                }
            }
        }.start(wait = true)
    }

    private fun startHtmlPage(): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit =
        {
            call.respondText(
                """
                            <form method="post">
                             <div>
                                <label for="say"> Ingrese los numeros </label>
                                <textarea name="numbers" id="numbers" value="Hi" rows="10"></textarea>
                             </div>
    
                              <div>
                                <input type="submit" value="Send">
                              </div>
                            </form>
                        """.trimIndent(), ContentType.Text.Html
            )
        }

    private fun processPostRequest(): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit =
        {
            val inputText = call.receiveText()
            val numbers = inputText
                .replace("numbers=", "")
                .replace("%2C", ",")
                .replace("%0D%0A", "")

            val validacionHoy: MensajeValidacion = RuletaValidador.validarStringJugadas(numbers)
            if (validacionHoy.isValido()) {
                val jugadas = numbers

                if (jugadas != "") {
                    val ruleta = Ruleta()
                    val resultado: String = ruleta.calcularResultado(jugadas)

                    call.respondText(resultado, ContentType.Text.Html)
                }
            } else {
                call.respondText(validacionHoy.mensaje, ContentType.Text.Html)
            }
        }
}