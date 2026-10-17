package br.com.langchain4j.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Payload para autenticação de usuário")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Schema(description = "Nome de usuário", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Username é obrigatório")
    private String username;

    @Schema(description = "Senha de acesso", example = "admin123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password é obrigatório")
    private String password;
}
