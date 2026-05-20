import Styles from './CSS/Site.module.css'
import HeaderSite from '../components/Site/Header/index'
import BannerSite from '../components/Site/Banner/index'
import Services from '../components/Site/Services/index'
import AboutUs from '../components/Site/AboutUs/index'
import Tutorial from '../components/Site/Tutorial/index'
import Mockup from '../components/Site/Mockup/index'
import Faq from '../components/Site/Faq/index'
import CTAStats from '../components/Site/CTAStats/index'
import Footer from '../components/Site/Footer/index'

function Site() {
  return (
    <div className={Styles.site}>
      <HeaderSite />
      <BannerSite />
      <AboutUs />
      <Tutorial/>
      <Services />
      <Mockup />
      <Faq />
      <CTAStats />
      <Footer />
      
    </div>
  )
}

export default Site