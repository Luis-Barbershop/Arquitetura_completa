import Styles from './CSS/ActionsBarber.module.css'
import DailyInsights from './DailyInsights'
import Stock from './Stock'
 

function ActionsBarber({ onNavigateToStock }) {
  return (
    <div className={Styles.ActionsBarber}>
        <div className={Styles.ActionsBarberTitle}>
        <h3>Ações Importantes</h3>
        </div>
        <div className={Styles.ActionsBarberContent}>
       <Stock onNavigateToStock={onNavigateToStock} />
       <DailyInsights/>
        </div>
    </div>
  )
}

export default ActionsBarber