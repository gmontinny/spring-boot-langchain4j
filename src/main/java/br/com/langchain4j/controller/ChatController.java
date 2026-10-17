package br.com.langchain4j.controller;

import br.com.langchain4j.ai.RestaurantAssistant;
import br.com.langchain4j.payload.request.ChatRequest;
import br.com.langchain4j.payload.response.ChatResponse;
import br.com.langchain4j.payload.response.ErrorMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat com IA (LangChain4j & Google Gemini)", description = "Endpoint de conversa inteligente sobre os dados de restaurantes usando IA e Function Calling Tools")
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RestaurantAssistant restaurantAssistant;

    @Value("${gemini.model-name:gemini-2.5-flash}")
    private String modelName;

    @Operation(summary = "Fazer perguntas em linguagem natural sobre a base de restaurantes",
            description = "Envia uma pergunta para o agente Google Gemini integrado via LangChain4j, que consulta autonomamente os restaurantes no banco PostgreSQL utilizando Tools/Function Calling.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resposta gerada com sucesso pela IA",
                    content = @Content(schema = @Schema(implementation = ChatResponse.class))),
            @ApiResponse(responseCode = "400", description = "Mensagem inválida ou em branco",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Não autorizado / Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "429", description = "Limite de taxa excedido (Rate Limit)")
    })
    @PostMapping
    public ResponseEntity<ChatResponse> askQuestion(@Valid @RequestBody ChatRequest chatRequest) {
        log.info("Recebida pergunta de chat sobre a base de dados: {}", chatRequest.getMessage());

        String aiResponse = restaurantAssistant.chat(chatRequest.getMessage());

        return ResponseEntity.ok(new ChatResponse(aiResponse, modelName));
    }
}
