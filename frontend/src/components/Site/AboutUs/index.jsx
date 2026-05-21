import Styles from './AboutUs.module.css'
function AboutUs() {
    return (
        <div id="sobre" className={Styles.aboutUs}>
            <div className={Styles.titleContainer}>
                <h1 className={Styles.title}>
                    Sobre Nós
                </h1>
            </div>

            <div className={Styles.mainContent}>
            <div className={Styles.content}>
                <p>
                    O CortaAi nasceu com o propósito de transformar a forma como barbearias e clientes se conectam no dia a dia. Somos uma plataforma digital que vai além do simples agendamento. Criamos um ecossistema completo que organiza o fluxo de atendimentos, elimina filas e oferece previsibilidade tanto para o cliente quanto para o profissional. <br/><br/>

                    Para os clientes, proporcionamos praticidade, permitindo encontrar barbeiros, visualizar horários disponíveis e agendar serviços com poucos cliques. Para as barbearias, entregamos uma ferramenta de gestão inteligente, com controle de agenda, relatórios e dados que auxiliam na tomada de decisão. Nosso objetivo é trazer eficiência, organização e uma experiência moderna para um setor tradicional, ajudando negócios a crescerem de forma estruturada e clientes a valorizarem melhor o seu tempo.
                </p>
            </div>

            <div className={Styles.imageContainer}>
                <img src="" alt="" />
            </div>
            </div>
        </div>
    )
}

export default AboutUs