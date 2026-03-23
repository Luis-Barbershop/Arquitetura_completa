import Styles from './Services.module.css'

function Services() {
  return (
    <div className={Styles.services}>
        <div className={Styles.titleContainer}>
            <h1 className={Styles.title}>Nossos Serviços</h1>
        </div>

        <div className={Styles.mainContent}>
            <div className={Styles.content}>
                <p>
                    O CortaAi oferece uma variedade de serviços para atender às necessidades de barbearias e clientes. Para os clientes, nossa plataforma permite agendar serviços de forma rápida e fácil, encontrar barbeiros próximos, visualizar horários disponíveis e receber notificações de lembrete. Para as barbearias, oferecemos uma ferramenta de gestão completa, incluindo controle de agenda, relatórios de desempenho, gerenciamento de clientes e integração com sistemas de pagamento. Nosso objetivo é proporcionar uma experiência eficiente e moderna para ambos os lados, facilitando a conexão entre profissionais e clientes.
                </p>
            </div>
        </div>
    </div>
  )
}

export default Services