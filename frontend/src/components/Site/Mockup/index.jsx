import Styles from './Mockup.module.css'

function Mockup() {
  return (
    <section className={Styles.Container}>
        <div className={Styles.DescriptionContainer}>
            <p className={Styles.kicker}>OPERAÇÃO SEM ATRITO</p>
            <h3>Um novo ritmo para a sua barbearia</h3>
            <p>No dia a dia corrido, organizar atendimentos, horários e a gestão da barbearia pode ser desafiador. Por isso, centralizamos tudo em um único fluxo: agenda, equipe, comunicação e relatórios. Menos retrabalho, mais resultado.</p>

            <div className={Styles.pillRow}>
              <span>Agenda online</span>
              <span>Fila organizada</span>
              <span>Analise diaria</span>
            </div>
        </div>

        <div className={Styles.BenefitsGrid}>
          <article className={Styles.BenefitCard}>
            <h4>Fluxo inteligente</h4>
            <p>Alocação de horários com menos conflitos e mais previsibilidade para equipe e clientes.</p>
          </article>

          <article className={Styles.BenefitCard}>
            <h4>Dados acionaveis</h4>
            <p>Veja gargalos e horários de pico em segundos para ajustar a operação com segurança.</p>
          </article>

          <article className={Styles.BenefitCard}>
            <h4>Experiência premium</h4>
            <p>Confirmações e lembretes automáticos para reduzir faltas e elevar a qualidade do atendimento.</p>
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