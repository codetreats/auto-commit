package de.codetreats.autocommit

import de.codetreats.autocommit.http.Server
import java.util.*

const val WEBSERVER = "WEBSERVER"

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))

    val bootstrap = Bootstrap(args)
    if (args[0] == WEBSERVER) {
        Server(8080, bootstrap).start()
    } else {
        bootstrap.committer.commit()
        bootstrap.cleanup.clean()
        bootstrap.repoScanner.scanAllRepos()
    }
}
