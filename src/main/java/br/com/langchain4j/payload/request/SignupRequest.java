package br.com.langchain4j.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Schema(description = "Payload para registro de novo usuário")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @Schema(description = "Nome de usuário único", example = "gmontinny", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Username é obrigatório")
    @Size(min = 3, max = 50, message = "Username deve ter entre 3 e 50 caracteres")
    private String username;

    @Schema(description = "E-mail válido do usuário", example = "gmontinny@email.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    private String email;

    @Schema(description = "Senha com no mínimo 6 caracteres", example = "Gmontinny2026", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, max = 100, message = "Senha deve ter no mínimo 6 caracteres")
    private String password;

    @Schema(description = "Perfis de acesso associados (ex: [\"user\", \"admin\"])", example = "[\"user\"]")
    private Set<String> roles;
}
