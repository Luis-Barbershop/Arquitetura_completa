import Styles from './BannerSite.module.css'

function BannerSite() {
  return (
    <div className={Styles.banner}>
        <div className={Styles.bannerContent}>
            
            <h1>TUDO O QUE VOCÊ PRECISA, EM UM SÓ LUGAR</h1>
                <p>Agendar, atender e gerenciar nunca foi tão fácil!</p>
        </div>
    </div>
  )
}

export default BannerSite