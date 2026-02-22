package de.codetreats.autocommit.http

import de.codetreats.autocommit.Bootstrap
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.io.File
import java.security.KeyStore

class Server(private val port: Int, private val bootstrap: Bootstrap) {
    private val logger = bootstrap.getLogger<Server>()
    private val pageCommit = bootstrap.pageCommit

    fun start() {
        println("Starting server")

        val server = embeddedServer(Netty, port = port) {
                routing {
                    post("/commit") {
                        logger.info("[POST] /commit")
                        val body = call.receiveText()
                        logger.info(body)
                        respondHtml { pageCommit.post(body) }
                    }
                    staticFiles("/images/", File("/src/src/main/resources/images"))
                    staticFiles("/diffs/", File("/var/www/html/diffs"))
                    staticFiles("/style.css", File("/src/src/main/resources/style.css"))
                    staticFiles("/app.js", File("/src/src/main/resources/app.js"))
                    staticFiles("/index.html", File("/src/src/main/resources/index.html"))
                    staticFiles("/", File("/src/src/main/resources/index.html"))
                }
            }
        server.engineConfig.configureSSL()
        server.start(wait = true)
    }

    private suspend fun RoutingContext.respondHtml(prepareText: () -> String) {
        try {
            call.respondText(contentType = ContentType.Text.Html, text = prepareText())
        } catch (e: Exception) {
            call.respondText(contentType = ContentType.Text.Html, text = "<h1>ERROR</h1><pre>${e.stackTraceToString()}</pre>")
        }
    }

    private fun ApplicationEngine.Configuration.configureSSL() {
        val certPassword = System.getenv("CERT_PASS")
        val keyStoreFile = File("/apache/keystore.jks")
        sslConnector(
            keyStore = KeyStore.getInstance(keyStoreFile, certPassword.toCharArray()),
            keyAlias = System.getenv("KEY_ALIAS"),
            keyStorePassword = { certPassword.toCharArray() },
            privateKeyPassword = { certPassword.toCharArray() }
        ) {
            port = 8443
            keyStorePath = keyStoreFile
        }
    }
}