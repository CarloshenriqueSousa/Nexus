package Controller;

import Data.JsonBuilder;
import Http.HttpRequest;
import Http.HttpResponse;
import Model.NoCanvas;
import Model.LigacaoCanvas;
import Persistencia.GerenciadorEntidade;
import java.util.List;
import java.util.Optional;

import Model.Projeto;

public class CanvasController {

    private boolean verificarAcessoProjeto(int projetoId, Security.User user) {
        if (user == null) return false;
        Optional<Projeto> opt = GerenciadorEntidade.buscarPorId(Projeto.class, projetoId);
        return opt.isPresent() && opt.get().getTenantId() == user.getTenantId();
    }

    public HttpResponse obterCanvas(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(Http.HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            List<NoCanvas> nos = GerenciadorEntidade.buscarPor(NoCanvas.class, "projetoId", projetoId);
            List<LigacaoCanvas> ligacoes = GerenciadorEntidade.buscarPor(LigacaoCanvas.class, "projetoId", projetoId);

            StringBuilder sbNos = new StringBuilder("[");
            for (int i = 0; i < nos.size(); i++) {
                if (i > 0) sbNos.append(",");
                NoCanvas n = nos.get(i);
                sbNos.append(new JsonBuilder()
                        .add("id", n.getId())
                        .add("projeto_id", n.getProjetoId())
                        .add("arquivo_id", n.getArquivoId())
                        .add("tipo", n.getTipo())
                        .add("titulo", n.getTitulo())
                        .add("pos_x", n.getPosX())
                        .add("pos_y", n.getPosY())
                        .add("largura", n.getLargura())
                        .add("altura", n.getAltura())
                        .add("cor", n.getCor())
                        .add("dados_extra", n.getDadosExtra())
                        .build());
            }
            sbNos.append("]");

            StringBuilder sbLigacoes = new StringBuilder("[");
            for (int i = 0; i < ligacoes.size(); i++) {
                if (i > 0) sbLigacoes.append(",");
                LigacaoCanvas l = ligacoes.get(i);
                sbLigacoes.append(new JsonBuilder()
                        .add("id", l.getId())
                        .add("projeto_id", l.getProjetoId())
                        .add("origem_id", l.getOrigemId())
                        .add("destino_id", l.getDestinoId())
                        .add("tipo", l.getTipo())
                        .add("cor", l.getCor())
                        .add("label", l.getLabel())
                        .add("porta_origem", l.getPortaOrigem())
                        .add("porta_destino", l.getPortaDestino())
                        .build());
            }
            sbLigacoes.append("]");

            String res = "{\"nos\":" + sbNos.toString() + ",\"ligacoes\":" + sbLigacoes.toString() + "}";
            return HttpResponse.ok().json(res);
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID do projeto inválido.");
        }
    }

    public HttpResponse adicionarNo(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(Http.HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            String corpo = req.getCorpo();
            String tipo = extrairCampoJson(corpo, "tipo");
            String titulo = extrairCampoJson(corpo, "titulo");
            String posXStr = extrairCampoJson(corpo, "pos_x");
            String posYStr = extrairCampoJson(corpo, "pos_y");
            String arquivoIdStr = extrairCampoJson(corpo, "arquivo_id");

            NoCanvas n = new NoCanvas();
            n.setProjetoId(projetoId);
            n.setTipo(tipo != null ? tipo : "nota");
            n.setTitulo(titulo != null ? titulo : "Novo Bloco");
            if (posXStr != null) n.setPosX(Double.parseDouble(posXStr));
            if (posYStr != null) n.setPosY(Double.parseDouble(posYStr));
            if (arquivoIdStr != null && !arquivoIdStr.equals("null") && !arquivoIdStr.isEmpty()) {
                n.setArquivoId(Integer.parseInt(arquivoIdStr));
            }
            n.setTenantId(req.getUser().getTenantId());

            GerenciadorEntidade.salvar(n);

            String res = new JsonBuilder()
                    .add("status", "success")
                    .add("id", n.getId())
                    .add("tipo", n.getTipo())
                    .add("titulo", n.getTitulo())
                    .build();
            return HttpResponse.ok().json(res);
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        }
    }

