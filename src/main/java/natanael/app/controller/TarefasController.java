package natanael.app.controller;

import java.security.Principal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.bind.annotation.ResponseBody;

import natanael.app.model.UsuariosEntity;
import natanael.app.enums.ConstantesIA;
import natanael.app.enums.StatusTarefaEnum;
import natanael.app.model.MetasEntity;
import natanael.app.model.TarefasEntity;
import natanael.app.repositories.MetasRepository;
import natanael.app.repositories.TarefasRepository;
import natanael.app.repositories.UsuariosRepository;

@Controller
public class TarefasController {

    private final ChatClient chatClient;
    private final UsuariosRepository usuariosRepository;
    private final TarefasRepository tarefasRepository;
    private final MetasRepository metasRepository;


    public TarefasController(ChatClient.Builder builder, UsuariosRepository usuariosRepository,
            TarefasRepository tarefasRepository, MetasRepository metasRepository) {
        this.chatClient = builder.build();
        this.usuariosRepository = usuariosRepository;
        this.tarefasRepository = tarefasRepository;
        this.metasRepository = metasRepository;
    }

    private BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/cadastro")
    public String cadastro() {
        return "cadastro";
    }

    @GetMapping("/editarPerfil")
    public String editarPerfil(ModelMap model, Principal principal) {
        model.addAttribute("username", principal.getName());
        return "editarPerfil";
    }

    @GetMapping("/home/{username}")
    public String home(@PathVariable String username, ModelMap model, Principal principal) {
        if (!principal.getName().equals(username)) {
            return "redirect:/logout";
        }
        List<MetasEntity> listaMetas = metasRepository.findByUsuarioUsername(username);
        List<TarefasEntity> tarefasIminentes = verificarTarefasIminentes(username);
        
        Map<UUID, Double> progressoMetas = new HashMap<>();
        for (MetasEntity meta : listaMetas) {
            List<TarefasEntity> tarefasDaMeta = tarefasRepository.findByMetaId(meta.getId());
            if (!tarefasDaMeta.isEmpty()) {
                long tarefasConcluidas = tarefasDaMeta.stream()
                    .filter(t -> t.getStatus() != null && t.getStatus().trim().equals("concluídas"))
                    .count();
                double progresso = (double) tarefasConcluidas / tarefasDaMeta.size() * 100;
                progressoMetas.put(meta.getId(), progresso);
            } else {
                progressoMetas.put(meta.getId(), 0.0);
            }
        }

        model.addAttribute("listaMetas", listaMetas);
        model.addAttribute("username", username);
        model.addAttribute("tarefasIminentes", tarefasIminentes);
        model.addAttribute("progressoMetas", progressoMetas);
        return "home";
    }

    @GetMapping("/home/tarefas/{username}/{metaId}")
    public String tarefas(@PathVariable String metaId, @PathVariable String username, ModelMap model,
            Principal principal) {
        Optional<MetasEntity> meta = metasRepository.findById(UUID.fromString(metaId));
        if (!principal.getName().equals(username)
                || !meta.get().getUsuario().getUsername().equals(principal.getName())) {
            return "redirect:/logout";
        }
        List<TarefasEntity> listaTarefas = tarefasRepository.findByMetaId(meta.get().getId());
        model.addAttribute("listaTarefas", listaTarefas);
        model.addAttribute("meta", meta.get());
        model.addAttribute("username", username);
        
        // Adiciona a análise ao modelo se existir
        if (meta.get().getAnalise() != null) {
            model.addAttribute("analise", meta.get().getAnalise());
        }
        
        return "tarefas";
    }

