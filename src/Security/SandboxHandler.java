package Security;

import Data.JsonBuilder;
import Http.HttpResponse;
import Http.HttpRequest;
import Persistencia.ConexaoDB;

public class SandboxHandler {

    public static HttpResponse handle(HttpRequest req) {
        User user = req.getUser();
        if (user == null || user.getCargo() != Role.ADMIN) {
            return new HttpResponse().status(Http.HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Não autorizado."));
        }

        try {
            String corpo = req.getCorpo();
            java.util.Map<String, Object> map = Data.JsonParser.parse(corpo);
            String comandoStr = Data.JsonParser.getString(map, "comando");
            if (comandoStr == null || comandoStr.isBlank()) {
                return HttpResponse.requisicaoInvalida("Comando não fornecido.");
            }

            String comando = comandoStr.trim().toLowerCase();
            String output = "";

            switch (comando) {
                case "help":
                    output = "Comandos disponíveis:\\n" +
                             "  status      Exibe o status do servidor Java\\n" +
                             "  db-status   Exibe as estatísticas de conexão do PostgreSQL\\n" +
                             "  users       Lista a contagem de usuários cadastrados\\n" +
                             "  storage     Mostra o uso do diretório de workspace\\n" +
                             "  clear       Limpa o console";
                    break;
                case "status":
                    long memoriaUsada = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                    output = "Servidor Vaultra HTTP ativo.\\n" +
                             "Versão do Java: " + System.getProperty("java.version") + "\\n" +
                             "Memória alocada (Heap): " + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + " MB\\n" +
                             "Memória em uso: " + (memoriaUsada / 1024 / 1024) + " MB";
                    break;
                case "db-status":
                    boolean ok = ConexaoDB.isDisponivel();
                    output = "Banco de Dados: PostgreSQL\\n" +
                             "Status: " + (ok ? "ONLINE" : "OFFLINE") + "\\n" +
                             "Pool de conexões configurado: 5 conexões";
                    break;
                case "users":
                    int count = Persistencia.GerenciadorEntidade.buscarTodos(Security.User.class).size();
                    output = "Sistema de autenticação (JWT) ativo e seguro.\\n" +
                             "Total de usuários registrados no banco de dados: " + count;
                    break;
                case "storage":
                    java.io.File pasta = new java.io.File("public/Workspace");
                    long size = calcularTamanhoPasta(pasta);
                    output = "Armazenamento do Workspace Vaultra:\\n" +
                             "Caminho: " + pasta.getAbsolutePath() + "\\n" +
                             "Tamanho total: " + (size / 1024) + " KB";
                    break;
                default:
                    output = "Comando não reconhecido: " + comando + "\\nDigite 'help' para a lista de comandos.";
            }

            String json = new JsonBuilder()
                    .add("status", "success")
                    .add("output", output)
                    .build();
            return HttpResponse.ok().json(json);

        } catch (Exception e) {
            String erroJson = new JsonBuilder()
                    .add("status", "error")
                    .add("error", "Erro interno no sandbox: " + e.getMessage())
                    .build();
            return HttpResponse.ok().json(erroJson);
        }
    }

    private static long calcularTamanhoPasta(java.io.File directory) {
        long length = 0;
        if (directory.listFiles() != null) {
            for (java.io.File file : directory.listFiles()) {
                if (file.isFile())
                    length += file.length();
                else
                    length += calcularTamanhoPasta(file);
            }
        }
        return length;
    }
}
