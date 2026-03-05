import React, { useEffect, useState } from 'react';
import { getShopServices, getMyAssignedActivities, assignActivities } from '../../services/barbershopService';
import styles from './CSS/ManageMySkills.module.css';

const ManageMySkills = ({ shopId }) => {
    const [shopServices, setShopServices] = useState([]);
    const [myServicesIds, setMyServicesIds] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadData = async () => {
            try {
                const [allServices, myActivities] = await Promise.all([
                    getShopServices(shopId),
                    getMyAssignedActivities()
                ]);

                setShopServices(allServices);
                setMyServicesIds(myActivities.map(a => a.id));
                setLoading(false);
            } catch (error) {
                console.error("Erro ao carregar habilidades:", error);
            }
        };

        if (shopId) loadData();
    }, [shopId]);

    const handleToggle = (serviceId) => {
        setMyServicesIds(prev =>
            prev.includes(serviceId)
                ? prev.filter(id => id !== serviceId)
                : [...prev, serviceId]
        );
    };

    const handleSave = async () => {
        try {
            await assignActivities(myServicesIds);
            alert("Habilidades atualizadas com sucesso!");
        } catch (error) {
            alert("Erro ao salvar habilidades.");
        }
    };

    return (
        <div className={styles.container}>
            <h2 className={styles.title}>Meus Serviços</h2>
            <p className={styles.subtitle}>Selecione quais serviços desta barbearia você realiza:</p>

            {loading ? (
                <p className={styles.loadingText}>Carregando...</p>
            ) : (
                <div className={styles.grid}>
                    {shopServices.map(service => {
                        const isSelected = myServicesIds.includes(service.id);
                        return (
                            <div
                                key={service.id}
                                onClick={() => handleToggle(service.id)}
                                className={isSelected ? styles.serviceCardSelected : styles.serviceCard}
                            >
                                <div className={styles.cardContent}>
                                    <strong>{service.activityName}</strong>
                                    {isSelected && <span className={styles.checkMark}>✓</span>}
                                </div>
                                <span className={styles.servicePrice}>R$ {service.price.toFixed(2)}</span>
                            </div>
                        );
                    })}
                </div>
            )}

            <button onClick={handleSave} className={styles.saveButton}>
                Salvar Minhas Habilidades
            </button>
        </div>
    );
};

export default ManageMySkills;