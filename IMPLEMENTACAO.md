# LUPPA — implementação e validação

Branch: `fix/luppa-contact-events-cache`. Alterações locais, sem push ou publicação.

## Diagnóstico e alterações por etapa

1. **404 e contato:** a Home apenas validava e limpava os campos, sem requisição. Agora utiliza `POST /api/support/contact`, mostra estado de envio, exibe aceitação somente após resposta do backend e preserva os dados em falhas. O suporte também apresenta erro de limite de tentativas. O backend envia uma única mensagem à equipe via Brevo antes de responder; removida a confirmação antecipada ao visitante, que poderia afirmar recebimento sem o envio ao suporte ter ocorrido. Reutilizados validação, escape HTML, autenticação opcional, CORS e limite existente (5 tentativas/IP em 10 minutos e 3/e-mail em 1 hora). Nenhuma credencial no frontend. Adicionado fallback SPA da Vercel após a resolução de arquivos, excluindo API, uploads e arquivos ausentes.
2. **Interessados:** representa usuários distintos que salvaram o evento. `EventResponse.interestCount` é calculado a partir de `eventoSalvo`, com consulta agrupada nas listagens e consulta individual nos detalhes. A restrição única existente é preservada; salvar/remover usa bloqueio transacional do evento para serializar tentativas concorrentes. Cards, detalhes, perfil e insights reagem às alterações. O modal permite salvar e remover interesse, com proteção contra cliques repetidos. Falhas não geram incrementos otimistas fictícios.
3. **Painel do criador:** `my-events` agora reutiliza `EventManager`, extraído do editor administrativo existente. Inclui tabela, filtros, interessados, detalhes, edição, confirmação de exclusão, carregamento, erro e lista vazia. Carrega todas as páginas dos eventos próprios. Preserva os segundos das datas originais e as coordenadas quando não alteradas. Falha no upload é informada separadamente dos dados já salvos, evitando recriação acidental. O backend usa o principal autenticado, protege explicitamente `/api/events/mine` e mantém a exceção administrativa existente. Edições de eventos publicados por criadores voltam para análise; publicação direta continua reservada à administração.
4. **Cache por último:** integrado `@tanstack/query-core` aos serviços imperativos existentes, sem reescrever todas as telas. Requisições GET iguais compartilham a mesma Promise, incluindo filtros e paginação. Mutações de eventos, avaliações, interesse, imagens e moderação invalidam leituras relacionadas e notificam as telas. Respostas antigas em andamento são canceladas. Sem persistência; limpeza na troca de conta, logout e alteração da sessão em outra aba. Tokens não são usados nas chaves. Detalhes pessoais abertos são ocultados imediatamente ao trocar de usuário. Contas, preferências, usuários administrativos e auditoria não são armazenados nesse cache.

| Dados | staleTime | gcTime |
| --- | --- | --- |
| Categorias | 5 minutos | 10 minutos |
| Eventos, detalhes, avaliações públicas e links | 30 segundos | 5 minutos |
| Salvos, avaliações próprias, eventos próprios e insights | 15 segundos | 1 minuto |

O cache não modifica consultas do backend para esconder lentidão. O cálculo dos interessados em listagens evita uma consulta adicional por evento.

## Exclusão e SQL Server

Não foram alteradas entidades JPA, tabelas, restrições, triggers ou mecanismos de auditoria. Nenhuma migração é necessária. O contexto de auditoria existente continua aplicado às operações JPA.

A exclusão física verifica interessados, avaliações, denúncias e links sociais. Quando há vínculos, retorna HTTP 409 e orienta cancelar o evento em Editar, preservando o histórico. Outras violações de integridade referencial também retornam conflito, sem expor SQL. Não há remoção em cascata de histórico. Esta é uma limitação deliberada: eventos vinculados são cancelados, não apagados. A confirmação da exclusão é bloqueada durante o envio.

## Testes e resultados

- `cd Luppa && npm test`: **35 testes aprovados**, em 6 arquivos. Cobrem contato com aceitação/falha, gerenciamento e confirmação, atualização simultânea de interessados em card/modal, remontagem, isolamento na troca de usuário, paginação, datas, cache, rotas protegidas, acesso direto e voltar/avançar.
- `cd Luppa && npm run build`: **aprovado**. Permanece o aviso de bundle acima de 500 kB.
- `cd Luppa/backend && mvn verify` com Java 25: **118 testes encontrados, 111 aprovados, 7 ignorados, nenhuma falha ou erro**; pacote executável gerado. Inclui testes de autorização, moderação, dependências de exclusão, contagem agrupada, idempotência, limites de suporte, validação e CORS. Os testes com SQL Server isolado são ignorados pelo mecanismo de proteção já existente quando `TEST_DB_URL` não está definido; foi acrescentada cobertura de interessados para executar quando esse banco estiver disponível.
- `cd Luppa && npm run lint`: **falhou com 13 erros e 5 avisos**. Os 13 erros são anteriores à alteração (effects React, export do ToastProvider e ambiente de `process` no Vite); não foram desativadas regras para fazê-los passar. Antes havia 7 avisos.
- `git diff --check`: aprovado.
- `vite preview`: requisições HTTP locais a `/`, `/events`, `/profile`, `/my-events` e `/admin/users` retornaram HTML e bundle de produção válidos. Isso valida a entrega local; não substitui a publicação na Vercel. A autorização foi testada no React Router e na camada HTTP Spring em testes isolados.

Testes usam respostas controladas e colaboradores isolados apenas no ambiente de testes; nenhuma funcionalidade de produção foi substituída por mocks.

## Publicação e pendências externas

