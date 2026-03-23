import Styles from './CSS/Site.module.css'
import HeaderSite from '../components/Site/Header/index'
import BannerSite from '../components/Site/Banner/index'
import Services from '../components/Site/Services/index'
import AboutUs from '../components/Site/AboutUs/index'

function Site() {
  return (
    <div className={Styles.site}>
      <HeaderSite />
      <BannerSite />
      <AboutUs />
      <Services />
    </div>
  )
}

export default Site