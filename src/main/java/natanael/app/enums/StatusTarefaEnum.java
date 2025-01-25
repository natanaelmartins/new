package natanael.app.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StatusTarefaEnum {
    LISTA_TAREFAS("lista de tarefas "), 
    EM_ANDAMENTO("em andamento "),
    CONCLUIDAS("concluidas ");

    private String descricao;
}