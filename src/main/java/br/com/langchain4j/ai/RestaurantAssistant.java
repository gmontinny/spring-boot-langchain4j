package br.com.langchain4j.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface RestaurantAssistant {

    @SystemMessage({
            "Você é um assistente especialista e amigável sobre a base de dados de restaurantes do Zomato.",
            "Você tem acesso a ferramentas para consultar os dados reais de restaurantes diretamente do banco de dados.",
            "Sempre utilize as ferramentas disponíveis quando precisar pesquisar restaurantes por cidade, culinária, melhores notas ou estatísticas.",
            "Forneça respostas claras, elegantes e úteis em português (ou no idioma solicitado pelo usuário)."
    })
    String chat(@UserMessage String userMessage);
}
