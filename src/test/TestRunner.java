package test;

import test.Security.AuthManagerTest;
import test.Persistencia.GerenciadorEntidadeTest;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("            INICIANDO SUITE DE TESTES            ");
        System.out.println("==================================================");

        int totalFailures = 0;

        totalFailures += runTest("AuthManager - Geracao e Validacao de Token", AuthManagerTest::testGerarEValidarToken);
        totalFailures += runTest("AuthManager - Token Expirado", AuthManagerTest::testTokenExpirado);
        totalFailures += runTest("AuthManager - Assinatura Invalida", AuthManagerTest::testTokenAssinaturaInvalida);
        totalFailures += runTest("AuthManager - Token Malformado", AuthManagerTest::testTokenMalformado);

        totalFailures += runTest("GerenciadorEntidade - Operacoes de CRUD", GerenciadorEntidadeTest::testCrudOperations);
        totalFailures += runTest("GerenciadorEntidade - Sincronizacao de Sequencia PostgreSQL", GerenciadorEntidadeTest::testSequenceSyncAndUserCreation);
        totalFailures += runTest("UserStore - Operacoes com Usuario", GerenciadorEntidadeTest::testUserStoreOperations);

        System.out.println("==================================================");
        if (totalFailures > 0) {
            System.err.println("FALHA: " + totalFailures + " teste(s) falharam!");
            System.exit(1);
        } else {
            System.out.println("SUCESSO: Todos os testes passaram com exito!");
            System.exit(0);
        }
    }

    private static int runTest(String nome, RunnableTeste teste) {
        System.out.print("Executando: " + nome + "... ");
        try {
            teste.run();
            System.out.println("PASSOU");
            return 0;
        } catch (Throwable e) {
            System.out.println("FALHOU!");
            System.err.println("Erro: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    @FunctionalInterface
    public interface RunnableTeste {
        void run() throws Exception;
    }
}
