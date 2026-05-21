import Style from './Tutorial.module.css'

function Tutorial() {
  return (
    <div id="como-funciona" className={Style.Container}>
      <div className={Style.header}>
        <p className={Style.title}>O FUTURO DA BAREARIA</p>
        <h2>Simplicidade em Cada <span className={Style.highlight}>Movimento.</span></h2>

        <p className={Style.description}>Desenvolvemos uma experiência fluida tanto para quem senta na cadeira quanto para quem segura a tesoura. Escolha seu Caminho abaixo.</p>
      </div>

      
        
     

      <div className={Style.Content}>
        <div className={Style.ContentOption1}>

          <div className={Style.Users}>
        <div className={Style.UserOption}>
          {/* <span className={Style.Line}></span> */}
          <p>PARA VOCÊ</p>
          {/* <span className={Style.Line}></span> */}
        </div>
       </div>

          <div className={Style.ContentOption1Container}>
            <div className={Style.ContentImage}>
              <img src="/Icons/Compass.png" alt="Bússula" />
            </div>
            <div className={Style.ContentText}>
              <p className={Style.ContentStep}>PASSO 01</p>
              <h3>Encontre a Sua Barbearia.</h3>
              <p className={Style.ContentDescription}>Localize os Melhores Profissionais Perto de Você através do nosso sistema inteligente.</p>
            </div>
          </div>

          <div className={Style.ContentOption1Container}>
            <div className={Style.ContentImage}>
              <img src="/Icons/scissors_icon.png" alt="Tesoura" />
            </div>
            <div className={Style.ContentText}>
              <p className={Style.ContentStep}>PASSO 02</p>
              <h3>Escolha o serviço e o profissional</h3>
              <p className={Style.ContentDescription}>Navegue pelo portfólio, preços e especialidades de cada mestre artesão.</p>
            </div>
          </div>

          <div className={Style.ContentOption1Container}>
            <div className={Style.ContentImage}>
              <img src="/Icons/Compass.png" alt="Bússula" />
            </div>
            <div className={Style.ContentText}>
              <p className={Style.ContentStep}>PASSO 03</p>
              <h3>Agende o seu horário em segundos.</h3>
              <p className={Style.ContentDescription}>Sem ligações. Sem Espera. Apenas alguns toques e seu lugar está garantido.</p>
            </div>
          </div>


          <div className={Style.ContentOption1Container}>
            <div className={Style.ContentImageFinal}>
              <img src="/Icons/Check.png" alt="Bússula" />
            </div>
            <div className={Style.ContentText}>
              <p className={Style.ContentStep}>PASSO 04</p>
              <h3>Pronto!</h3>
              <p className={Style.ContentDescription}>Receba um lembrete automático via whatsapp antes do seu agendamento e vá renovar o seu estilo.</p>
            </div>
          </div>

        </div>

        <div className={Style.ContentOption2}>
          <div className={Style.Users}>
        <div className={Style.UserOption} id={Style.UserOption2}>
          {/* <span className={Style.Line}></span> */}
          <p>PARA PROFISSIONAIS</p>
          {/* <span className={Style.Line}></span> */}
        </div>
        </div>
           <div className={Style.ContentOption1Container}>
            <div className={Style.ContentImage}>
              <img src="/Icons/Store.png" alt="Loja" />
            </div>
            <div className={Style.ContentText}>
              <p className={Style.ContentStep}>PASSO 01</p>
              <h3>Cadastre a Sua Barbearia e equipe.</h3>
              <p className={Style.ContentDescription}>Crie o seu perfil digital em minutos e destaque a identidade do seu negócio.</p>
            </div>
          </div>

          <div className={Style.ContentOption1Container}>
            <div className={Style.ContentImage}>
              <img src="/Icons/Settings.png" alt="Engrenagem" />
            </div>
            <div className={Style.ContentText}>
              <p className={Style.ContentStep}>PASSO 02</p>
              <h3>Configure seus serviços e Horários.</h3>
              <p className={Style.ContentDescription}>Navegue pelo portfólio, preços e especialidades de cada mestre artesão.</p>
            </div>
          </div>

          <div className={Style.ContentOption1Container}>
            <div className={Style.ContentImage}>
              <img src="/Icons/Box.png" alt="Caixa de Armazenamento" />
            </div>
            <div className={Style.ContentText}>
              <p className={Style.ContentStep}>PASSO 03</p>
              <h3>Gerencie agendamentos e estoque.</h3>
              <p className={Style.ContentDescription}>Controle total da sua operação em uma interface limpa e sem distrações.</p>
            </div>
          </div>


          <div className={Style.ContentOption1Container}>
            <div className={Style.ContentImageFinal}>
              <img src="/Icons/GraphicsInsights.png" alt="Insights Gráficos" />
            </div>
            <div className={Style.ContentText}>
              <p className={Style.ContentStep}>PASSO 04</p>
              <h3>Acompanhe o seu Crescimento.</h3>
              <p className={Style.ContentDescription}>Dashboards inteligentes que mostram sua evolução e faturamento em tempo real.</p>
            </div>
          </div> 
        </div>
      </div>
    </div>

  )
}

export default Tutorial