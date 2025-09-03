package com.example.http_server_andoid

import android.os.Bundle
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
        const val PORT = 8099

        const val SEPARADOR_DIA: String = "-"
        const val SEPARADOR_JUGADAS: String = ","

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
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                    <title>Number Processor</title>
                    <style>
                        * {
                            margin: 0;
                            padding: 0;
                            box-sizing: border-box;
                        }
                        
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            min-height: 100vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            padding: 20px;
                        }
                        
                        .container {
                            background: rgba(255, 255, 255, 0.95);
                            backdrop-filter: blur(10px);
                            border-radius: 20px;
                            padding: 30px;
                            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
                            width: 100%;
                            max-width: 400px;
                            text-align: center;
                        }
                        
                        .title {
                            color: #333;
                            font-size: 24px;
                            font-weight: 600;
                            margin-bottom: 20px;
                            letter-spacing: -0.5px;
                        }
                        
                        .form-group {
                            margin-bottom: 25px;
                        }
                        
                        .textarea-container {
                            position: relative;
                        }
                        
                        textarea {
                            width: 100%;
                            min-height: 200px;
                            padding: 15px;
                            border: 2px solid #e1e5e9;
                            border-radius: 12px;
                            font-size: 16px;
                            font-family: inherit;
                            resize: vertical;
                            transition: all 0.3s ease;
                            background: #fff;
                            line-height: 1.5;
                        }
                        
                        textarea:focus {
                            outline: none;
                            border-color: #667eea;
                            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
                        }
                        
                        .submit-btn {
                            width: 100%;
                            padding: 16px;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            border: none;
                            border-radius: 12px;
                            font-size: 18px;
                            font-weight: 600;
                            cursor: pointer;
                            transition: all 0.3s ease;
                            letter-spacing: 0.5px;
                        }
                        
                        .submit-btn:hover {
                            transform: translateY(-2px);
                            box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
                        }
                        
                        .submit-btn:active {
                            transform: translateY(0);
                        }
                        
                        .placeholder-text {
                            color: #999;
                            font-size: 14px;
                            margin-top: 8px;
                            line-height: 1.4;
                        }
                        
                        @media (max-width: 480px) {
                            .container {
                                padding: 20px;
                                margin: 10px;
                            }
                            
                            .title {
                                font-size: 20px;
                            }
                            
                            textarea {
                                min-height: 150px;
                                font-size: 16px;
                            }
                            
                            .submit-btn {
                                font-size: 16px;
                                padding: 14px;
                            }
                        }
                        
                        @media (max-width: 375px) {
                            .container {
                                padding: 15px;
                            }
                            
                            .title {
                                font-size: 18px;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1 class="title">Number Processor</h1>
                        <form method="post">
                            <div class="form-group">
                                <div class="textarea-container">
                                    <textarea name="numbers" id="numbers" placeholder="Enter your numbers here..."></textarea>
                                </div>
                                <div class="placeholder-text">
                                    Enter numbers separated by commas or line breaks
                             </div>
                              </div>
                            <button type="submit" class="submit-btn">Process Numbers</button>
                            </form>
                    </div>
                </body>
                </html>
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

            val validacionInput: MensajeValidacion = RuletaValidador.validarStringJugadas(numbers)
            if (validacionInput.isValido()) {
                val jugadas = numbers

                if (jugadas != "") {
                    val ruleta = Ruleta()
                    val resultado: String = ruleta.calcularResultado(jugadas)

                    call.respondText("""
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                            <style>
                                body {
                                    margin: 0;
                                    padding: 20px 20px 100px 20px;
                                    font-family: Arial, sans-serif;
                                    min-height: 100vh;
                                    box-sizing: border-box;
                                }
                                .container {
                                    max-width: 100%;
                                }
                                .result {
                                    font-size: 16px;
                                    margin: 20px 0;
                                    word-wrap: break-word;
                                    text-align: left;
                                }
                                .button {
                                    display: inline-block;
                                    padding: 12px 24px;
                                    background-color: #007bff;
                                    color: white;
                                    text-decoration: none;
                                    border-radius: 5px;
                                    font-size: 16px;
                                    margin-top: 20px;
                                }
                                .button-container {
                                    text-align: center;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <p class="result">$resultado</p>
                                <div class="button-container">
                                    <a href="/" class="button">Process More Numbers</a>
                                </div>
                            </div>
                        </body>
                        </html>
                    """.trimIndent(), ContentType.Text.Html)
                }
            } else {
                call.respondText("""
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                        <style>
                            body {
                                margin: 0;
                                padding: 20px 20px 100px 20px;
                                font-family: Arial, sans-serif;
                                min-height: 100vh;
                                box-sizing: border-box;
                            }
                            .container {
                                max-width: 100%;
                            }
                            .error {
                                font-size: 18px;
                                margin: 20px 0;
                                color: red;
                                word-wrap: break-word;
                                text-align: left;
                            }
                            .button {
                                display: inline-block;
                                padding: 12px 24px;
                                background-color: #007bff;
                                color: white;
                                text-decoration: none;
                                border-radius: 5px;
                                font-size: 16px;
                                margin-top: 20px;
                            }
                            .button-container {
                                text-align: center;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <h2>Error:</h2>
                            <p class="error">${validacionInput.mensaje}</p>
                            <div class="button-container">
                                <a href="/" class="button">Try Again</a>
                            </div>
                        </div>
                    </body>
                    </html>
                """.trimIndent(), ContentType.Text.Html)
            }
        }

    
}