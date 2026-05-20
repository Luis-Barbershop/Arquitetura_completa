import Styles from './BannerSite.module.css'

function BannerSite() {
  return (
    <div id="inicio" className={Styles.banner}>
        <div className={Styles.bannerContent}>
            <p className={Styles.badge}>Agenda e gestão para barbearias modernas</p>
            <h1>SEU NEGOCIO EM ALTA, SUA ROTINA NO CONTROLE.</h1>
            <p>Conecte clientes, equipe e operação em uma única plataforma com foco em produtividade e experiência premium.</p>

            <div className={Styles.actions}>
              <a href="/identificacao" className={Styles.primaryAction}>Comecar agora</a>
              <a href="#como-funciona" className={Styles.secondaryAction}>Ver como funciona</a>
            </div>

            <div className={Styles.quickStats}>
              <div>
                <strong>+12k</strong>
                <span>agendamentos concluídos</span>
              </div>
              <div>
                <strong>4.9</strong>
                <span>avaliação media</span>
              </div>
              <div>
                <strong>24h</strong>
                <span>agenda sempre acessível</span>
              </div>
            </div>
        </div>
    </div>
  )
}

export default BannerSite