package Persistencia;

import java.util.List;
import java.util.Optional;

public interface Repositorio<T> {
    T salvar(T entidade);
    Optional<T> buscarPorId(int id);
    List<T> buscarTodos();
    boolean remover(int id);
}
