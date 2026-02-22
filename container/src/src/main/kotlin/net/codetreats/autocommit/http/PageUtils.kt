package de.codetreats.autocommit.http

import java.time.format.DateTimeFormatter

class PageUtils() {
    val germanDateTime = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    fun result(title: String, head: String = "", body: String = "") = """
        <!doctype html>
        <html>
          <head>
            <title>$title</title>
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@4.0.0/dist/css/bootstrap.min.css" integrity="sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm" crossorigin="anonymous">
            <link rel='stylesheet' type='text/css' href='/style.css'>
            <meta charset="utf-8">
            <meta http-equiv='cache-control' content='max-age=0' />
            <meta http-equiv='cache-control' content='no-cache' />
            <meta http-equiv='cache-control' content='no-store' />
            <meta http-equiv='expires' content='0' />
            <meta http-equiv='expires' content='Tue, 01 Jan 1980 1:00:00 GMT' />
            <meta http-equiv='pragma' content='no-cache' />
            $head
          </head>
          <body>
          $body
          </body>
        </html>
    """.trimIndent()

    fun back(href: String = "/") = """<a class='btn btn-info btn-lg back' href='$href'><img class="button-image back-image" src="/images/back.svg"></a>"""

    fun button(href: String, text: String, selected: Boolean = false) = if (selected) {
        """<a class='btn btn-info btn-lg' href='$href'>$text</a>"""
    } else {
        """<a class='btn btn-secondary btn-lg' href='$href'>$text</a>"""
    }
}