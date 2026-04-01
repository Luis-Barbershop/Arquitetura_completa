import Styles from './CSS/Buttonsbarber.module.css'

function Buttonsbarber() {
  return (
    <div className={Styles.containerButtons}>
        <button className={Styles.button}>
            <div>
                <div className={Styles.iconReport}>
                    <img src="/Icons/bar.png" alt="Barrinhas" />
                </div>
                <h3>Relatórios</h3>
            </div>
        </button>

        <button className={Styles.button}>
            <div>
                <div className={Styles.iconscheduling}>
                    <img src="/Icons/plusIcon.png" alt="" />
                </div>
                <h3>Novos Agendamentos</h3>
            </div>
        </button>

        {/* <button className={Styles.button}>Gerenciar Habilidades</button>
        <button className={Styles.button}>Faturamento</button> */}
    </div>
  )
}

export default Buttonsbarber