    public HttpResponse atualizarNo(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(Http.HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            int nid = Integer.parseInt(req.getParametroPath("nid"));
            Optional<NoCanvas> opt = GerenciadorEntidade.buscarPorId(NoCanvas.class, nid);
            if (opt.isEmpty()) return HttpResponse.naoEncontrado();

            NoCanvas n = opt.get();
            String corpo = req.getCorpo();
            String posXStr = extrairCampoJson(corpo, "pos_x");
            String posYStr = extrairCampoJson(corpo, "pos_y");
            String larguraStr = extrairCampoJson(corpo, "largura");
            String alturaStr = extrairCampoJson(corpo, "altura");
            String titulo = extrairCampoJson(corpo, "titulo");
            String cor = extrairCampoJson(corpo, "cor");
            String dadosExtra = extrairCampoJson(corpo, "dados_extra");

            if (posXStr != null) n.setPosX(Double.parseDouble(posXStr));
            if (posYStr != null) n.setPosY(Double.parseDouble(posYStr));
            if (larguraStr != null) n.setLargura(Double.parseDouble(larguraStr));
            if (alturaStr != null) n.setAltura(Double.parseDouble(alturaStr));
            if (titulo != null) n.setTitulo(titulo);
            if (cor != null) n.setCor(cor);
            if (dadosExtra != null) n.setDadosExtra(dadosExtra);

            GerenciadorEntidade.salvar(n);
            return HttpResponse.ok().json(JsonBuilder.sucesso("Nó atualizado."));
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        }
    }

    public HttpResponse removerNo(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(Http.HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            int nid = Integer.parseInt(req.getParametroPath("nid"));
            GerenciadorEntidade.remover(NoCanvas.class, nid);
            return HttpResponse.ok().json(JsonBuilder.sucesso("Nó removido."));
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        }
    }

    public HttpResponse adicionarLigacao(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(Http.HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            String corpo = req.getCorpo();
            String origemIdStr = extrairCampoJson(corpo, "origem_id");
            String destinoIdStr = extrairCampoJson(corpo, "destino_id");
            String tipo = extrairCampoJson(corpo, "tipo");
            String portaOrigem = extrairCampoJson(corpo, "porta_origem");
            String portaDestino = extrairCampoJson(corpo, "porta_destino");

            if (origemIdStr == null || destinoIdStr == null) {
                return HttpResponse.requisicaoInvalida("Nós de origem e destino são obrigatórios.");
            }

            LigacaoCanvas l = new LigacaoCanvas();
            l.setProjetoId(projetoId);
            l.setOrigemId(Integer.parseInt(origemIdStr));
            l.setDestinoId(Integer.parseInt(destinoIdStr));
            if (tipo != null) l.setTipo(tipo);
            if (portaOrigem != null) l.setPortaOrigem(portaOrigem);
            if (portaDestino != null) l.setPortaDestino(portaDestino);

            GerenciadorEntidade.salvar(l);

            String res = new JsonBuilder()
                    .add("status", "success")
                    .add("id", l.getId())
                    .build();
            return HttpResponse.ok().json(res);
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        }
    }

    public HttpResponse removerLigacao(HttpRequest req) {
        try {
            int projetoId = Integer.parseInt(req.getParametroPath("id"));
            if (!verificarAcessoProjeto(projetoId, req.getUser())) {
                return new HttpResponse().status(Http.HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso negado."));
            }
            int lid = Integer.parseInt(req.getParametroPath("lid"));
            Optional<LigacaoCanvas> opt = GerenciadorEntidade.buscarPorId(LigacaoCanvas.class, lid);
            if (opt.isEmpty()) return HttpResponse.naoEncontrado();
            GerenciadorEntidade.remover(LigacaoCanvas.class, lid);
            return HttpResponse.ok().json(JsonBuilder.sucesso("Ligação removida."));
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        }
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
