import Styles from './CTAStats.module.css'

const stats = [
    { value: '12k+', label: 'Cortes Finalizados' },
    { value: '4.9', label: 'Avaliacao Google' },
    { value: '67%', label: 'Aproveitamento de Tempo' },
    { value: '+10', label: 'Barbeiros Ativos' },
]

function CTAStats() {
    return (
        <section className={Styles.ctaSection}>
            <div className={Styles.contentGrid}>

                <div className={Styles.rightContent}>
                    <h2>Pronto para elevar o nível da sua rotina?</h2>
                    <p>
                        Junte-se a milhares de clientes e barbearias que já transformaram a forma
                        de agendar beleza.
                    </p>
                    <button type="button">Começar Agora</button>
                </div>

                <div className={Styles.leftStats}>
                    {stats.map((item) => (
                        <article className={Styles.statCard} key={item.label}>
                            <h3>{item.value}</h3>
                            <p>{item.label}</p>
                        </article>
                    ))}
                </div>

                
            </div>
        </section>
    )
}

export default CTAStats
