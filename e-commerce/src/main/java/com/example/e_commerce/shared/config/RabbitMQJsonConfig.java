package com.example.e_commerce.shared.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Convierte el objeto de java  a un json, para que sea entendible para rabbitmq
 */
@Configuration
public class RabbitMQJsonConfig {

    /**
     * Crea un MessageConverter que serializa/deserializa mensajes como JSON.
     *
     * JacksonJsonMessageConverter() sin argumentos:
     *   Crea su propio JsonMapper internamente (Jackson 3, el que Spring Boot 4
     *   usa bajo el paquete tools.jackson). No necesitamos inyectarlo manualmente.
     *
     * POR QUÉ NO Jackson2JsonMessageConverter:
     *   Ese es el converter de Jackson 2 (com.fasterxml.jackson.databind), que
     *   NO está en el classpath de Spring Boot 4. Solo JacksonJsonMessageConverter
     *   (Jackson 3 / tools.jackson.databind.json.JsonMapper) es compatible aquí.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
