import { useEffect, useMemo, useState } from 'react';
import api from '../../services/api';
import Styles from './CSS/NextScheduling.module.css'

function NextScheduling({ onViewAll }) {
    const [appointments, setAppointments] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadUpcomingAppointments = async () => {
            try {
                setLoading(true);
                const response = await api.get('/appointments/my-appointments');
                const items = Array.isArray(response.data) ? response.data : [];
                const now = new Date();
                const activeStatuses = ['SCHEDULED', 'CONFIRMED', 'IN_PROGRESS'];

                const upcoming = items
                    .filter((item) => {
                        const start = item?.startTime ? new Date(item.startTime) : null;
                        if (!start || Number.isNaN(start.getTime())) return false;
                        return start >= now && activeStatuses.includes(String(item?.status || '').toUpperCase());
                    })
                    .sort((a, b) => new Date(a.startTime) - new Date(b.startTime))
                    .slice(0, 3);

                setAppointments(upcoming);
            } catch (error) {
                console.error('Erro ao carregar proximos agendamentos:', error);
                setAppointments([]);
            } finally {
                setLoading(false);
            }
        };

        loadUpcomingAppointments();
    }, []);

    const toHour = (dateString) => {
        const date = new Date(dateString);
        if (Number.isNaN(date.getTime())) return '--:--';
        return date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    };

    const toServiceSummary = (appointment) => {
        if (Array.isArray(appointment?.activities) && appointment.activities.length > 0) {
            return appointment.activities.map((item) => item.activityName).filter(Boolean).join(', ');
        }
        if (Array.isArray(appointment?.activityNames) && appointment.activityNames.length > 0) {
            return appointment.activityNames.join(', ');
        }
        return 'Servico';
    };

    const hasData = useMemo(() => appointments.length > 0, [appointments]);

    return (
        <div className={Styles.containerNextScheduling}>
            <div className={Styles.nextSchedulingTitleContent}>
                <h3>Próximos Agendamentos</h3>
                <h5 onClick={onViewAll}>Ver Todos</h5>
            </div>

            {loading ? (
                <div className={Styles.nextSchedulingbackground}>
                    <div className={Styles.nextScheduling}>
                        <div className={Styles.nextSchedulingInfo}>
                            <h2>Carregando agenda...</h2>
                            <p>Aguarde alguns segundos</p>
                        </div>
                    </div>
                </div>
            ) : hasData ? (
                appointments.map((appointment) => (
                    <div className={Styles.nextSchedulingbackground} key={appointment.id}>
                        <div className={Styles.nextScheduling}>
                            <div className={Styles.nextSchedulingHour}>
                                <h2>{toHour(appointment.startTime)}</h2>
                            </div>

                            <div className={Styles.nextSchedulingInfo}>
                                <h2>{appointment.customerName || appointment.barberName || 'Agendamento'}</h2>
                                <p>{toServiceSummary(appointment)}</p>
                            </div>

                            <div className={Styles.nextSchedulingActions}>
                                <button className={Styles.ButtonActions} onClick={onViewAll}>...</button>
                            </div>
                        </div>
                    </div>
                ))
            ) : (
                <div className={Styles.nextSchedulingbackground}>
                    <div className={Styles.nextScheduling}>
                        <div className={Styles.nextSchedulingInfo}>
                            <h2>Sem proximos agendamentos</h2>
                            <p>Novos atendimentos aparecerao aqui</p>
                        </div>
                    </div>
                </div>
            )}

        </div>
    )
}

export default NextScheduling