    @PostMapping("/salvarCadastro")
    public String salvarCadastro(String username, String email, String password) {
        if (usuariosRepository.findByUsername(username).isPresent()
                || usuariosRepository.findByEmail(email).isPresent()) {
            return "/login"; // mensagem de erro
        }
        UsuariosEntity usuario = new UsuariosEntity();
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder().encode(password));
        usuariosRepository.save(usuario);
        return "/login";
    }

    @PostMapping("/editarCadastro")
    public String editarCadastro(String username, String email, String password, Principal principal) {
        Optional<UsuariosEntity> usuarioAlterado = usuariosRepository.findByUsername(principal.getName());
        if (usuariosRepository.findByUsername(username).isPresent()
                || usuariosRepository.findByEmail(email).isPresent()) {
            return "/login"; // mensagem de erro
        }
        if (!username.isEmpty()) {
            usuarioAlterado.get().setUsername(username);
        }
        if (!email.isEmpty()) {
            usuarioAlterado.get().setEmail(email);
        }
        if (!password.isEmpty()) {
            usuarioAlterado.get().setPassword(passwordEncoder().encode(password));
        }
        usuariosRepository.save(usuarioAlterado.get());
        return "redirect:/home";
    }

    @PostMapping("/adicionarMeta")
    public String adicionarMeta(String meta, Principal principal) {
        UsuariosEntity usuario = usuariosRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        
        MetasEntity novaMeta = new MetasEntity(UUID.randomUUID(), meta, usuario);
        metasRepository.save(novaMeta);
        return "redirect:/home/" + principal.getName();
    }

    @PostMapping("/editarMeta")
    public String editarMeta(String id, String tituloMeta, Principal principal) {
        MetasEntity metaEditada = metasRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("Meta não encontrada"));
        metaEditada.setTituloMeta(tituloMeta);
        metasRepository.save(metaEditada);
        return "redirect:/home/" + principal.getName();
    }

    @PostMapping("/deletarMeta")
    public String deletarMeta(String id) {
        List<TarefasEntity> listaTarefas = tarefasRepository.findByMetaId(UUID.fromString(id));
        tarefasRepository.deleteAll(listaTarefas);
        metasRepository.delete(metasRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("Meta não encontrada")));
        return "redirect:/home";
    }

    @PostMapping("/adicionarTarefa")
    public String adicionarTarefa(String tarefa, String metaId, Principal principal) {
        TarefasEntity novaTarefa = new TarefasEntity(UUID.randomUUID(), tarefa, null, null,
                metasRepository.findById(UUID.fromString(metaId))
                        .orElseThrow(() -> new IllegalArgumentException("Meta não encontrada")),
                StatusTarefaEnum.LISTA_TAREFAS.getDescricao());
        tarefasRepository.save(novaTarefa);
        return "redirect:/home/tarefas/" + principal.getName() + "/" + metaId;
    }

    @PostMapping("/editarTarefa")
    public String editarTarefa(String id, String txt, String descricao, Principal principal, String metaId) {
        TarefasEntity tarefaEditada = tarefasRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada"));
        tarefaEditada.setTaskText(txt);
        tarefaEditada.setDescricao(descricao);
        tarefasRepository.save(tarefaEditada);
        return redirecionarParaPaginaTarefas(principal.getName(), metaId);
    }

    @PostMapping("/editarStatusTarefa")
    public String editarStatusTarefa(String id, String status, String metaId, Principal principal) {
        TarefasEntity tarefa = tarefasRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada"));
        tarefa.setStatus(status);
        tarefasRepository.save(tarefa);
        return "redirect:/home/tarefas/" + principal.getName() + "/" + metaId;
    }

    @PostMapping("/deletarTarefa")
    public String deletarTarefa(String id, String metaId, Principal principal) {
        tarefasRepository.delete(tarefasRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada")));
        return "redirect:/home/tarefas/" + principal.getName() + "/" + metaId;
    }

    @PostMapping("/gerarTarefa")
    public String gerarTarefas(String objetivo, String metaId, Principal principal) {
        try {
            List<TarefasEntity> tarefasGeradas = gerarTarefasDoObjetivo(objetivo, metaId);
            salvarTarefas(tarefasGeradas);
            return redirecionarParaPaginaTarefas(principal.getName(), metaId);
        } catch (Exception e) {
            System.out.println("O erro é: " + e);
            return redirecionarParaPaginaTarefas(principal.getName(), metaId); // Redirect to the current page on error
        }
    }

    @PostMapping("/editarPrazoTarefa")
    public String editarPrazoTarefa(String id, String prazo, String metaId, Principal principal) {
        TarefasEntity tarefa = tarefasRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada"));
        tarefa.setPrazo(Date.valueOf(prazo));
        tarefasRepository.save(tarefa);
        return redirecionarParaPaginaTarefas(principal.getName(), metaId);
    }

    private List<TarefasEntity> gerarTarefasDoObjetivo(String objetivo, String metaId) {
        String respostaJson = obterRespostaIA(objetivo);
        respostaJson = limparResposta(respostaJson);
        if (respostaJson == null || respostaJson.isEmpty() || !isValidJson(respostaJson)) {
            System.out.println("JSON inválido: " + respostaJson + " - O JSON não é válido.");
            return null; // Return null to indicate invalid JSON
        }
        JSONArray listaTarefasJson = extrairListaTarefasJson(respostaJson);
        return converterJsonParaTarefas(listaTarefasJson, metaId);
    }

    private String limparResposta(String resposta) {
        // Implement logic to extract valid JSON from the response
        int startIndex = resposta.indexOf("{");
        int endIndex = resposta.lastIndexOf("}");
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return resposta.substring(startIndex, endIndex + 1);
        }
        return null; // Return null if no valid JSON found
    }

    private boolean isValidJson(String json) {
        try {
            new JSONObject(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String obterRespostaIA(String objetivo) {
        try {
            return chatClient.prompt()
                    .user(ConstantesIA.PROMPT.promptFormatado(objetivo))
                    .call()
                    .content();
        } catch (RestClientException e) {
            System.err.println("Erro na chamada da IA: " + e.getMessage());
            return "{}"; 
        }
    }

    private JSONArray extrairListaTarefasJson(String respostaJson) {
        JSONObject objetoJson = new JSONObject(respostaJson);
        return objetoJson.getJSONArray(ConstantesIA.STEPS.getDescricao());
    }

    private List<TarefasEntity> converterJsonParaTarefas(JSONArray listaTarefasJson, String metaId) {
        List<TarefasEntity> tarefas = new ArrayList<>();
        MetasEntity meta = metasRepository.findById(UUID.fromString(metaId))
                .orElseThrow(() -> new IllegalArgumentException("Meta não encontrada"));

        for (int i = 0; i < listaTarefasJson.length(); i++) {
            JSONObject elemento = listaTarefasJson.getJSONObject(i);
            TarefasEntity tarefa = criarTarefa(elemento.getString("title"),
                    elemento.getString("description"),
                    meta);
            tarefas.add(tarefa);
        }

        return tarefas;
    }

    private TarefasEntity criarTarefa(String titulo, String descricao, MetasEntity meta) {
        return new TarefasEntity(UUID.randomUUID(),
                titulo,
                descricao,
                null,
                meta,
                StatusTarefaEnum.LISTA_TAREFAS.getDescricao());
    }

    private void salvarTarefas(List<TarefasEntity> tarefas) {
        tarefasRepository.saveAll(tarefas);
    }

    private String redirecionarParaPaginaTarefas(String username, String metaId) {
        return "redirect:/home/tarefas/" + username + "/" + metaId;
    }

    private List<TarefasEntity> verificarTarefasIminentes(String username) {
        LocalDate hoje = LocalDate.now();
        LocalDate tresDiasDepois = hoje.plusDays(3);
        
        return tarefasRepository.findByMetaUsuarioUsernameAndPrazoBetween(
            username,
            Date.from(hoje.atStartOfDay(ZoneId.systemDefault()).toInstant()),
            Date.from(tresDiasDepois.atStartOfDay(ZoneId.systemDefault()).toInstant())
        );
    }

    @GetMapping("/analisarTarefas/{metaId}")
    @ResponseBody
    public Map<String, String> analisarTarefas(@PathVariable String metaId) {
        MetasEntity meta = metasRepository.findById(UUID.fromString(metaId))
                .orElseThrow(() -> new IllegalArgumentException("Meta não encontrada"));
                
        List<TarefasEntity> tarefas = tarefasRepository.findByMetaId(meta.getId());
                
        // Se já existe análise salva, retorna ela
        if (meta.getAnalise() != null) {
            return Map.of("analysis", meta.getAnalise());
        }
                
        // Create a prompt with all tasks
        StringBuilder prompt = new StringBuilder();
        prompt.append("Em um parágrafo, analise as seguintes tarefas e forneça uma orientação sobre o que o usuário deve focar. Fale de forma informal como se tivesse dando um conselho para um alguém sem tempo:\n\n");
                
        for (TarefasEntity tarefa : tarefas) {
            prompt.append("- ").append(tarefa.getTaskText()).append("\n");
        }
                
        // Get AI response
        String analysis = chatClient.prompt()
                .user(prompt.toString())
                .call()
                .content();
                
        // Salva a análise
        meta.setAnalise(analysis);
        metasRepository.save(meta);
                
        return Map.of("analysis", analysis);
    }
}