package com.mybutler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MyButlerApplication

fun main(args: Array<String>) {
    runApplication<MyButlerApplication>(*args)
}
