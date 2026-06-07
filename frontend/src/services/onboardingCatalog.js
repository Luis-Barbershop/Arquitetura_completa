const shared = {
  replayHint: 'Você pode rever este onboarding a qualquer momento no menu do perfil.',
};

const PAGE_TARGETS = {
  'customer-home': [
    '[data-onboarding-id="customer-home-hero"]',
    '[data-onboarding-id="customer-home-search"]',
    '[data-onboarding-id="customer-home-list"]',
  ],
  'customer-barbershop-detail': [
    '[data-onboarding-id="shop-detail-banner"]',
    '[data-onboarding-id="shop-detail-services"]',
    '[data-onboarding-id="shop-detail-cta"]',
  ],
  'customer-booking': [
    '[data-onboarding-id="booking-services"]',
    '[data-onboarding-id="booking-professional"]',
    '[data-onboarding-id="booking-confirm"]',
  ],
  'customer-appointments': [
    '[data-onboarding-id="appointments-hero"]',
    '[data-onboarding-id="appointments-filters"]',
    '[data-onboarding-id="appointments-list"]',
  ],
  'customer-profile': [
    '[data-onboarding-id="customer-profile-card"]',
    '[data-onboarding-id="customer-profile-form"]',
    '[data-onboarding-id="customer-profile-notifications"]',
  ],
  'barber-home': [
    '[data-onboarding-id="barber-home-hero"]',
    '[data-onboarding-id="barber-home-actions"]',
    '[data-onboarding-id="barber-home-next"]',
  ],
  'barber-home-unlinked': [
    '[data-onboarding-id="barber-home-unlinked-panel"]',
    '[data-onboarding-id="barber-home-unlinked-panel"]',
    '[data-onboarding-id="barber-home-unlinked-panel"]',
  ],
  'barber-services': [
    '[data-onboarding-id="barber-services-hero"]',
    '[data-onboarding-id="barber-services-skills"]',
    '[data-onboarding-id="barber-services-skills"]',
  ],
  'owner-services': [
    '[data-onboarding-id="barber-services-hero"]',
    '[data-onboarding-id="barber-services-management"]',
    '[data-onboarding-id="barber-services-skills"]',
  ],
  'barber-unavailability': [
    '[data-onboarding-id="barber-unavailability-hero"]',
    '[data-onboarding-id="barber-unavailability-hour-form"]',
    '[data-onboarding-id="barber-unavailability-list"]',
  ],
  'barber-profile': [
    '[data-onboarding-id="barber-profile-hero"]',
    '[data-onboarding-id="barber-profile-card"]',
    '[data-onboarding-id="barber-profile-schedule"]',
  ],
  'barber-manual-booking': [
    '[data-onboarding-id="barber-manual-booking-hero"]',
    '[data-onboarding-id="barber-manual-booking-services"]',
    '[data-onboarding-id="barber-manual-booking-submit"]',
  ],
  'barber-appointments': [
    '[data-onboarding-id="appointments-hero"]',
    '[data-onboarding-id="appointments-filters"]',
    '[data-onboarding-id="appointments-list"]',
  ],
  'owner-stock': [
    '[data-onboarding-id="owner-stock-hero"]',
    '[data-onboarding-id="owner-stock-form"]',
    '[data-onboarding-id="owner-stock-list"]',
  ],
  'owner-team': [
    '[data-onboarding-id="owner-team-hero"]',
    '[data-onboarding-id="owner-team-members"]',
    '[data-onboarding-id="owner-team-commissions"]',
  ],
  'owner-dashboard': [
    '[data-onboarding-id="owner-dashboard-hero"]',
    '[data-onboarding-id="owner-dashboard-metrics"]',
    '[data-onboarding-id="owner-dashboard-reports"]',
  ],
  'owner-manage-shop': [
    '[data-onboarding-id="owner-manage-shop-hero"]',
    '[data-onboarding-id="owner-manage-shop-media"]',
    '[data-onboarding-id="owner-manage-shop-form"]',
  ],
  'owner-team-agenda': [
    '[data-onboarding-id="appointments-hero"]',
    '[data-onboarding-id="appointments-filters"]',
    '[data-onboarding-id="appointments-list"]',
  ],
  'barber-create-shop': [
    '[data-onboarding-id="barber-create-shop-page"]',
    '[data-onboarding-id="barber-create-shop-page"]',
    '[data-onboarding-id="barber-create-shop-page"]',
  ],
};

const DEFAULT_FALLBACK_SELECTORS = [
  '[data-onboarding-id="global-header"]',
  'main h1',
  'h1',
  'header',
];

const DEFAULT_PLACEMENTS = ['bottom', 'right', 'top'];

