package com.mybutler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class MyButlerApplication

fun main(args: Array<String>) {
    runApplication<MyButlerApplication>(*args)
}
