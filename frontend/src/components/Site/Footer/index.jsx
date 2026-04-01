import Styles from './Footer.module.css'

function Footer() {
  return (
    <footer className={Styles.footerWrap}>
      <div className={Styles.footerCard}>
        <div className={Styles.topArea}>
          <div className={Styles.brandCol}>
            <img src="/CortaAiLogo.png" alt="Corta Ai" className={Styles.logo} />
            <p>
              Plataforma completa para conectar clientes e barbeiros, com agendamento
              simples, rapido e profissional.
            </p>
            <div className={Styles.socialRow}>
              <a href="#" aria-label="Instagram">IG</a>
              <a href="#" aria-label="Facebook">FB</a>
              <a href="#" aria-label="LinkedIn">IN</a>
            </div>
          </div>

          <div className={Styles.linkCol}>
            <h4>Navegacao</h4>
            <ul>
              <li><a href="#">Inicio</a></li>
              <li><a href="#">Sobre Nos</a></li>
              <li><a href="#">Servicos</a></li>
              <li><a href="#">FAQ</a></li>
            </ul>
          </div>

          <div className={Styles.linkCol}>
            <h4>Para Voce</h4>
            <ul>
              <li><a href="#">Agendar Horario</a></li>
              <li><a href="#">Encontrar Barbearias</a></li>
              <li><a href="#">Avaliacoes</a></li>
              <li><a href="#">Suporte</a></li>
            </ul>
          </div>

          <div className={Styles.linkCol}>
            <h4>Profissionais</h4>
            <ul>
              <li><a href="#">Painel de Agenda</a></li>
              <li><a href="#">Gestao de Clientes</a></li>
              <li><a href="#">Metricas</a></li>
              <li><a href="#">Contato Comercial</a></li>
            </ul>
          </div>
        </div>

        <div className={Styles.bottomArea}>
          <p>2026 Corta Ai. Todos os direitos reservados.</p>
          <div className={Styles.legalLinks}>
            <a href="#">Privacidade</a>
            <a href="#">Termos de Uso</a>
          </div>
        </div>
      </div>
    </footer>
  )
}

export default Footer