- Na Vercel, selecionar **Root Directory `Luppa`** (pasta que contém o package.json do React e `vercel.json`). O comando é `npm run build`, saída `dist`. O fallback é específico da hospedagem; precisa de novo deploy para corrigir URLs já publicadas. Se a configuração atual usar outra raiz, ajuste a raiz para esta pasta antes de publicar.
- Configurar `VITE_API_BASE_URL` com a origem **HTTPS real da API** para o build publicado. Não usar localhost em produção. Não foi inventado um endereço de backend nem um rewrite para um serviço desconhecido. `/api` é preservado pelo fallback e requer API/reverse proxy real quando usado na mesma origem.
- No backend, configurar `FRONTEND_URL` com as origens reais permitidas (por exemplo, `https://luppa-one.vercel.app`), `BREVO_API_KEY`, remetente Brevo verificado (`LUPPA_EMAIL_FROM`) e destinatário de suporte (`LUPPA_SUPPORT_EMAIL`). Valores secretos ficam nas configurações seguras do serviço, nunca no Vite. A entrega real de e-mail não foi testada por falta de configuração e conectividade; os testes verificam tratamento de aceitação e rejeição do provedor.
- A execução integral da API ainda depende de conexão e credenciais para o SQL Server informado: `db72445.public.databaseasp.net:1433`, banco `db72445`. Não houve acesso ao banco remoto. O ambiente atual não tem rota TCP pública suportada, VPN/grants nem credenciais injetadas. O esquema e os triggers reais não puderam ser inspecionados em execução.
- Para validar integrações, fornecer um **banco SQL Server descartável separado**, com `test` no nome, por `TEST_DB_URL`, `TEST_DB_USERNAME` e `TEST_DB_PASSWORD`. Não apontar os testes para `db72445` ou para `luppaBD`.
- Nenhuma publicação, envio real de contato, migração ou escrita no SQL Server remoto foi executada.

## Arquivos alterados/adicionados

- `Luppa/backend/src/main/java/com/luppa/api/dto/EventResponse.java`
- `Luppa/backend/src/main/java/com/luppa/api/exception/GlobalExceptionHandler.java`
- `Luppa/backend/src/main/java/com/luppa/api/mapper/EventMapper.java`
- `Luppa/backend/src/main/java/com/luppa/api/mapper/SavedEventMapper.java`
- `Luppa/backend/src/main/java/com/luppa/api/repository/EventRepository.java`
- `Luppa/backend/src/main/java/com/luppa/api/repository/EventSocialLinkRepository.java`
- `Luppa/backend/src/main/java/com/luppa/api/repository/RatingRepository.java`
- `Luppa/backend/src/main/java/com/luppa/api/repository/ReportRepository.java`
- `Luppa/backend/src/main/java/com/luppa/api/repository/SavedEventRepository.java`
- `Luppa/backend/src/main/java/com/luppa/api/security/SecurityConfig.java`
- `Luppa/backend/src/main/java/com/luppa/api/service/EventDeletionGuard.java`
- `Luppa/backend/src/main/java/com/luppa/api/service/EventResponseFactory.java`
- `Luppa/backend/src/main/java/com/luppa/api/service/EventService.java`
- `Luppa/backend/src/main/java/com/luppa/api/service/SavedEventService.java`
- `Luppa/backend/src/main/java/com/luppa/api/service/SupportService.java`
- `Luppa/backend/src/test/java/com/luppa/api/controller/EventWorkflowSecurityTest.java`
- `Luppa/backend/src/test/java/com/luppa/api/security/PublicEndpointRateLimiterTest.java`
- `Luppa/backend/src/test/java/com/luppa/api/service/EventFeaturedServiceIntegrationTest.java`
- `Luppa/backend/src/test/java/com/luppa/api/service/EventInterestTest.java`
- `Luppa/backend/src/test/java/com/luppa/api/service/EventManagementTest.java`
- `Luppa/backend/src/test/java/com/luppa/api/service/SupportServiceTest.java`
- `Luppa/package-lock.json`
- `Luppa/package.json`
- `Luppa/src/auth/AuthProvider.jsx`
- `Luppa/src/components/events/EventDetailsModal.jsx`
- `Luppa/src/components/events/EventManager.jsx`
- `Luppa/src/hooks/useEventDetails.js`
- `Luppa/src/hooks/useEventRevision.js`
- `Luppa/src/pages/AdminLuppa/AdminEvents.jsx`
- `Luppa/src/pages/CategoryEventsLuppa/CategoryEventsLuppa.jsx`
- `Luppa/src/pages/ExploreEventsLuppa/ExploreEventsLuppa.jsx`
- `Luppa/src/pages/MenuLuppa/MenuLuppa.jsx`
- `Luppa/src/pages/MenuLuppa/SupportPage.jsx`
- `Luppa/src/pages/MyEventsLuppa/CreatorInsightsLuppa.jsx`
- `Luppa/src/pages/MyEventsLuppa/MyEventsLuppa.jsx`
- `Luppa/src/pages/MyEventsLuppa/MyEventsLuppa.module.css`
- `Luppa/src/pages/ProfileLuppa/ProfileLuppa.jsx`
- `Luppa/src/services/apiClient.js`
- `Luppa/src/services/eventChanges.js`
- `Luppa/src/services/eventService.js`
- `Luppa/src/services/requestCache.js`
- `Luppa/src/services/supportService.js`
- `Luppa/src/test/contact.test.jsx`
- `Luppa/src/test/eventManager.test.jsx`
- `Luppa/src/test/eventService.test.js`
- `Luppa/src/test/interests.test.jsx`
- `Luppa/src/test/requestCache.test.js`
- `Luppa/src/test/routing.test.jsx`
- `Luppa/src/test/setup.js`
- `Luppa/vercel.json`
- `Luppa/vitest.config.js`
- `IMPLEMENTACAO.md` (este relatório).
