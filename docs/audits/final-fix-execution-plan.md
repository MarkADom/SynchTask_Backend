# Plano Completo de Execução — Fix Final (SynchTask Backend)

## Objetivo
Fechar definitivamente os problemas levantados no audit anterior com mudanças de baixo risco, previsíveis e testáveis, cobrindo:
1. `@EntityGraph` inválidos.
2. Contratos de repositório com referências de fetch inválidas.
3. Inconsistências de mapeamento no domínio (lado dono/inverso).
4. Limites DDD do agregado `User`.
5. Segurança (anotações e superfície pública).
6. N+1 e desempenho.
7. Alinhamento da suíte de testes.
8. Riscos de mapeamento DTO.

---

## Estratégia de Entrega (4 Fases)

### Fase 1 — Correções bloqueantes de inicialização JPA (prioridade máxima)
**Meta:** remover causas de falha de bootstrap/context load.

#### Escopo
- Corrigir `RefreshTokenRepository`:
  - `@EntityGraph(attributePaths = ["com/synchtask/user"])` -> `@EntityGraph(attributePaths = ["user"])`.
- Corrigir `UserRepository`:
  - Remover `@EntityGraph` que referencia `receivedFriendRequests` e `sentFriendRequests` (campos inexistentes no `User` atual).

#### Critério de aceite
- Aplicação sobe sem erro de parsing/metamodel de repository.
- Testes de contexto para repositórios passam.

#### Testes mínimos
- `@DataJpaTest` para `UserRepository`.
- `@DataJpaTest` para `RefreshTokenRepository`.

---

### Fase 2 — Correção funcional de query e consistência de relacionamento
**Meta:** eliminar bugs silenciosos de consulta e persistência de associação.

#### Escopo
1. `ChatRoomRepository.findByExactParticipants`:
   - substituir a estratégia atual (`MEMBER OF` com `Set<User>`) por consulta de matching exato de participantes (JOIN + GROUP BY/HAVING ou alternativa equivalente estável).
2. `ProjectService.update` (project-board):
   - garantir atualização do lado dono (`Board.project`) ao alterar o conjunto de boards do projeto.
   - opcional recomendado: encapsular em método de domínio (`assignBoards`) para manter invariantes.

#### Critério de aceite
- Busca por sala com participantes exatos retorna resultado correto para cenários com interseção parcial.
- Alterações de boards de projeto persistem corretamente no banco (lado dono consistente).

#### Testes mínimos
- teste de integração para `findByExactParticipants`:
  - match exato,
  - subconjunto,
  - superset.
- teste de serviço para update de projeto validando `Board.project` após persistência.

---

### Fase 3 — Hardening de segurança e clareza de autorização
**Meta:** tornar autorização explícita e reduzir risco de regressão por config global.

#### Escopo
- Adicionar `@PreAuthorize("isAuthenticated()")` em endpoints hoje protegidos só por `anyRequest().authenticated()`:
  - `TaskController.createTask`
  - `TaskController.getTaskDetail`
  - `TaskResourceController.listLinks`
  - `TaskResourceController.listAttachments`
  - `ActivityController.getMyActivities`
- Revisar `SecurityConfig` para reduzir `permitAll` amplo em `/auth/**`:
  - manter apenas endpoints realmente públicos.
  - preservar restrições por método (defesa em profundidade).

#### Critério de aceite
- sem autenticação: endpoints protegidos retornam `401`/`403` conforme esperado.
- endpoints públicos continuam acessíveis sem token.

#### Testes mínimos
- testes MVC/security para:
  - acesso anônimo negado em rotas protegidas.
  - acesso permitido em rotas públicas (`/auth/login`, `/auth/register`, `/jwks`, openid config).
  - endpoints admin continuam exigindo role admin.

---

### Fase 4 — Performance e contrato de leitura (N+1/DTO)
**Meta:** reduzir N+1 nos fluxos de listagem e desacoplar DTO de grafo excessivo.

#### Escopo
- Introduzir queries otimizadas/projeções para casos críticos:
  - listagem de projetos (`members`, `boards`).
  - listagem/paginação de tarefas (owner, collaborators, board, project).
  - listagem de boards com owner.
- Revisar DTOs para evitar exposição/acoplamento além do necessário:
  - avaliar se `TaskResponseDTO` precisa sempre de `boardName` e `projectName`.
  - separar DTO de lista vs detalhe quando fizer sentido.

#### Critério de aceite
- redução de queries por endpoint (medida com logs SQL/estatísticas Hibernate).
- sem regressão funcional dos contratos de API.

#### Testes mínimos
- testes de serviço/integrados com cenários de coleção grande.
- (opcional) assert de contagem de queries com ferramenta de inspeção.

---

## Plano de Branch/PR (recomendado)

### PR A (rápido e seguro)
- Fase 1 completa + testes de bootstrap.
- Objetivo: restaurar estabilidade de contexto.

### PR B
- Fase 2 completa + testes de integração de query/relacionamento.

### PR C
- Fase 3 completa + suíte de segurança.

### PR D
- Fase 4 completa + medições de desempenho + ajustes DTO.

> Se o time quiser “fix final” em PR único, manter commits separados por fase para facilitar rollback seletivo.

---

## Riscos e Mitigações

1. **Mudança de comportamento em repositório `UserRepository`**
   - Mitigar com testes de serviço que dependem de `findByEmail` e `findByEmailIn`.
2. **Query de chat exato gerar SQL caro**
   - Mitigar com índice em tabela de associação e benchmark com massa realista.
3. **Segurança: regressão em endpoints de auth**
   - Mitigar com matriz explícita de endpoints públicos/protegidos e testes automatizados.
4. **Otimização prematura de DTO**
   - Mitigar separando endpoint de lista/detalhe com critérios de uso do frontend.

---

## Checklist de Execução

### Checklist técnico
- [ ] Corrigir EntityGraph inválido de refresh token.
- [ ] Remover EntityGraphs órfãos de `UserRepository`.
- [ ] Refatorar query de chat com participantes exatos.
- [ ] Corrigir update de boards no projeto via lado dono.
- [ ] Adicionar `@PreAuthorize` faltantes.
- [ ] Refinar `permitAll` no security config.
- [ ] Otimizar queries de listagem críticas.
- [ ] Revisar DTOs de lista/detalhe.

### Checklist de qualidade
- [ ] `./gradlew test` verde no ambiente CI compatível.
- [ ] Novos testes de JPA/security adicionados.
- [ ] Revisão de impacto em API (contratos e status codes).
- [ ] Changelog técnico da correção publicado.

---

## Definição de Pronto (DoD)

O fix final é considerado concluído quando:
1. Não há referência de grafo/fetch para atributos inexistentes.
2. Consultas de chat por participantes exatos são corretas em todos os cenários relevantes.
3. Relacionamentos `Project`-`Board` persistem com consistência do lado dono.
4. Regras de segurança estão explícitas por endpoint crítico.
5. Endpoints de listagem prioritários não apresentam N+1 relevante.
6. Testes de regressão cobrindo os pontos acima passam em CI.

---

## Minha recomendação (opinião)
Se o objetivo é “fix final” com baixo risco, eu faria **em duas ondas**:
1. **Onda 1 (imediata):** Fases 1 + 2 + testes essenciais.
2. **Onda 2 (hardening):** Fases 3 + 4 com métrica de performance.

Assim você resolve primeiro o que quebra startup/correção funcional e depois fecha segurança/performance de forma controlada.
