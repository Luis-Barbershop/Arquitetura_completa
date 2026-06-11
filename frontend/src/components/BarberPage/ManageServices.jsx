import React, { useEffect, useState } from 'react';
import { getMyServices, createService, deleteService } from '../../services/barbershopService';
import styles from './CSS/ManageServices.module.css';

const ManageServices = () => {
    const [services, setServices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isOwner, setIsOwner] = useState(false);

    const [name, setName] = useState("");
    const [price, setPrice] = useState("");
    const [duration, setDuration] = useState("30");

    const loadServices = async () => {
        try {
            const data = await getMyServices();
            setServices(data);

            const userRaw = localStorage.getItem('user');
            const userData = userRaw ? JSON.parse(userRaw) : null;
            setIsOwner(Boolean(userData?.isOwner ?? userData?.owner));
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadServices();
    }, []);

    const handleAdd = async (e) => {
        e.preventDefault();
        if (!name || !price || !duration) return;

        if (!isOwner) {
            alert('Apenas o dono da barbearia pode cadastrar serviços.');
            return;
        }

        const parsedPrice = Number(String(price).replace(',', '.').trim());
        const parsedDuration = Number(duration);

        if (!Number.isFinite(parsedPrice) || parsedPrice <= 0) {
            alert('Informe um preço valido maior que zero.');
            return;
        }

        if (!Number.isFinite(parsedDuration) || parsedDuration <= 0) {
            alert('Informe uma duração valida em minutos.');
            return;
        }

        try {
            await createService({
                activityName: name,
                price: parsedPrice,
                durationMinutes: parsedDuration
            });

            alert("Serviço adicionado!");
            setName("");
            setPrice("");
            loadServices();
        } catch (error) {
            const status = error?.response?.status;
            if (status === 403) {
                alert('Seu usuário não tem permissão ativa de dono no token atual. Faça logout e login novamente.');
            } else {
                alert("Erro ao criar serviço.");
            }
        }
    };

    const handleDelete = async (id) => {
        if (window.confirm("Tem certeza que deseja excluir este serviço?")) {
            try {
                await deleteService(id);
                loadServices();
            } catch {
                alert("Erro ao excluir.");
            }
        }
    };

    return (
        <div className={styles.container}>
            <h2 className={styles.title}>Gerenciar Serviços</h2>

            <div style={{ marginBottom: '30px' }}>
                {loading ? (
                    <p className={styles.loadingText}>Carregando...</p>
                ) : services.length > 0 ? (
                    <ul className={styles.serviceList}>
                        {services.map(s => (
                            <li key={s.id} className={styles.serviceItem}>
                                <div>
                                    <span className={styles.serviceName}>{s.activityName}</span>
                                    <span className={styles.serviceDuration}>({s.durationMinutes} min)</span>
                                </div>
                                <div>
                                    <span className={styles.servicePrice}>R$ {s.price.toFixed(2)}</span>
                                    <button onClick={() => handleDelete(s.id)} className={styles.deleteButton}>
                                        Excluir
                                    </button>
                                </div>
                            </li>
                        ))}
                    </ul>
                ) : (
                    <p className={styles.emptyText}>Nenhum serviço cadastrado. Adicione o primeiro abaixo!</p>
                )}
            </div>

            <form onSubmit={handleAdd} className={styles.form}>
                {!isOwner && (
                    <p className={styles.emptyText}>
                        Apenas o dono pode cadastrar serviços. Se você acabou de criar a barbearia,
                        faça logout e login para atualizar o token.
                    </p>
                )}
                <div className={styles.formGroupName}>
                    <label className={styles.formLabel}>Nome do Serviço</label>
                    <input
                        type="text"
                        placeholder="Ex: Corte Degradê"
                        value={name}
                        onChange={e => setName(e.target.value)}
                        className={styles.formInput}
                        disabled={!isOwner}
                        required
                    />
                </div>

                <div className={styles.formGroupPrice}>
                    <label className={styles.formLabel}>Preço (R$)</label>
                    <input
                        type="number"
                        placeholder="0.00"
                        step="0.01"
                        value={price}
                        onChange={e => setPrice(e.target.value)}
                        className={styles.formInput}
                        disabled={!isOwner}
                        required
                    />
                </div>

                <div className={styles.formGroupDuration}>
                    <label className={styles.formLabel}>Duração (min)</label>
                    <input
                        type="number"
                        placeholder="30"
                        value={duration}
                        onChange={e => setDuration(e.target.value)}
                        className={styles.formInput}
                        disabled={!isOwner}
                        required
                    />
                </div>

                <button type="submit" className={styles.addButton} disabled={!isOwner}>
                    + Adicionar
                </button>
            </form>
        </div>
    );
};

export default ManageServices;