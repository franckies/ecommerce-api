package it.polito.master.ap.group6.ecommerce.warehouseservice.listeners

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class AlarmLevelListener() {
    @KafkaListener(groupId = "ecommerce", topics = ["alarm_level"])
    fun listener(message: String) {
        println(message)
    }
}