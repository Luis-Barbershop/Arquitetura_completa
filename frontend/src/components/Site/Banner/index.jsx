import Styles from './BannerSite.module.css'

function BannerSite() {
  return (
    <div id="inicio" className={Styles.banner}>
        <div className={Styles.bannerContent}>
            <p className={Styles.badge}>Agenda e gestao para barbearias modernas</p>
            <h1>SEU NEGOCIO EM ALTA, SUA ROTINA NO CONTROLE.</h1>
            <p>Conecte clientes, equipe e operacao em uma unica plataforma com foco em produtividade e experiencia premium.</p>

            <div className={Styles.actions}>
              <a href="/identificacao" className={Styles.primaryAction}>Comecar agora</a>
              <a href="#como-funciona" className={Styles.secondaryAction}>Ver como funciona</a>
            </div>

            <div className={Styles.quickStats}>
              <div>
                <strong>+12k</strong>
                <span>agendamentos concluidos</span>
              </div>
              <div>
                <strong>4.9</strong>
                <span>avaliacao media</span>
              </div>
              <div>
                <strong>24h</strong>
                <span>agenda sempre acessivel</span>
              </div>
            </div>
        </div>
    </div>
  )
}

export default BannerSite