package natanael.app.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ConstantesIA {

    PROMPT("Crie um roadmap para {objetivo} em formato JSON, contendo uma lista única chamda 'steps' com os campos 'title', 'description' e 'deadline'. Responda apenas no formato JSON, começando com '{'"),

    OBJETIVO("{objetivo}"),

    GENERATION("generation"),

    STEPS("steps");

    private String descricao;

    public String promptFormatado(String objetivo) {
        return descricao.replace(OBJETIVO.descricao, objetivo);
    }
}