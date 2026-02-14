package com.synchtask

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.synchtask"])
@ConfigurationPropertiesScan
class SynchTaskApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<SynchTaskApplication>(*args)
}
