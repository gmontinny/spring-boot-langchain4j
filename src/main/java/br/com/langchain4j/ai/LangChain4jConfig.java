package br.com.langchain4j.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class LangChain4jConfig {

    @Value("${gemini.api-key:AIzaSyDjvSqrrGcA7iwNIfkBz_B9vshmtbmEVFo}")
    private String apiKey;

    @Value("${gemini.model-name:gemini-2.5-flash}")
    private String modelName;

    @Value("${gemini.temperature:0.3}")
    private Double temperature;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("Inicializando Google AI Gemini Chat Model com modelo: {}", modelName);
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .build();
    }

    @Bean
    public RestaurantAssistant restaurantAssistant(ChatLanguageModel chatLanguageModel, RestaurantTools restaurantTools) {
        return AiServices.builder(RestaurantAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(restaurantTools)
                .build();
    }
}
