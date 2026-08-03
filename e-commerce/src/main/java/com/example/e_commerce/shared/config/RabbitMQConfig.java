package com.example.e_commerce.shared.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ: define los elementos de mensajería que usaremos.
 *
 *
 *   DIRECT EXCHANGE:
 *     El exchange compara la routing key del mensaje EXACTAMENTE con la routing key
 *     del binding. Si coinciden → el mensaje va a esa cola. Es como un buzón con
 *     casillas numeradas: el número del mensaje决定了dónde cae.
 *
 *   DEAD LETTER EXCHANGE (DLX):
 *     Es un "exchange de respaldo". Cuando un mensaje falla (el consumidor lo rechaza
 *     con basicNack o se exceden los reintentos), RabbitMQ lo envía al DLX en vez de
 *     eliminarlo. Esto permite revisar mensajes fallidos después.
 *
 * FLUJO VISUAL:
 *
 *   [Productor] ──mensaje──→ [claim.exchange] ──routing key "claim.status.changed"──→ [claim.status.queue]
 *                                                                                    │
 *                                                                              [Consumidor lee]
 *                                                                                    │
 *                                                                              [Si falla 3 veces]
 *                                                                                    ↓
 *   [claim.dlx] ──→ [claim.dlq] (cola de mensajes fallidos para revisión manual)
 */
@Configuration
public class RabbitMQConfig {

    /** Nombre del exchange principal : Decide donde se guarda la informacion (MENSAJERO) */
    public static final String CLAIM_EXCHANGE = "claim.exchange";

    /** Nombre de la cola: Donde se guarda la informacion (BUZON) */
    public static final String CLAIM_STATUS_QUEUE = "claim.status.queue";

    /** Routing key: la "dirección" que identifica de almacenamiento (UBICACION DEL BUZON) */
    public static final String CLAIM_STATUS_ROUTING_KEY = "claim.status.changed";

    /** Nombre del exchange de Dead Letter (respaldo para mensajes fallidos) */
    public static final String DEAD_LETTER_EXCHANGE = "claim.dlx";

    /** Nombre de la cola de Dead Letter (mensajes que fallaron 3 veces) */
    public static final String DEAD_LETTER_QUEUE = "claim.dlq";


    /**
     * DirectExchange: enruta mensajes usando routing key exacta.
     */
    @Bean
    public DirectExchange claimExchange() {
        return new DirectExchange(CLAIM_EXCHANGE);
    }

    // ──────────────────────────────────────────────────────────────
    // COLA PRINCIPAL
    // ──────────────────────────────────────────────────────────────

    /**
     * Cola durable: persiste mensajes incluso si RabbitMQ se reinicia.
     *
     * .durable(CLAIM_STATUS_QUEUE) → la cola sobrevive reinicios del broker
     *
     * .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE) →
     *   Cuando un mensaje se rechaza (con basicNack y requeue=false),
     *   RabbitMQ NO lo elimina. Lo envía al Dead Letter Exchange definido aquí.
     *   Esto es como una "bandeja de salida" para mensajes que no pudieron
     *   procesarse.
     */
    @Bean
    public Queue claimStatusQueue() {
        return QueueBuilder.durable(CLAIM_STATUS_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .build();
    }

    /**
     * Binding: conecta el exchange con la cola usando una routing key.
        conecta el Exchage con la queue atraves de la routingkey
     */
    @Bean
    public Binding claimStatusBinding(Queue claimStatusQueue, DirectExchange claimExchange) {
        return BindingBuilder
                .bind(claimStatusQueue)       // cola destino
                .to(claimExchange)            // exchange origen
                .with(CLAIM_STATUS_ROUTING_KEY); // routing key
    }

    // ──────────────────────────────────────────────────────────────
    // DEAD LETTER EXCHANGE (DLX) Y COLA (DLQ)
    // ──────────────────────────────────────────────────────────────

    /**
     * Dead Letter Exchange: exchange de respaldo para mensajes fallidos.
     *
     * Cuando un consumidor hace basicNack(requeue=false) o un mensaje
     * excede el TTL de la cola, RabbitMQ lo envía a este exchange.
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    /**
     * Cola de Dead Letter: donde terminan los mensajes que fallaron.
     *
     * Un desarrollador o administrador puede revisar esta cola después,
     * ver qué mensajes fallaron y por qué, y reintentarlos manualmente
     * o corregir el problema.
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    /**
     * Binding del DLX → DLQ: conecta el exchange de respaldo con su cola.
     *
     * Routing key vacía ("") porque el DLX solo tiene un destino.
     * Todos los mensajes que caigan al DLX van a esta cola.
     */
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder
                .bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(""); // routing key vacía: todo va a esta cola
    }
}
