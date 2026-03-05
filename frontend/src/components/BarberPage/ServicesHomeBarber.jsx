import styles from "./CSS/ServicesHomeBarber.module.css"

const sampleServices = [
    { id: 1, name: 'Corte Masculino', price: 'R$ 35,00', duration: '30 min' },
    { id: 2, name: 'Barba', price: 'R$ 25,00', duration: '20 min' },
    { id: 3, name: 'Corte + Barba', price: 'R$ 55,00', duration: '50 min' },
];

function ServicesHomeBarber() {
  return (
    <div className={styles.container}>
        <div className={styles.header}>
            <h2 className={styles.title}>Serviços</h2>
            <button className={styles.seeMoreButton}>Ver Todos →</button>
        </div>

        <div className={styles.cardsRow}>
            {sampleServices.map(service => (
                <div key={service.id} className={styles.card}>
                    <h3 className={styles.cardName}>{service.name}</h3>
                    <span className={styles.cardDuration}>{service.duration}</span>
                    <span className={styles.cardPrice}>{service.price}</span>
                </div>
            ))}
        </div>
    </div>
  )
}

export default ServicesHomeBarber