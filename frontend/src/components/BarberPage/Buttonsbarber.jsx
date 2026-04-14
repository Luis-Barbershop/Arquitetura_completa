import Styles from './CSS/Buttonsbarber.module.css'

function Buttonsbarber({ onReportsClick, onMyBookingsClick }) {
  return (
    <div className={Styles.containerButtons}>
        <button type="button" className={Styles.button} onClick={onReportsClick}>
            <div>
                <div className={Styles.iconReport}>
                    <img src="/Icons/bar.png" alt="Barrinhas" />
                </div>
                <h3>Relatórios</h3>
            </div>
        </button>

        <button type="button" className={Styles.button} onClick={onMyBookingsClick}>
            <div>
                <div className={Styles.iconscheduling}>
                    <img src="/Icons/plusIcon.png" alt="Agenda" />
                </div>
                <h3>Meus Agendamentos</h3>
            </div>
        </button>

        {/* <button className={Styles.button}>Gerenciar Habilidades</button>
        <button className={Styles.button}>Faturamento</button> */}
    </div>
  )
}

export default Buttonsbarber