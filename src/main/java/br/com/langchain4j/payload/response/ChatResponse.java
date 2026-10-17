package br.com.langchain4j.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String response;
    private String model;
    @Builder.Default
    private Instant timestamp = Instant.now();

    public ChatResponse(String response, String model) {
        this.response = response;
        this.model = model;
        this.timestamp = Instant.now();
    }
}
