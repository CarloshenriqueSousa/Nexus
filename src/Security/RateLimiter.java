package Security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limitador de taxa simples por endereço IP (Anti-DDoS e brute-force).
 * Opera com uma janela deslizante aproximada de 60 segundos.
 */
public class RateLimiter {
    private static final ConcurrentHashMap<String, AtomicInteger> requisicoesPorIp = new ConcurrentHashMap<>();
    private static int maxRequisicoes = 60;

    static {
        // Thread Daemon para limpar o contador de requisições a cada minuto
        Thread resetThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000);
                    requisicoesPorIp.clear();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        resetThread.setDaemon(true);
        resetThread.setName("RateLimiter-Reset-Thread");
        resetThread.start();
    }

    /**
     * Configura o limite de requisições por minuto.
     */
    public static void configurar(int max) {
        if (max > 0) {
            maxRequisicoes = max;
            System.out.println("[RateLimiter] Configurado limite de " + max + " requisições por minuto.");
        }
    }

    /**
     * Verifica se o IP excedeu o limite de requisições e incrementa o contador.
     * @return true se a requisição é permitida, false se deve ser bloqueada (Rate Limit Exceeded)
     */
    public static boolean verificarEIncrementar(String ip) {
        if (ip == null || ip.isBlank()) return true; // Ignora se o IP for nulo/vazio
        
        AtomicInteger count = requisicoesPorIp.computeIfAbsent(ip, k -> new AtomicInteger(0));
        int total = count.incrementAndGet();
        
        return total <= maxRequisicoes;
    }
}
