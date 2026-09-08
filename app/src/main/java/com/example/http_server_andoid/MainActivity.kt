package com.example.http_server_andoid

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.http_server_andoid.logic.Ruleta
import com.example.http_server_andoid.logic.RuletaSnapshot
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

    /**
     * Wraps both parts of the cache as a single atomic reference so that
     * concurrent requests never see a mismatched key+snapshot pair.
     */
    private data class SnapshotEntry(val key: String, val snapshot: RuletaSnapshot)

    @Volatile
    private var snapshotEntry: SnapshotEntry? = null

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
                    // ── Two-phase cache: skip re-processing historical sessions ──
                    val days = jugadas.split(SEPARADOR_DIA)
                    val histKey = if (days.size > 1) days.dropLast(1).joinToString(SEPARADOR_DIA) else ""
                    val entry = snapshotEntry  // single atomic read

                    val ruleta = Ruleta()
                    val resultado: String
                    if (histKey.isNotEmpty() && entry != null && entry.key == histKey) {
                        // Cache hit: restore from snapshot, process only today's session
                        resultado = ruleta.calcularResultadoConHistorial(entry.snapshot, jugadas)
                    } else {
                        // Cache miss: full processing
                        resultado = ruleta.calcularResultado(jugadas)
                        // Save snapshot of historical sessions for next request
                        if (histKey.isNotEmpty()) {
                            val snap = ruleta.lastHistoricalSnapshot
                            if (snap != null) {
                                snapshotEntry = SnapshotEntry(histKey, snap)
                            }
                        }
                    }

                    call.respondText("""
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                            <style>
                                * { margin: 0; padding: 0; box-sizing: border-box; }

                                body {
                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
                                                 Oxygen, Ubuntu, Cantarell, sans-serif;
                                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                                    min-height: 100vh;
                                    padding: 20px 20px 80px 20px;
                                }

                                .page-wrapper {
                                    max-width: 720px;
                                    margin: 0 auto;
                                }

                                .card {
                                    background: rgba(255, 255, 255, 0.95);
                                    border-radius: 16px;
                                    padding: 20px 24px;
                                    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
                                    margin-bottom: 16px;
                                }

                                .card-header {
                                    font-size: 13px;
                                    font-weight: 700;
                                    text-transform: uppercase;
                                    letter-spacing: 0.08em;
                                    color: #764ba2;
                                    border-bottom: 2px solid #ede9f7;
                                    padding-bottom: 8px;
                                    margin-bottom: 14px;
                                }

                                .total-bolas {
                                    font-size: 18px;
                                    font-weight: 600;
                                    color: #333;
                                }
                                .total-bolas span {
                                    color: #764ba2;
                                    font-size: 26px;
                                }

                                .num-highlight {
                                    color: #ff3100;
                                    font-weight: 700;
                                }

                                .freq-row {
                                    display: flex;
                                    align-items: baseline;
                                    gap: 10px;
                                    padding: 4px 0;
                                    border-bottom: 1px solid #f3f0fa;
                                    font-size: 15px;
                                }
                                .freq-row:last-child { border-bottom: none; }
                                .freq-label {
                                    color: #888;
                                    font-size: 13px;
                                    min-width: 90px;
                                    flex-shrink: 0;
                                }
                                .freq-acum {
                                    margin-left: auto;
                                    font-size: 12px;
                                    font-weight: 600;
                                    color: #764ba2;
                                    flex-shrink: 0;
                                }

                                .seq-key {
                                    font-weight: 600;
                                    color: #555;
                                    font-size: 14px;
                                    margin-bottom: 4px;
                                }
                                .seq-values {
                                    padding-left: 12px;
                                    padding-bottom: 6px;
                                    font-size: 15px;
                                }

                                .hoy-row {
                                    display: flex;
                                    justify-content: space-between;
                                    align-items: baseline;
                                    padding: 3px 0;
                                    font-size: 15px;
                                    border-bottom: 1px solid #f3f0fa;
                                }
                                .hoy-row:last-child { border-bottom: none; }

                                .resumen-box {
                                    background: linear-gradient(135deg, #fff4f2 0%, #ffe8e3 100%);
                                    border: 2px solid #ff3100;
                                    border-radius: 14px;
                                    padding: 20px 24px;
                                    margin-bottom: 16px;
                                    text-align: center;
                                }
                                .resumen-box .card-header {
                                    color: #ff3100;
                                    border-bottom-color: #ffcfc6;
                                }
                                .resumen-numbers {
                                    display: flex;
                                    flex-wrap: wrap;
                                    gap: 8px;
                                    justify-content: center;
                                    padding-top: 4px;
                                }
                                .resumen-chip {
                                    display: inline-flex;
                                    align-items: center;
                                    justify-content: center;
                                    background: #ff3100;
                                    color: white;
                                    border-radius: 10px;
                                    padding: 8px 16px;
                                    font-size: 22px;
                                    font-weight: 700;
                                    min-width: 52px;
                                    line-height: 1;
                                }

                                .stats-table {
                                    width: 100%;
                                    border-collapse: collapse;
                                    font-size: 14px;
                                }
                                .stats-table th {
                                    background: #f0ecfa;
                                    color: #764ba2;
                                    font-weight: 700;
                                    padding: 8px 10px;
                                    text-align: left;
                                    font-size: 12px;
                                    text-transform: uppercase;
                                    letter-spacing: 0.06em;
                                }
                                .stats-table th:nth-child(2),
                                .stats-table th:nth-child(3) { text-align: right; }
                                .stats-table td {
                                    padding: 7px 10px;
                                    border-bottom: 1px solid #f3f0fa;
                                    color: #333;
                                }
                                .stats-table td:nth-child(2),
                                .stats-table td:nth-child(3) { text-align: right; }
                                .stats-table tr:last-child td { border-bottom: none; }
                                .stats-table tr:nth-child(even) td { background: #faf9fd; }
                                .num-cell {
                                    font-weight: 700;
                                    font-size: 16px;
                                    width: 48px;
                                }

                                .stat-grid {
                                    display: grid;
                                    grid-template-columns: 1fr 1fr;
                                    gap: 10px;
                                }
                                .stat-item-label {
                                    font-size: 13px;
                                    font-weight: 600;
                                    color: #555;
                                    margin-bottom: 4px;
                                    display: flex;
                                    justify-content: space-between;
                                }
                                .stat-item-label span { color: #764ba2; font-weight: 700; }
                                .progress-track {
                                    background: #ede9f7;
                                    border-radius: 99px;
                                    height: 10px;
                                    overflow: hidden;
                                }
                                .progress-fill {
                                    height: 100%;
                                    border-radius: 99px;
                                    background: linear-gradient(90deg, #667eea, #764ba2);
                                }
                                .progress-fill.rojo  { background: linear-gradient(90deg, #ff6b6b, #ff3100); }
                                .progress-fill.negro { background: linear-gradient(90deg, #555, #222); }
                                .progress-fill.par   { background: linear-gradient(90deg, #43b89c, #2d8c6e); }
                                .progress-fill.impar { background: linear-gradient(90deg, #f7a44a, #e07b1a); }

                                .modulo-table {
                                    width: 100%;
                                    border-collapse: collapse;
                                    font-size: 14px;
                                    margin-bottom: 8px;
                                }
                                .modulo-table th {
                                    background: #f0ecfa;
                                    color: #764ba2;
                                    font-weight: 700;
                                    padding: 7px 10px;
                                    text-align: left;
                                    font-size: 12px;
                                    text-transform: uppercase;
                                    letter-spacing: 0.06em;
                                }
                                .modulo-table th:nth-child(2),
                                .modulo-table th:nth-child(3),
                                .modulo-table th:nth-child(4) { text-align: right; }
                                .modulo-table td {
                                    padding: 6px 10px;
                                    border-bottom: 1px solid #f3f0fa;
                                }
                                .modulo-table td:nth-child(2),
                                .modulo-table td:nth-child(3),
                                .modulo-table td:nth-child(4) { text-align: right; }
                                .modulo-table tr:last-child td { border-bottom: none; }

                                .callout-box {
                                    background: linear-gradient(135deg, #f0fff4 0%, #d4f7e2 100%);
                                    border: 2px solid #2d8c6e;
                                    border-radius: 14px;
                                    padding: 20px 24px;
                                    margin-bottom: 16px;
                                }
                                .callout-box .card-header {
                                    color: #2d8c6e;
                                    border-bottom-color: #b2ead4;
                                }
                                .callout-profit {
                                    font-size: 18px;
                                    font-weight: 600;
                                    color: #1a5e47;
                                    line-height: 1.8;
                                }
                                .profit-number {
                                    font-size: 24px;
                                    font-weight: 700;
                                    color: #2d8c6e;
                                }

                                .btn-back {
                                    display: block;
                                    width: 100%;
                                    max-width: 320px;
                                    margin: 8px auto 0;
                                    padding: 16px;
                                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                                    color: white;
                                    text-decoration: none;
                                    border-radius: 12px;
                                    font-size: 18px;
                                    font-weight: 600;
                                    text-align: center;
                                    letter-spacing: 0.5px;
                                    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
                                }
                                .btn-back:hover {
                                    box-shadow: 0 10px 20px rgba(102, 126, 234, 0.4);
                                }

                                .callout-gold {
                                    background: linear-gradient(135deg, #fffdf0 0%, #fff8d4 100%);
                                    border: 2px solid #b8860b;
                                    border-radius: 14px;
                                    padding: 20px 24px;
                                    margin-bottom: 16px;
                                }
                                .callout-gold .card-header {
                                    color: #b8860b;
                                    border-bottom-color: #ffe9a0;
                                }
                                .signal-badge {
                                    display: inline-block;
                                    font-size: 10px;
                                    font-weight: 700;
                                    background: #764ba2;
                                    color: white;
                                    border-radius: 4px;
                                    padding: 1px 5px;
                                    margin: 1px;
                                    letter-spacing: 0.04em;
                                }

                                @media (max-width: 480px) {
                                    .stat-grid { grid-template-columns: 1fr; }
                                    .resumen-chip { font-size: 18px; padding: 7px 12px; min-width: 44px; }
                                    .card { padding: 16px; }
                                    .modulo-table th,
                                    .modulo-table td { padding: 5px 5px; font-size: 12px; }
                                }
                            </style>
                        </head>
                        <body>
                            <div class="page-wrapper">
                                $resultado
                                <a href="/" class="btn-back">Procesar mas numeros</a>
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