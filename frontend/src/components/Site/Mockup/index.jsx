import Styles from './Mockup.module.css'

function Mockup() {
  return (
    <section className={Styles.Container}>
        <div className={Styles.DescriptionContainer}>
            <p className={Styles.kicker}>OPERACAO SEM ATRITO</p>
            <h3>Um novo ritmo para a sua barbearia</h3>
            <p>No dia a dia corrido, organizar atendimentos, horarios e a gestao da barbearia pode ser desafiador. Por isso, centralizamos tudo em um unico fluxo: agenda, equipe, comunicacao e relatorios. Menos retrabalho, mais resultado.</p>

            <div className={Styles.pillRow}>
              <span>Agenda online</span>
              <span>Fila organizada</span>
              <span>Analise diaria</span>
            </div>
        </div>

        <div className={Styles.BenefitsGrid}>
          <article className={Styles.BenefitCard}>
            <h4>Fluxo inteligente</h4>
            <p>Alocacao de horarios com menos conflitos e mais previsibilidade para equipe e clientes.</p>
          </article>

          <article className={Styles.BenefitCard}>
            <h4>Dados acionaveis</h4>
            <p>Veja gargalos e horarios de pico em segundos para ajustar a operacao com seguranca.</p>
          </article>

          <article className={Styles.BenefitCard}>
            <h4>Experiencia premium</h4>
            <p>Confirmacoes e lembretes automaticos para reduzir faltas e elevar a qualidade do atendimento.</p>
          </article>

          <article className={Styles.BenefitCard}>
            <h4>Crescimento continuo</h4>
            <p>Uma base digital pronta para escalar equipe, unidades e receita sem caos operacional.</p>
          </article>
        </div>
    </section>
  )
}

export default Mockup