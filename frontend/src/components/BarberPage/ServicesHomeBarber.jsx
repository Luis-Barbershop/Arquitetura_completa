import { useCallback, useEffect, useMemo, useState } from 'react';
import { getMyServices } from '../../services/barbershopService';
import styles from './CSS/ServicesHomeBarber.module.css';

function ServicesHomeBarber({ onNavigateToServices }) {
    const [services, setServices] = useState([]);
    const [loading, setLoading] = useState(true);

    const loadServices = useCallback(async () => {
        try {
            const data = await getMyServices();
            setServices(Array.isArray(data) ? data : []);
        } catch (error) {
            console.error('Erro ao carregar serviços da home:', error);
            setServices([]);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadServices();
    }, [loadServices]);

    const highlightedServices = useMemo(() => services.slice(0, 3), [services]);

  return (
    <div className={styles.container}>
        <div className={styles.header}>
            <h2 className={styles.title}>Serviços</h2>
                        <button type="button" className={styles.seeMoreButton} onClick={onNavigateToServices}>Ver todos</button>
        </div>

                {loading ? (
                    <p className={styles.statusText}>Carregando serviços...</p>
                ) : highlightedServices.length ? (
                    <div className={styles.cardsRow}>
                            {highlightedServices.map((service) => (
                                    <div key={service.id} className={styles.card}>
                                            <h3 className={styles.cardName}>{service.activityName}</h3>
                                            <span className={styles.cardDuration}>{service.durationMinutes} min</span>
                                            <span className={styles.cardPrice}>
                                                R$ {Number(service.price || 0).toFixed(2).replace('.', ',')}
                                            </span>
                                    </div>
                            ))}
                    </div>
                ) : (
                    <p className={styles.statusText}>Você ainda não cadastrou serviços.</p>
                )}
    </div>
    );
}

export default ServicesHomeBarber;