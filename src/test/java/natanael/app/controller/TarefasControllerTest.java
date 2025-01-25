package natanael.app.controller;

import natanael.app.enums.ConstantesIA;
import natanael.app.enums.StatusTarefaEnum;
import natanael.app.model.MetasEntity;
import natanael.app.model.TarefasEntity;
import natanael.app.repositories.MetasRepository;
import natanael.app.repositories.TarefasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TarefasControllerTest {

    @InjectMocks
    private TarefasController tarefasController;

    @Mock
    private ChatClient chatClient;

    @Mock
    private MetasRepository metasRepository;

    @Mock
    private TarefasRepository tarefasRepository;

    @Mock
    private Principal principal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGerarTarefas() {
        // Arrange
        String objetivo = "Aprender Java";
        String metaId = UUID.randomUUID().toString();
        String username = "testUser";
        MetasEntity meta = new MetasEntity(UUID.fromString(metaId), "Meta Test", null);

        String iaResponse = "{\"" + ConstantesIA.STEPS.getDescricao() + "\": [" +
                "{\"title\": \"Tarefa 1\", \"description\": \"Descrição 1\"}," +
                "{\"title\": \"Tarefa 2\", \"description\": \"Descrição 2\"}" +
                "]}";

        when(principal.getName()).thenReturn(username);
        when(metasRepository.findById(any(UUID.class))).thenReturn(Optional.of(meta));
        when(chatClient.prompt()).thenReturn(mock(ChatClient.ChatClientRequest.class));
        when(chatClient.prompt().user(anyString())).thenReturn(mock(ChatClient.ChatClientRequest.class));
        when(chatClient.prompt().user(anyString()).call()).thenReturn(mock(ChatClient.ChatClientRequest.CallResponseSpec.class));
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn(iaResponse);

        // Act
        String result = tarefasController.gerarTarefas(objetivo, metaId, principal);

        // Assert
        verify(tarefasRepository, times(1)).saveAll(anyList());
        assertEquals("redirect:/home/tarefas/" + username + "/" + metaId, result);

        verify(metasRepository, times(1)).findById(UUID.fromString(metaId));
        verify(chatClient, times(4)).prompt();
        verify(tarefasRepository, times(1)).saveAll(argThat((List<TarefasEntity> tarefas) -> {
            assertEquals(2, tarefas.size());
            assertEquals("Tarefa 1", tarefas.get(0).getTaskText());
            assertEquals("Descrição 1", tarefas.get(0).getDescricao());
            assertEquals(StatusTarefaEnum.LISTA_TAREFAS.getDescricao(), tarefas.get(0).getStatus());
            assertEquals("Tarefa 2", tarefas.get(1).getTaskText());
            assertEquals("Descrição 2", tarefas.get(1).getDescricao());
            assertEquals(StatusTarefaEnum.LISTA_TAREFAS.getDescricao(), tarefas.get(1).getStatus());
            return true;
        }));
    }
}