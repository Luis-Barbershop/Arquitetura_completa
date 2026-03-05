import Styles from './CSS/NextScheduling.module.css'

function NextScheduling() {
    return (
        <div className={Styles.containerNextScheduling}>
            <div className={Styles.nextSchedulingTitleContent}>
                <h3>Próximos Agendamentos</h3>
                <h5>Ver Todos →</h5>
            </div>

            <div className={Styles.nextSchedulingbackground}>
                <div className={Styles.nextScheduling}>

                    <div className={Styles.nextSchedulingHour}>
                        <h2>09:00</h2>
                    </div>

                    <div className={Styles.nextSchedulingInfo}>
                        <h2>João Silva</h2>
                        <p>Corte Masculino</p>
                    </div>

                    <div className={Styles.nextSchedulingActions}>
                        <button className={Styles.ButtonActions}>...</button>
                    </div>
                </div>
            </div>

             <div className={Styles.nextSchedulingbackground}>
                <div className={Styles.nextScheduling}>

                    <div className={Styles.nextSchedulingHour}>
                        <h2>12:00</h2>
                    </div>

                    <div className={Styles.nextSchedulingInfo}>
                        <h2>Matheus Daleffi</h2>
                        <p>Barba Terapia</p>
                    </div>

                    <div className={Styles.nextSchedulingActions}>
                        <button className={Styles.ButtonActions}>...</button>
                    </div>
                </div>
            </div>
             <div className={Styles.nextSchedulingbackground}>
                <div className={Styles.nextScheduling}>

                    <div className={Styles.nextSchedulingHour}>
                        <h2>17:00</h2>
                    </div>

                    <div className={Styles.nextSchedulingInfo}>
                        <h2>Pedro Manuel</h2>
                        <p>Corte Social</p>
                    </div>

                    <div className={Styles.nextSchedulingActions}>
                        <button className={Styles.ButtonActions}>...</button>
                    </div>
                </div>
            </div>

        </div>
    )
}

export default NextScheduling