package Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilitário de logging estruturado para a plataforma Vaultra/Nexus.
 * Adiciona timestamps, nível do log e o módulo associado.
 */
public class Logger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void info(String modulo, String msg) {
        log("INFO", modulo, msg);
    }

    public static void warn(String modulo, String msg) {
        log("WARN", modulo, msg);
    }

    public static void error(String modulo, String msg) {
        log("ERROR", modulo, msg);
    }

    public static void error(String modulo, String msg, Throwable t) {
        log("ERROR", modulo, msg + " - Exceção: " + t.getMessage());
        t.printStackTrace();
    }

    private static synchronized void log(String nivel, String modulo, String msg) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.printf("[%s] [%s] [%s] %s%n", timestamp, nivel, modulo, msg);
    }
}
