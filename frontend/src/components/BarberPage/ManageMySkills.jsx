import React, { useEffect, useState } from 'react';
import { getShopServices, getMyAssignedActivities, assignActivities } from '../../services/barbershopService';
import styles from './CSS/ManageMySkills.module.css';

const normalizeServiceId = (id) => String(id || '').trim().toLowerCase();

const ManageMySkills = ({ shopId, refreshKey = 0 }) => {
    const [shopServices, setShopServices] = useState([]);
    const [myServicesIds, setMyServicesIds] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    useEffect(() => {
        const loadData = async () => {
            try {
                setLoading(true);
                setErrorMessage('');
                setSuccessMessage('');
                const [allServices, myActivities] = await Promise.all([
                    getShopServices(shopId),
                    getMyAssignedActivities()
                ]);

                setShopServices(allServices);
                // O backend retorna Set<UUID> (array de strings), não objetos ActivityDTO
                setMyServicesIds((myActivities || []).map((id) => normalizeServiceId(id)).filter(Boolean));
            } catch (error) {
                console.error("Erro ao carregar habilidades:", error);
                setErrorMessage('Nao foi possivel carregar os servicos do perfil.');
            } finally {
                setLoading(false);
            }
        };

        if (shopId) loadData();
    }, [shopId, refreshKey]);

    const handleToggle = (serviceId) => {
        const normalizedId = normalizeServiceId(serviceId);
        if (!normalizedId) return;

        setMyServicesIds(prev =>
            prev.includes(normalizedId)
                ? prev.filter(id => id !== normalizedId)
                : [...prev, normalizedId]
        );
    };

    const handleSave = async () => {
        try {
            setSaving(true);
            setErrorMessage('');
            setSuccessMessage('');
            await assignActivities(myServicesIds);
            setSuccessMessage('Habilidades atualizadas com sucesso!');
        } catch (error) {
            console.error("Erro ao salvar habilidades:", error);
            setErrorMessage('Erro ao salvar habilidades. Tente novamente.');
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className={styles.container}>
            {!!errorMessage && <p className={styles.errorText}>{errorMessage}</p>}
            {!!successMessage && <p className={styles.successText}>{successMessage}</p>}

            {loading ? (
                <p className={styles.loadingText}>Carregando...</p>
            ) : (
                <>
                    <div className={styles.metaRow}>
                        <span>{myServicesIds.length} selecionados</span>
                        <span>{shopServices.length} disponiveis</span>
                    </div>

                    <div className={styles.grid}>
                        {shopServices.map(service => {
                            const normalizedServiceId = normalizeServiceId(service.id);
                            const isSelected = myServicesIds.includes(normalizedServiceId);
                            return (
                                <button
                                    key={service.id}
                                    type="button"
                                    onClick={() => handleToggle(normalizedServiceId)}
                                    className={isSelected ? styles.serviceCardSelected : styles.serviceCard}
                                >
                                    <div className={styles.cardContent}>
                                        <strong>{service.activityName}</strong>
                                        <span className={isSelected ? styles.cardStatusSelected : styles.cardStatus}>
                                            {isSelected ? 'Selecionado' : 'Selecionar'}
                                        </span>
                                    </div>
                                    <span className={styles.servicePrice}>R$ {service.price.toFixed(2)}</span>
                                </button>
                            );
                        })}
                    </div>
                </>
            )}

            {!loading && shopServices.length === 0 && (
                <p className={styles.loadingText}>Nao ha servicos cadastrados na barbearia ainda.</p>
            )}

            <button onClick={handleSave} className={styles.saveButton} disabled={saving || loading}>
                {saving ? 'Salvando...' : 'Salvar Minhas Habilidades'}
            </button>
        </div>
    );
};

export default ManageMySkills;