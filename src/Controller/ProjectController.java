package Controller;

import Data.JsonBuilder;
import Data.MimeTypes;
import Http.HttpRequest;
import Http.HttpResponse;
import Http.HttpStatus;
import Model.ArquivoProjeto;
import Model.Projeto;
import Model.NoCanvas;
import Persistencia.GerenciadorEntidade;
import Persistencia.ConexaoDB;
import Files.FilesInfo;
import java.util.List;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ProjectController {

    private boolean verificarAcessoProjeto(int projetoId, Security.User user) {
        if (user == null) return false;
        Optional<Projeto> opt = GerenciadorEntidade.buscarPorId(Projeto.class, projetoId);
        return opt.isPresent() && opt.get().getTenantId() == user.getTenantId();
    }

    public HttpResponse listarProjetos(HttpRequest req) {
        int tenantId = req.getUser().getTenantId();
        List<Projeto> projetos = GerenciadorEntidade.buscarPor(Projeto.class, "tenantId", tenantId);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < projetos.size(); i++) {
            if (i > 0) sb.append(",");
            Projeto p = projetos.get(i);
            sb.append(new JsonBuilder()
                    .add("id", p.getId())
                    .add("nome", p.getNome())
                    .add("descricao", p.getDescricao())
                    .add("criador_id", p.getCriadorId())
                    .add("caminho_raiz", p.getCaminhoRaiz())
                    .add("icone", p.getIcone())
                    .add("criado_em", p.getCriadoEm())
                    .add("atualizado_em", p.getAtualizadoEm())
                    .build());
        }
        sb.append("]");
        return HttpResponse.ok().json(sb.toString());
    }

    public HttpResponse criarProjeto(HttpRequest req) {
        String corpo = req.getCorpo();
        String nome = extrairCampoJson(corpo, "nome");
        String descricao = extrairCampoJson(corpo, "descricao");

        if (nome == null || nome.isBlank()) {
            return HttpResponse.requisicaoInvalida("Nome do projeto é obrigatório.");
        }

        Projeto p = new Projeto();
        p.setNome(nome);
        p.setDescricao(descricao != null ? descricao : "");
        p.setCriadorId(req.getUser().getId());
        p.setTenantId(req.getUser().getTenantId());

        String pastaNome = nome.replaceAll("[^a-zA-Z0-9_-]", "_");
        String caminhoRaiz = "public/Workspace/" + pastaNome;
        p.setCaminhoRaiz(caminhoRaiz);
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(caminhoRaiz));
        } catch (Exception e) {
            System.err.println("[ProjectController] Erro ao criar diretório físico para projeto: " + e.getMessage());
        }

        GerenciadorEntidade.salvar(p);
        Model.LogAtividade.registrar("CRIAR_PROJETO", req.getUser(), p.getId(), "Projeto '" + p.getNome() + "' criado.");

        // Criar nó inicial de pasta no Canvas
        NoCanvas noInicial = new NoCanvas();
        noInicial.setProjetoId(p.getId());
        noInicial.setTipo("pasta");
        noInicial.setTitulo(p.getNome());
        noInicial.setPosX(100);
        noInicial.setPosY(100);
        noInicial.setTenantId(req.getUser().getTenantId());
        GerenciadorEntidade.salvar(noInicial);

        String jsonResponse = new JsonBuilder()
                .add("status", "success")
                .add("id", p.getId())
                .add("nome", p.getNome())
                .build();
        return HttpResponse.ok().json(jsonResponse);
    }

    public HttpResponse obterProjeto(HttpRequest req) {
        try {
            int id = Integer.parseInt(req.getParametroPath("id"));
            Optional<Projeto> opt = GerenciadorEntidade.buscarPorId(Projeto.class, id);
            if (opt.isEmpty()) return HttpResponse.naoEncontrado();
            Projeto p = opt.get();
            if (p.getTenantId() != req.getUser().getTenantId()) {
                return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso não autorizado ao projeto deste inquilino."));
            }
            String json = new JsonBuilder()
                    .add("id", p.getId())
                    .add("nome", p.getNome())
                    .add("descricao", p.getDescricao())
                    .add("caminho_raiz", p.getCaminhoRaiz())
                    .add("icone", p.getIcone())
                    .build();
            return HttpResponse.ok().json(json);
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID do projeto inválido.");
        }
    }

    public HttpResponse deletarProjeto(HttpRequest req) {
        try {
            int id = Integer.parseInt(req.getParametroPath("id"));
            Optional<Projeto> opt = GerenciadorEntidade.buscarPorId(Projeto.class, id);
            if (opt.isEmpty()) return HttpResponse.naoEncontrado();
            Projeto p = opt.get();
            if (p.getTenantId() != req.getUser().getTenantId()) {
                return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Não autorizado."));
            }
            
            int tenantId = req.getUser() != null ? req.getUser().getTenantId() : 1;

            Model.LogAtividade.registrar("DELETAR_PROJETO", req.getUser(), id, "Projeto '" + p.getNome() + "' removido.");

            ConexaoDB.iniciarTransacao();
            try {
                GerenciadorEntidade.executarDelete("ligacoes_canvas", "projeto_id", id);
                GerenciadorEntidade.executarDelete("nos_canvas", "projeto_id", id);
                GerenciadorEntidade.executarDelete("arquivos_projeto", "projeto_id", id);
                
                boolean removido = GerenciadorEntidade.remover(Projeto.class, id);
                if (!removido) {
                    ConexaoDB.rollbackTransacao();
                    return HttpResponse.naoEncontrado();
                }
                
                ConexaoDB.comitarTransacao();

                // Limpeza física da pasta do projeto
                Path diretorioProjeto = Paths.get("public", "Workspace", "tenant_" + tenantId, "projeto_" + id);
                deletarDiretorioFisico(diretorioProjeto);

                return HttpResponse.ok().json(JsonBuilder.sucesso("Projeto removido com sucesso."));
            } catch (Exception e) {
                ConexaoDB.rollbackTransacao();
                throw e;
            }
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID do projeto inválido.");
        } catch (Exception e) {
            return HttpResponse.erroInterno("Erro ao remover projeto: " + e.getMessage());
        }
    }

    public HttpResponse listarArquivos(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            List<ArquivoProjeto> arquivos = GerenciadorEntidade.buscarPor(ArquivoProjeto.class, "projetoId", projetoId);

            String pastaIdParam = req.getParametrosQuery("pasta_id");
            if (pastaIdParam != null && !pastaIdParam.isBlank()) {
                int pastaId = Integer.parseInt(pastaIdParam);
                arquivos = arquivos.stream()
                        .filter(a -> a.getPastaPaiId() != null && a.getPastaPaiId() == pastaId)
                        .collect(java.util.stream.Collectors.toList());
            }

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arquivos.size(); i++) {
                if (i > 0) sb.append(",");
                ArquivoProjeto aq = arquivos.get(i);
                sb.append(new JsonBuilder()
                        .add("id", aq.getId())
                        .add("projeto_id", aq.getProjetoId())
                        .add("nome", aq.getNome())
                        .add("caminho", aq.getCaminho())
                        .add("tipo_mime", aq.getTipoMime())
                        .add("tamanho_bytes", aq.getTamanhoBytes())
                        .add("tipo_arquivo", aq.getTipoArquivo())
                        .add("eh_pasta", aq.isEhPasta())
                        .add("pasta_pai_id", aq.getPastaPaiId())
                        .add("criado_em", aq.getCriadoEm())
                        .build());
            }
            sb.append("]");
            return HttpResponse.ok().json(sb.toString());
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID do projeto inválido.");
        }
    }

    public HttpResponse criarPasta(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            String corpo = req.getCorpo();
            String nome = extrairCampoJson(corpo, "nome");
            String pastaPaiIdStr = extrairCampoJson(corpo, "pasta_pai_id");

            if (nome == null || nome.isBlank()) {
                return HttpResponse.requisicaoInvalida("Nome da pasta é obrigatório.");
            }

            ArquivoProjeto pasta = new ArquivoProjeto();
            pasta.setProjetoId(projetoId);
            pasta.setNome(nome);
            pasta.setEhPasta(true);
            pasta.setCaminho("");
            pasta.setTipoArquivo("pasta");
            pasta.setTipoMime("");

            if (pastaPaiIdStr != null && !pastaPaiIdStr.isBlank() && !pastaPaiIdStr.equals("null")) {
                pasta.setPastaPaiId(Integer.parseInt(pastaPaiIdStr));
            }

            GerenciadorEntidade.salvar(pasta);
            Model.LogAtividade.registrar("CRIAR_PASTA", req.getUser(), projetoId, "Pasta '" + pasta.getNome() + "' criada.");

            String json = new JsonBuilder()
                    .add("status", "success")
                    .add("id", pasta.getId())
                    .add("nome", pasta.getNome())
                    .add("eh_pasta", true)
                    .build();
            return HttpResponse.ok().json(json);
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        }
    }

    public HttpResponse moverArquivo(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            int aid = Integer.parseInt(req.getParametroPath("aid"));
            Optional<ArquivoProjeto> opt = GerenciadorEntidade.buscarPorId(ArquivoProjeto.class, aid);
            if (opt.isEmpty()) return HttpResponse.naoEncontrado();

            ArquivoProjeto aq = opt.get();
            String corpo = req.getCorpo();
            String novaPastaIdStr = extrairCampoJson(corpo, "nova_pasta_id");

            Integer novaPastaId = null;
            if (novaPastaIdStr != null && !novaPastaIdStr.isBlank() && !novaPastaIdStr.equals("null")) {
                novaPastaId = Integer.parseInt(novaPastaIdStr);
            }

            // Se for uma pasta, evitar ciclos
            if (aq.isEhPasta() && novaPastaId != null) {
                List<ArquivoProjeto> todos = GerenciadorEntidade.buscarPor(ArquivoProjeto.class, "projetoId", projetoId);
                if (ehDescendente(aq.getId(), novaPastaId, todos)) {
                    return HttpResponse.requisicaoInvalida("Não é possível mover uma pasta para dentro de si mesma ou de suas subpastas.");
                }
            }

            aq.setPastaPaiId(novaPastaId);
            GerenciadorEntidade.salvar(aq);
            Model.LogAtividade.registrar("MOVER_ITEM", req.getUser(), projetoId, "Item '" + aq.getNome() + "' movido.");
            return HttpResponse.ok().json(JsonBuilder.sucesso("Item movido com sucesso."));
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        }
    }

    public HttpResponse obterArvore(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            List<ArquivoProjeto> todos = GerenciadorEntidade.buscarPor(ArquivoProjeto.class, "projetoId", projetoId);

            StringBuilder raiz = new StringBuilder("[");
            boolean primeiro = true;
            for (ArquivoProjeto aq : todos) {
                if (aq.getPastaPaiId() == null) {
                    if (!primeiro) raiz.append(",");
                    primeiro = false;
                    raiz.append(construirNoArvore(aq, todos));
                }
            }
            raiz.append("]");

            return HttpResponse.ok().json("{\"raiz\":" + raiz.toString() + "}");
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID do projeto inválido.");
        }
    }

    public HttpResponse deletarPasta(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            int pid = Integer.parseInt(req.getParametroPath("pid"));
            Optional<ArquivoProjeto> opt = GerenciadorEntidade.buscarPorId(ArquivoProjeto.class, pid);
            if (opt.isEmpty()) return HttpResponse.naoEncontrado();

            ArquivoProjeto pasta = opt.get();
            if (!pasta.isEhPasta()) {
                return HttpResponse.requisicaoInvalida("O item especificado não é uma pasta.");
            }

            List<ArquivoProjeto> todos = GerenciadorEntidade.buscarPor(ArquivoProjeto.class, "projetoId", projetoId);
            
            java.util.List<ArquivoProjeto> arquivosParaDeletar = new java.util.ArrayList<>();
            java.util.List<ArquivoProjeto> pastasParaDeletar = new java.util.ArrayList<>();
            pastasParaDeletar.add(pasta);

            coletarDescendentes(pid, todos, arquivosParaDeletar, pastasParaDeletar);

            int tenantId = req.getUser() != null ? req.getUser().getTenantId() : 1;

            ConexaoDB.iniciarTransacao();
            try {
                // 1. Limpar arquivos da pasta (DB e Canvas)
                for (ArquivoProjeto aq : arquivosParaDeletar) {
                    deletarNoECanvasPorArquivoId(projetoId, aq.getId());
                    GerenciadorEntidade.remover(ArquivoProjeto.class, aq.getId());
                }

                // 2. Limpar subpastas e a pasta pai (DB e Canvas)
                for (ArquivoProjeto p : pastasParaDeletar) {
                    deletarNoECanvasPorArquivoId(projetoId, p.getId());
                    GerenciadorEntidade.remover(ArquivoProjeto.class, p.getId());
                }

                Model.LogAtividade.registrar("DELETAR_PASTA", req.getUser(), projetoId, "Pasta '" + pasta.getNome() + "' e todo o seu conteúdo foram excluídos.");

                ConexaoDB.comitarTransacao();

                // 3. Exclusão física dos arquivos no disco
                for (ArquivoProjeto aq : arquivosParaDeletar) {
                    try {
                        Path caminhoFisico = Paths.get("public", "Workspace", "tenant_" + tenantId, "projeto_" + projetoId, aq.getNome());
                        Files.deleteIfExists(caminhoFisico);
                    } catch (Exception e) {
                        System.err.println("[ProjectController] Erro ao deletar arquivo físico na exclusão recursiva: " + e.getMessage());
                    }
                }

                return HttpResponse.ok().json(JsonBuilder.sucesso("Pasta e todo o seu conteúdo foram excluídos com sucesso."));
            } catch (Exception e) {
                ConexaoDB.rollbackTransacao();
                throw e;
            }
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        } catch (Exception e) {
            return HttpResponse.erroInterno("Erro ao excluir pasta: " + e.getMessage());
        }
    }

    public HttpResponse renomearPasta(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            int pid = Integer.parseInt(req.getParametroPath("pid"));
            Optional<ArquivoProjeto> opt = GerenciadorEntidade.buscarPorId(ArquivoProjeto.class, pid);
            if (opt.isEmpty()) return HttpResponse.naoEncontrado();

            ArquivoProjeto pasta = opt.get();
            String corpo = req.getCorpo();
            String novoNome = extrairCampoJson(corpo, "nome");

            if (novoNome == null || novoNome.isBlank()) {
                return HttpResponse.requisicaoInvalida("Nome é obrigatório.");
            }

            String antigoNome = pasta.getNome();
            pasta.setNome(novoNome);
            GerenciadorEntidade.salvar(pasta);
            Model.LogAtividade.registrar("RENOMEAR_PASTA", req.getUser(), projetoId, "Pasta '" + antigoNome + "' renomeada para '" + novoNome + "'.");
            return HttpResponse.ok().json(JsonBuilder.sucesso("Pasta renomeada com sucesso."));
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        }
    }

    public HttpResponse deletarArquivo(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            int aid = Integer.parseInt(req.getParametroPath("aid"));
            Optional<ArquivoProjeto> opt = GerenciadorEntidade.buscarPorId(ArquivoProjeto.class, aid);
            if (opt.isEmpty()) return HttpResponse.naoEncontrado();

            ArquivoProjeto aq = opt.get();
            if (aq.isEhPasta()) {
                return HttpResponse.requisicaoInvalida("O item especificado não é um arquivo.");
            }

            int tenantId = req.getUser() != null ? req.getUser().getTenantId() : 1;

            ConexaoDB.iniciarTransacao();
            try {
                deletarNoECanvasPorArquivoId(projetoId, aid);
                GerenciadorEntidade.remover(ArquivoProjeto.class, aid);
                ConexaoDB.comitarTransacao();

                // Deletar arquivo físico
                try {
                    Path caminhoFisico = Paths.get("public", "Workspace", "tenant_" + tenantId, "projeto_" + projetoId, aq.getNome());
                    Files.deleteIfExists(caminhoFisico);
                } catch (Exception e) {
                    System.err.println("[ProjectController] Erro ao deletar arquivo físico: " + e.getMessage());
                }

                return HttpResponse.ok().json(JsonBuilder.sucesso("Arquivo excluído com sucesso."));
            } catch (Exception e) {
                ConexaoDB.rollbackTransacao();
                throw e;
            }
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        } catch (Exception e) {
            return HttpResponse.erroInterno("Erro ao excluir arquivo: " + e.getMessage());
        }
    }

    public HttpResponse renomearArquivo(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            int aid = Integer.parseInt(req.getParametroPath("aid"));
            Optional<ArquivoProjeto> opt = GerenciadorEntidade.buscarPorId(ArquivoProjeto.class, aid);
            if (opt.isEmpty()) return HttpResponse.naoEncontrado();

            ArquivoProjeto aq = opt.get();
            if (aq.isEhPasta()) {
                return HttpResponse.requisicaoInvalida("O item especificado não é um arquivo.");
            }

            String corpo = req.getCorpo();
            String novoNome = extrairCampoJson(corpo, "nome");
            if (novoNome == null || novoNome.isBlank()) {
                return HttpResponse.requisicaoInvalida("Novo nome é obrigatório.");
            }

            // Sanitizar o novo nome mantendo a extensão original
            String ext = FilesInfo.obterExtensao(aq.getNome());
            String novoNomeSanitizado = novoNome.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
            if (!novoNomeSanitizado.endsWith("." + ext) && !ext.isEmpty()) {
                novoNomeSanitizado = novoNomeSanitizado + "." + ext;
            }

            int tenantId = req.getUser() != null ? req.getUser().getTenantId() : 1;
            Path diretorio = Paths.get("public", "Workspace", "tenant_" + tenantId, "projeto_" + projetoId);
            Path caminhoAntigo = diretorio.resolve(aq.getNome());
            Path caminhoNovo = diretorio.resolve(novoNomeSanitizado);

            ConexaoDB.iniciarTransacao();
            try {
                // Renomear fisicamente no disco
                if (Files.exists(caminhoAntigo)) {
                    Files.move(caminhoAntigo, caminhoNovo, StandardCopyOption.REPLACE_EXISTING);
                }

                // Salvar no banco de dados
                aq.setNome(novoNomeSanitizado);
                aq.setCaminho("/workspace/tenant_" + tenantId + "/projeto_" + projetoId + "/" + novoNomeSanitizado);
                aq.setTipoMime(Data.MimeTypes.porCaminho(novoNomeSanitizado));
                aq.setTipoArquivo(FilesInfo.obterTipoArquivo(novoNomeSanitizado));
                GerenciadorEntidade.salvar(aq);

                // Sincronizar NoCanvas correspondente
                List<NoCanvas> nos = GerenciadorEntidade.buscarPor(NoCanvas.class, "projetoId", projetoId);
                for (NoCanvas n : nos) {
                    if (n.getArquivoId() != null && n.getArquivoId() == aid) {
                        n.setTitulo(novoNomeSanitizado);
                        n.setDadosExtra(new JsonBuilder()
                                .add("caminho", aq.getCaminho())
                                .add("tipo_arquivo", aq.getTipoArquivo())
                                .add("tamanho", aq.getTamanhoBytes())
                                .build());
                        GerenciadorEntidade.salvar(n);
                    }
                }

                ConexaoDB.comitarTransacao();
                return HttpResponse.ok().json(JsonBuilder.sucesso("Arquivo renomeado com sucesso."));
            } catch (Exception e) {
                ConexaoDB.rollbackTransacao();
                throw e;
            }
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        } catch (Exception e) {
            return HttpResponse.erroInterno("Erro ao renomear arquivo: " + e.getMessage());
        }
    }

    private boolean ehDescendente(int folderId, int targetParentId, List<ArquivoProjeto> todos) {
        if (folderId == targetParentId) return true;
        for (ArquivoProjeto item : todos) {
            if (item.getId() == targetParentId) {
                if (item.getPastaPaiId() == null) return false;
                return ehDescendente(folderId, item.getPastaPaiId(), todos);
            }
        }
        return false;
    }

    private void coletarDescendentes(int pastaId, List<ArquivoProjeto> todos, List<ArquivoProjeto> arquivos, List<ArquivoProjeto> pastas) {
        for (ArquivoProjeto filho : todos) {
            if (filho.getPastaPaiId() != null && filho.getPastaPaiId() == pastaId) {
                if (filho.isEhPasta()) {
                    pastas.add(filho);
                    coletarDescendentes(filho.getId(), todos, arquivos, pastas);
                } else {
                    arquivos.add(filho);
                }
            }
        }
    }

    private void deletarNoECanvasPorArquivoId(int projetoId, int arquivoId) {
        List<Model.NoCanvas> nos = GerenciadorEntidade.buscarPor(Model.NoCanvas.class, "projetoId", projetoId);
        for (Model.NoCanvas n : nos) {
            if (n.getArquivoId() != null && n.getArquivoId() == arquivoId) {
                GerenciadorEntidade.executarDelete("ligacoes_canvas", "origem_id", n.getId());
                GerenciadorEntidade.executarDelete("ligacoes_canvas", "destino_id", n.getId());
                GerenciadorEntidade.executarDelete("nos_canvas", "id", n.getId());
            }
        }
    }

    private void deletarDiretorioFisico(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (Exception ignored) {}
                    });
            }
        } catch (Exception e) {
            System.err.println("[ProjectController] Erro ao deletar pasta física: " + e.getMessage());
        }
    }

    public HttpResponse uploadArquivo(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            Optional<Projeto> opt = GerenciadorEntidade.buscarPorId(Projeto.class, projetoId);
            if (opt.isEmpty()) return HttpResponse.naoEncontrado();

            Projeto p = opt.get();
            
            String nomeHeaderRaw = req.getCabecalhos("X-File-Name");
            if (nomeHeaderRaw == null || nomeHeaderRaw.isBlank()) {
                return HttpResponse.requisicaoInvalida("Nome do arquivo (X-File-Name) é obrigatório no header.");
            }
            String nomeHeader = java.net.URLDecoder.decode(nomeHeaderRaw, java.nio.charset.StandardCharsets.UTF_8);
            
            String nomeSanitizado = Paths.get(nomeHeader).getFileName().toString();
            nomeSanitizado = nomeSanitizado.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
            
            String tipoArquivo = req.getCabecalhos("X-File-Type");
            if (tipoArquivo == null || tipoArquivo.isBlank()) {
                tipoArquivo = FilesInfo.obterTipoArquivo(nomeSanitizado);
            }
            
            String pastaIdStr = req.getCabecalhos("X-Folder-Id");
            
            int tenantId = req.getUser() != null ? req.getUser().getTenantId() : 1;
            Path diretorioProjeto = Paths.get("public", "Workspace", "tenant_" + tenantId, "projeto_" + p.getId());
            
            if (!Files.exists(diretorioProjeto)) {
                Files.createDirectories(diretorioProjeto);
            }
            
            Path destino = diretorioProjeto.resolve(nomeSanitizado);
            long tamanhoBytes = 0;

            java.io.InputStream stream = req.getBodyStream();
            if (stream != null) {
                tamanhoBytes = Files.copy(stream, destino, StandardCopyOption.REPLACE_EXISTING);
            } else {
                String corpo = req.getCorpo();
                if (corpo != null && !corpo.isEmpty()) {
                    byte[] bytes = corpo.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    Files.write(destino, bytes);
                    tamanhoBytes = bytes.length;
                } else {
                    if (!Files.exists(destino)) {
                        Files.createFile(destino);
                    }
                }
            }

            ArquivoProjeto aq = new ArquivoProjeto();
            aq.setProjetoId(p.getId());
            aq.setNome(nomeSanitizado);
            aq.setCaminho("/workspace/tenant_" + tenantId + "/projeto_" + p.getId() + "/" + nomeSanitizado);
            aq.setTamanhoBytes(tamanhoBytes);
            aq.setTipoArquivo(tipoArquivo);
            aq.setTipoMime(MimeTypes.porCaminho(nomeSanitizado));

            if (pastaIdStr != null && !pastaIdStr.isBlank()) {
                try {
                    aq.setPastaPaiId(Integer.parseInt(pastaIdStr));
                } catch (NumberFormatException ignored) {}
            }

            GerenciadorEntidade.salvar(aq);
            Model.LogAtividade.registrar("UPLOAD_ARQUIVO", req.getUser(), p.getId(), "Upload do arquivo '" + nomeSanitizado + "'.");

            NoCanvas n = new NoCanvas();
            n.setProjetoId(p.getId());
            n.setArquivoId(aq.getId());
            n.setTipo("arquivo");
            n.setTitulo(nomeSanitizado);
            n.setPosX(300);
            n.setPosY(200);
            n.setDadosExtra(new JsonBuilder()
                    .add("caminho", aq.getCaminho())
                    .add("tipo_arquivo", aq.getTipoArquivo())
                    .add("tamanho", aq.getTamanhoBytes())
                    .build());
            GerenciadorEntidade.salvar(n);

            String jsonRes = new JsonBuilder()
                    .add("status", "success")
                    .add("id", aq.getId())
                    .add("no_id", n.getId())
                    .add("nome", aq.getNome())
                    .add("caminho", aq.getCaminho())
                    .build();
            
            return HttpResponse.ok().json(jsonRes);
        } catch (Exception e) {
            System.err.println("[ProjectController] Erro ao fazer upload de arquivo: " + e.getMessage());
            return HttpResponse.erroInterno("Erro ao processar upload do arquivo.");
        }
    }

    public HttpResponse obterConteudoArquivo(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            int aid = Integer.parseInt(req.getParametroPath("aid"));
            Optional<ArquivoProjeto> opt = GerenciadorEntidade.buscarPorId(ArquivoProjeto.class, aid);
            if (opt.isEmpty()) return HttpResponse.naoEncontrado();

            ArquivoProjeto aq = opt.get();
            int tenantId = req.getUser() != null ? req.getUser().getTenantId() : 1;
            Path caminhoFisico = Paths.get("public", "Workspace", "tenant_" + tenantId, "projeto_" + projetoId, aq.getNome());

            String conteudo = "";
            if (Files.exists(caminhoFisico)) {
                conteudo = Files.readString(caminhoFisico, java.nio.charset.StandardCharsets.UTF_8);
            }

            String jsonRes = new JsonBuilder()
                    .add("id", aq.getId())
                    .add("nome", aq.getNome())
                    .add("conteudo", conteudo)
                    .add("tipo_mime", aq.getTipoMime() != null ? aq.getTipoMime() : "")
                    .add("tipo_arquivo", aq.getTipoArquivo() != null ? aq.getTipoArquivo() : "")
                    .build();

            return HttpResponse.ok().json(jsonRes);
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        } catch (Exception e) {
            return HttpResponse.erroInterno("Erro ao ler conteúdo do arquivo: " + e.getMessage());
        }
    }

    public HttpResponse salvarConteudoArquivo(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            int aid = Integer.parseInt(req.getParametroPath("aid"));
            Optional<ArquivoProjeto> opt = GerenciadorEntidade.buscarPorId(ArquivoProjeto.class, aid);
            if (opt.isEmpty()) return HttpResponse.naoEncontrado();

            ArquivoProjeto aq = opt.get();
            String corpo = req.getCorpo();
            String novoConteudo = extrairCampoJson(corpo, "conteudo");
            if (novoConteudo == null) {
                novoConteudo = "";
            }

            int tenantId = req.getUser() != null ? req.getUser().getTenantId() : 1;
            Path diretorio = Paths.get("public", "Workspace", "tenant_" + tenantId, "projeto_" + projetoId);
            if (!Files.exists(diretorio)) {
                Files.createDirectories(diretorio);
            }

            Path caminhoFisico = diretorio.resolve(aq.getNome());
            byte[] bytes = novoConteudo.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Files.write(caminhoFisico, bytes);

            aq.setTamanhoBytes(bytes.length);
            GerenciadorEntidade.salvar(aq);

            Model.LogAtividade.registrar("EDITAR_ARQUIVO", req.getUser(), projetoId, "Arquivo '" + aq.getNome() + "' editado.");

            return HttpResponse.ok().json(JsonBuilder.sucesso("Conteúdo do arquivo salvo com sucesso."));
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        } catch (Exception e) {
            return HttpResponse.erroInterno("Erro ao salvar conteúdo do arquivo: " + e.getMessage());
        }
    }

    private String construirNoArvore(ArquivoProjeto item, List<ArquivoProjeto> todos) {
        JsonBuilder jb = new JsonBuilder()
                .add("id", item.getId())
                .add("nome", item.getNome())
                .add("eh_pasta", item.isEhPasta())
                .add("tipo_arquivo", item.getTipoArquivo() != null ? item.getTipoArquivo() : "")
                .add("caminho", item.getCaminho() != null ? item.getCaminho() : "")
                .add("tamanho_bytes", item.getTamanhoBytes())
                .add("tipo_mime", item.getTipoMime() != null ? item.getTipoMime() : "")
                .add("criado_em", item.getCriadoEm());

        if (item.getPastaPaiId() != null) {
            jb.add("pasta_pai_id", item.getPastaPaiId());
        }

        if (item.isEhPasta()) {
            StringBuilder filhos = new StringBuilder("[");
            boolean primeiro = true;
            for (ArquivoProjeto filho : todos) {
                if (filho.getPastaPaiId() != null && filho.getPastaPaiId().equals(item.getId())) {
                    if (!primeiro) filhos.append(",");
                    primeiro = false;
                    filhos.append(construirNoArvore(filho, todos));
                }
            }
            filhos.append("]");

            String base = jb.build();
            return base.substring(0, base.length() - 1) + ",\"filhos\":" + filhos.toString() + "}";
        }

        return jb.build();
    }

    private String extrairCampoJson(String json, String campo) {
        try {
            java.util.Map<String, Object> map = Data.JsonParser.parse(json);
            return Data.JsonParser.getString(map, campo);
        } catch (Exception e) {
            return null;
        }
    }
}
