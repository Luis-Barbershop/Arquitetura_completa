import Styles from './Services.module.css'

function Services() {
  return (
    <div className={Styles.services}>
      <div className={Styles.titleContainer}>
        <h1 className={Styles.title}>Nossos Serviços</h1>
      </div>

      <div className={Styles.mainContent}>
        <div className={Styles.card1}>
          <div className={Styles.titleCard}>
            <div className={Styles.iconContainer}>
              <img src="/public/Icons/pencil.png" alt="Pincel Barbeiro" />
            </div>
            <h3>SERVIÇOS PARA PROFISSIONAIS:</h3>
          </div>
          <div className={Styles.servicesList}>
            <p><span style={{fontWeight: 'bold'}}>1- Gestão Inteligente de Agenda:</span> Organize todos os atendimentos em um único sistema, evitando conflitos de horários e otimizando o fluxo de clientes.</p>
          </div>

          <div className={Styles.servicesList}>
            <p><span style={{fontWeight: 'bold'}}>2- Controle de Atendimentos:</span> Acompanhe os agendamentos em tempo real e tenha uma visão clara do funcionamento do seu negócio.</p>
          </div>

          <div className={Styles.servicesList}>
            <p><span style={{fontWeight: 'bold'}}>3- Relatórios e Insights:</span> Acesse dados importantes sobre atendimentos, horários mais movimentados e desempenho, ajudando na tomada de decisões.</p>
          </div>

           <div className={Styles.servicesList}>
            <p><span style={{fontWeight: 'bold'}}>4- Redução de Filas e Tempo Ocioso:</span> Melhore a experiência do cliente e aumente a eficiência da equipe com um fluxo de atendimento mais organizado.</p>
          </div>

          <div className={Styles.servicesList}>
            <p><span style={{fontWeight: 'bold'}}>5- Presença Digital:</span> Esteja disponível online para que novos clientes encontrem sua barbearia com facilidade.</p>
          </div>
          
        </div>

        <div className={Styles.card2}>
          <div className={Styles.titleCard}>
            <div className={Styles.iconContainer}>
              <img src="/public/Icons/chair.png" alt="Cadeira de Barbeiro" />
            </div>
            <h3>SERVIÇOS PARA OS CLIENTES:</h3>
          </div>

           <div className={Styles.servicesList}>
            <p><span style={{fontWeight: 'bold'}}>1- Agendamento Online Rápido: </span> Agende seu horário em poucos cliques, escolhendo o melhor dia e horário sem precisar enfrentar filas ou esperar atendimento.</p>
          </div>
          
           <div className={Styles.servicesList}>
            <p><span style={{fontWeight: 'bold'}}>2- Escolha do Profissional: </span> Selecione o barbeiro de sua preferência com base na disponibilidade e no estilo que mais combina com você.</p>
          </div>

            <div className={Styles.servicesList}>
            <p><span style={{fontWeight: 'bold'}}>3- Visualização de Horários em Tempo Real: </span> Tenha acesso à agenda atualizada das barbearias e encontre o melhor momento para o seu atendimento.</p>
          </div>

            <div className={Styles.servicesList}>
            <p><span style={{fontWeight: 'bold'}}>4- Praticidade e Comodidade: </span> Gerencie seus agendamentos de forma simples, com tudo organizado em um só lugar.</p>
          </div>

            <div className={Styles.servicesList}>
            <p><span style={{fontWeight: 'bold'}}>5- Lembretes de Agendamento: </span> Receba notificações para lembrar do seu horário, evitando esquecimentos e garantindo que você esteja sempre no horário certo.</p>
          </div>
          
        </div>
      </div>
    </div>
  )
}

export default Services