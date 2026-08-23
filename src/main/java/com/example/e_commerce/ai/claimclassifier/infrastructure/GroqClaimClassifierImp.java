package com.example.e_commerce.ai.claimclassifier.infrastructure;

import com.example.e_commerce.ai.claimclassifier.domain.model.ClaimAiMetadata;
import com.example.e_commerce.ai.claimclassifier.domain.repository.ClaimClassifier;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GroqClaimClassifierImp implements ClaimClassifier {

    private static final String CLASSIFICATION_TEMPLATE = """
            Eres un analista de reclamos de e-commerce.

            Analiza la descripción del reclamo del cliente y devuelve SOLO un JSON
            con exactamente estas claves:
            - "category": categoría de negocio corta (ej: PAYMENT, SHIPPING, PRODUCT_QUALITY, ACCOUNT, OTHER)
            - "urgency": una de LOW, MEDIUM, HIGH o CRITICAL
            - "summary": resumen de una frase (máximo 140 caracteres)

            Descripción del reclamo:
            {description}
            """;

    private final ChatClient chatClient;
    private final StructuredOutputConverter<ClaimAiMetadata> outputConverter;

    public GroqClaimClassifierImp(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build(); // Permite hacer el llamado al modelo
        this.outputConverter = new BeanOutputConverter<>(ClaimAiMetadata.class); // convierte la respuesa a Json
    }

    @Override
    public ClaimAiMetadata classify(String description) {

        // Permite estructurar el prompt uniendo todo
        Prompt prompt = new PromptTemplate(CLASSIFICATION_TEMPLATE)
                .create(Map.of("description", description));

        return chatClient.prompt(prompt)
                .call() // Ejecuta la peticion al proveedor de del modelo de ia
                .entity(outputConverter); // Toma la respuesta de la ia para pasarla al formato Json deseado
    }
}
