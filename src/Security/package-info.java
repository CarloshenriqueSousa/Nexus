/**
 * Pacote de segurança do Vaultra Secure Server.
 * 
 * Contém todas as camadas de proteção:
 * - PathGuard: Proteção contra Path Traversal
 * - AuthManager: Autenticação por token SHA-256
 * - RateLimiter: Controle de taxa Anti-DDoS por IP
 * - AuditLogger: Trilha de auditoria de acessos
 * - SecurityHeaders: Cabeçalhos HTTP de proteção
 * - SecurityMiddleware: Orquestrador central de segurança
 */
package Security;
