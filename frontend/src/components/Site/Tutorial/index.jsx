import Style from './Tutorial.module.css'

function Tutorial() {
  return (
    <div className={Style.Container}>
      <div className={Style.header}>
      <p className={Style.title}>O FUTURO DA BAREARIA</p>
      <h2>Simplicidade em Cada <span style={{color: '#c19006', fontStyle: 'italic'}}>Movimento.</span></h2>

      <p className={Style.description}>Desenvolvemos uma experiência fluida tanto para quem senta na cadeira quanto para quem segura a tesoura. Escolha seu Caminho abaixo.</p>
      </div>

      <div className={Style.Users}>
        <div className={Style.UserOption}>
          <span className={Style.Line}></span>
          <p>PARA VOCÊ</p>
          <span className={Style.Line}></span>
        </div>
        <div className={Style.UserOption}>
          <span className={Style.Line}></span>
          <p>PARA PROFSSIONAIS</p>
          <span className={Style.Line}></span>
        </div>
      </div>
    </div>

  )
}

export default Tutorial