const withAnchors = (pageKey, steps) => {
  const targets = PAGE_TARGETS[pageKey] || [];

  return steps.map((step, index) => ({
    ...step,
    selector: step.selector || targets[index] || targets[targets.length - 1] || DEFAULT_FALLBACK_SELECTORS[0],
    fallbackSelectors: step.fallbackSelectors || DEFAULT_FALLBACK_SELECTORS,
    placement: step.placement || DEFAULT_PLACEMENTS[index % DEFAULT_PLACEMENTS.length],
    offset: step.offset || 14,
    spotlightPadding: step.spotlightPadding || 10,
    spotlightRadius: step.spotlightRadius || 14,
  }));
};

export const ONBOARDING_STEPS = {
  'customer-home': [
    { title: 'Bem-vindo ao painel do cliente', description: 'Aqui você encontra barbearias, pesquisa por nome e acompanha suas favoritas com acesso rápido.' },
    { title: 'Pesquisa e descoberta', description: 'Use a busca para filtrar resultados e comparar opções antes de agendar.' },
    { title: 'Próximos passos', description: `Abra uma barbearia para ver detalhes de serviços e iniciar seu agendamento. ${shared.replayHint}` },
  ],
  'customer-barbershop-detail': [
    { title: 'Visão da barbearia', description: 'Nesta tela você confere informações gerais, avaliação, endereço e profissionais disponíveis.' },
    { title: 'Serviços e equipe', description: 'Analise preço e duração dos serviços para decidir o melhor atendimento.' },
    { title: 'Ação principal', description: 'Quando estiver pronto, use “Agendar agora” para abrir o fluxo completo de marcação.' },
  ],
  'customer-booking': [
    { title: 'Monte seu agendamento', description: 'Selecione serviços, profissional, data e horário conforme disponibilidade real da agenda.' },
    { title: 'Revisão antes de confirmar', description: 'Confira o resumo final e escolha a forma de pagamento para evitar conflitos de horário.' },
    { title: 'Confirmação', description: 'Após confirmar, acompanhe o status em “Meus Agendamentos”.' },
  ],
  'customer-appointments': [
    { title: 'Central de agendamentos', description: 'Aqui você acompanha seus atendimentos, filtra por status e gerencia cada compromisso.' },
    { title: 'Ações disponíveis', description: 'Dependendo do status, você pode remarcar, cancelar e avaliar a barbearia após concluir.' },
    { title: 'Organização', description: 'Use visualização e filtros para encontrar rapidamente atendimentos futuros ou passados.' },
  ],
  'customer-profile': [
    { title: 'Seu perfil', description: 'Atualize nome, telefone e foto para manter seus dados sempre em dia.' },
    { title: 'Notificações', description: 'Ative notificações para receber alertas de agendamento e atualizações importantes.' },
    { title: 'Segurança', description: 'No menu do perfil você também encontra atalhos de segurança da conta.' },
  ],
  'barber-home': [
    { title: 'Home do profissional', description: 'Este painel centraliza visão rápida do dia, próximos atendimentos e atalhos operacionais.' },
    { title: 'Prioridades', description: 'Use os cards para agir rápido em agenda, serviços e rotinas do seu fluxo diário.' },
    { title: 'Navegação', description: `As demais funções ficam no menu superior/inferior por aba. ${shared.replayHint}` },
  ],
  'barber-home-unlinked': [
    { title: 'Bem-vindo ao painel pro', description: 'Você ainda não está vinculado a uma barbearia, então o foco aqui é concluir sua entrada no ecossistema.' },
    { title: 'Dois caminhos rápidos', description: 'Você pode criar sua própria barbearia agora ou abrir o perfil para aceitar um convite pendente.' },
    { title: 'Depois do vínculo', description: `Assim que houver vínculo, sua home muda para visão operacional completa de agenda e rotina. ${shared.replayHint}` },
  ],
  'barber-services': [
    { title: 'Serviços e habilidades', description: 'Nesta tela você gerencia serviços visíveis e, para colaborador, habilidades atribuídas.', placement: 'bottom' },
    { title: 'Cadastro e manutenção', description: 'Owners podem cadastrar, editar e remover serviços conforme estratégia da barbearia.', placement: 'right' },
    { title: 'Impacto na agenda', description: 'Duração e preço dos serviços influenciam disponibilidade e faturamento.', placement: 'top' },
  ],
  'owner-services': [
    { title: 'Gestão de serviços (owner)', description: 'Você controla o catálogo completo de serviços da barbearia.', placement: 'bottom' },
    { title: 'Padronização', description: 'Mantenha preço e duração consistentes para melhorar agenda e previsibilidade.', placement: 'left' },
    { title: 'Integração com equipe', description: 'Após ajustar serviços, revise comissões e habilidades no módulo de time.', placement: 'top' },
  ],
  'barber-unavailability': [
    { title: 'Bloqueio de agenda', description: 'Use esta tela para registrar indisponibilidades e evitar encaixes em horários inválidos.', placement: 'bottom' },
    { title: 'Planejamento', description: 'Defina períodos com antecedência para reduzir conflitos de remarcação.', placement: 'right' },
    { title: 'Resultado', description: 'Bloqueios ativos já impactam a disponibilidade exibida ao cliente.', placement: 'top' },
  ],
  'barber-profile': [
    { title: 'Perfil do barbeiro', description: 'Gerencie dados pessoais, foto, horários de trabalho e convites de equipe.', placement: 'bottom' },
    { title: 'Rotina de horário', description: 'Configure blocos por dia para manter agenda fiel ao seu expediente real.', placement: 'right' },
    { title: 'Notificações e conta', description: 'Ative notificações e use os atalhos de segurança da sua conta.', placement: 'left' },
  ],
  'barber-manual-booking': [
    { title: 'Novo encaixe', description: 'Registre atendimento imediato sem depender de cadastro prévio do cliente no app.', placement: 'bottom' },
    { title: 'Seleção obrigatória', description: 'Informe cliente, serviços e horário válido com base na disponibilidade atual.', placement: 'right' },
    { title: 'Confirmação operacional', description: 'Após salvar, o agendamento entra no fluxo normal de acompanhamento.', placement: 'top', spotlightPadding: 6, offset: 10 },
  ],
  'barber-appointments': [
    { title: 'Minha agenda', description: 'Visualize seus atendimentos por status e período para organizar o dia.' },
    { title: 'Ações por status', description: 'Concluir, cancelar e remarcar ficam disponíveis conforme regras do atendimento.' },
    { title: 'Produtividade', description: 'Use filtros e paginação para priorizar próximos horários e pendências.' },
  ],
  'owner-stock': [
    { title: 'Estoque da barbearia', description: 'Controle produtos, categorias e níveis mínimos para evitar ruptura.', placement: 'bottom' },
    { title: 'Movimentações', description: 'Registre consumo, venda e ajustes para manter rastreabilidade completa.', placement: 'left' },
    { title: 'Saúde operacional', description: 'Acompanhe alertas de baixo estoque e valor total imobilizado.', placement: 'top' },
  ],
  'owner-team': [
    { title: 'Gestão do time', description: 'Convide colaboradores, acompanhe membros ativos e administre comissões por serviço.', placement: 'bottom' },
    { title: 'Comissões', description: 'Defina percentuais de forma clara para cada atividade da equipe.', placement: 'right' },
    { title: 'Remoção segura', description: 'Ao remover colaborador, trate conflitos de agenda para evitar impacto operacional.', placement: 'left' },
  ],
  'owner-dashboard': [
    { title: 'Dashboard executivo', description: 'Acompanhe faturamento, gastos e performance da barbearia em painéis analíticos.', placement: 'bottom' },
    { title: 'Visões estratégicas', description: 'Use os relatórios para decisões sobre agenda, estoque, retenção e equipe.', placement: 'right' },
    { title: 'Fechamento do mês', description: 'Mantenha gastos fixos atualizados para resultado estimado mais confiável.', placement: 'top' },
  ],
  'owner-manage-shop': [
    { title: 'Dados da barbearia', description: 'Edite informações públicas, endereço e identidade visual da loja.', placement: 'bottom' },
    { title: 'Marca e presença', description: 'Miniatura e banner impactam confiança e conversão no fluxo do cliente.', placement: 'right' },
    { title: 'Consistência', description: 'Sempre valide dados de contato e localização após alterações.', placement: 'left' },
  ],
  'owner-team-agenda': [
    { title: 'Agenda da equipe', description: 'Veja os atendimentos de todos os profissionais da barbearia no mesmo lugar.' },
    { title: 'Filtros e período', description: 'Use data e status para monitorar operação e antecipar gargalos.' },
    { title: 'Acompanhamento', description: 'Combine esta visão com o dashboard para decisões de capacidade.' },
  ],
  'barber-create-shop': [
    { title: 'Cadastro da barbearia', description: 'Finalize os dados da sua loja para habilitar os módulos de gestão.' },
    { title: 'Endereço e identidade', description: 'Preencha endereço corretamente e envie logo para melhorar apresentação.' },
    { title: 'Após concluir', description: 'Com a barbearia criada, você desbloqueia agenda, serviços e demais recursos.' },
  ],
};

export const getOnboardingSteps = (pageKey) => {
  const steps = ONBOARDING_STEPS[pageKey] || [];
  return withAnchors(pageKey, steps);
};
