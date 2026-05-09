import React, { useEffect, useState, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../services/api';
import {
    getMyInvites,
    acceptInvite,
    rejectInvite,
    leaveShop,
    getMyWorkSchedule,
    saveMyWorkSchedule,
    getBarbershopById,
    updateMyBarbershop,
    uploadMyBarbershopLogo,
    uploadMyBarbershopBanner,
    geocodeAddress,
} from '../services/barbershopService';
import { logoutUser } from '../services/authService';
import { isCustomer } from '../services/userContext';
import { maskCpf, maskPhone, onlyDigits } from '../utils/inputMasks';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import CropImageModal from '../components/CropImageModal/CropImageModal';
import { uploadBarberProfilePhoto } from '../services/userProfileService';
import styles from './CSS/BarberHomePage.module.css';

const DAYS_OF_WEEK = [
    { key: 'MONDAY',    label: 'Seg' },
    { key: 'TUESDAY',   label: 'Ter' },
    { key: 'WEDNESDAY', label: 'Qua' },
    { key: 'THURSDAY',  label: 'Qui' },
    { key: 'FRIDAY',    label: 'Sex' },
    { key: 'SATURDAY',  label: 'Sáb' },
    { key: 'SUNDAY',    label: 'Dom' },
];

const EMPTY_BLOCK = { startTime: '', endTime: '' };

function formatPhoneForProfile(value) {
    const digits = onlyDigits(value);
    if (!digits) return '—';
    return maskPhone(digits);
}

function formatCpfForProfile(value) {
    const digits = onlyDigits(value);
    if (!digits) return '—';
    return maskCpf(digits);
}

/* ── Componente seletor de horário HH:MM ────────────────────────────────── */
function TimePicker({ value, onChange, disabled }) {
    const safeValue = value && value.length >= 5 ? value.substring(0, 5) : '';
    return (
        <div className={styles.timePickerWrapper}>
            <input
                type="time"
                step={60}
                value={safeValue}
                onChange={(e) => onChange(e.target.value)}
                disabled={disabled}
                className={`${styles.timePickerInput} ${disabled ? styles.timePickerInputDisabled : ''}`}
            />
        </div>
    );
}

/**
 * Página de Perfil do Barbeiro — exibe e permite editar dados pessoais e horário de trabalho.
 * Disponível para: Barbeiro colaborador, Owner e qualquer barbeiro (com ou sem barbearia).
 */
function BarberProfilePage() {
    const navigate = useNavigate();
    const [barber, setBarber] = useState(null);
    const [loading, setLoading] = useState(true);
    const profilePhotoInputRef = useRef(null);
    const logoInputRef = useRef(null);
    const bannerInputRef = useRef(null);
    const cropObjectUrlRef = useRef(null);

    // ── actAsBarber toggle ─────────────────────────────────────────────────────
    const [actAsBarber, setActAsBarber] = useState(true);
    const [savingActAsBarber, setSavingActAsBarber] = useState(false);

    // ── Horário de trabalho (multi-bloco por dia) ──────────────────────────────
    // weekSchedule: { MONDAY: [{startTime:'09:00', endTime:'12:00'}, ...], ... }
    const [weekSchedule, setWeekSchedule] = useState({});
    const [savingSchedule, setSavingSchedule] = useState(false);
    const [loadingSchedule, setLoadingSchedule] = useState(false);

    // ── Copiar horário de um dia para outros ───────────────────────────────────
    const [copySource, setCopySource] = useState(null);
    const [copyTargets, setCopyTargets] = useState([]);
    const [isCopyModalOpen, setIsCopyModalOpen] = useState(false);

    // ── Convites pendentes (barbeiro sem barbearia) ────────────────────────────
    const [pendingInvites, setPendingInvites] = useState([]);
    const [loadingInvites, setLoadingInvites] = useState(false);
    const [invitesError, setInvitesError] = useState(false);
    const [inviteActionLoading, setInviteActionLoading] = useState(null);
    const [leavingShop, setLeavingShop] = useState(false);
    const [uploadingProfilePhoto, setUploadingProfilePhoto] = useState(false);

    const [loadingBarbershopInfo, setLoadingBarbershopInfo] = useState(false);
    const [savingBarbershopInfo, setSavingBarbershopInfo] = useState(false);
    const [uploadingLogo, setUploadingLogo] = useState(false);
    const [uploadingBanner, setUploadingBanner] = useState(false);
    const [cropModal, setCropModal] = useState(null);
    const [barbershopForm, setBarbershopForm] = useState({ name: '', address: '', latitude: null, longitude: null });
    const [barbershopMedia, setBarbershopMedia] = useState({ logoUrl: '', bannerUrl: '' });

    useEffect(() => {
        if (isCustomer()) { navigate('/homepage', { replace: true }); return; }
        const token = localStorage.getItem('token');
        if (!token) { navigate('/', { replace: true }); return; }

        api.get('/auth/me')
            .then(res => {
                const data = res.data;
                // Se o backend não retornar imageUrl, usa o cache do localStorage
                if (!data.imageUrl) {
                    const cached = localStorage.getItem('userProfileImage');
                    if (cached) data.imageUrl = cached;
                }
                setBarber(data);
                setActAsBarber(data?.actAsBarber ?? true);
                if (data?.barbershopName) localStorage.setItem('barbershopName', data.barbershopName);
                if (data?.barbershopId) localStorage.setItem('barbershopId', String(data.barbershopId));
                if (data?.imageUrl) localStorage.setItem('userProfileImage', data.imageUrl);
                setLoading(false);
            })
            .catch(() => { setLoading(false); navigate('/'); });
    }, [navigate]);

    useEffect(() => () => {
        if (cropObjectUrlRef.current) {
            URL.revokeObjectURL(cropObjectUrlRef.current);
        }
    }, []);

    // ── Carrega a grade de horários multi-bloco ────────────────────────────────
    useEffect(() => {
        if (!barber) return;
        setLoadingSchedule(true);
        getMyWorkSchedule()
            .then(data => {
                // data = [{ dayOfWeek: 'MONDAY', blocks: [{ startTime: '09:00', endTime: '12:00' }, ...] }, ...]
                const map = {};
                (data || []).forEach(day => {
                    if (day.blocks && day.blocks.length > 0) {
                        map[day.dayOfWeek] = day.blocks.map(b => ({
                            startTime: b.startTime ? b.startTime.substring(0, 5) : '',
                            endTime:   b.endTime   ? b.endTime.substring(0, 5)   : '',
                        }));
                    }
                });
                setWeekSchedule(map);
            })
            .catch(() => {
                // Fallback: se o endpoint falhar, tenta montar a partir do barber legado
                if (barber.workStartTime && barber.workEndTime) {
                    const start = barber.workStartTime.substring(0, 5);
                    const end   = barber.workEndTime.substring(0, 5);
                    const fallback = {};
                    ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'].forEach(d => {
                        fallback[d] = [{ startTime: start, endTime: end }];
                    });
                    setWeekSchedule(fallback);
                }
            })
            .finally(() => setLoadingSchedule(false));
    }, [barber]);

    useEffect(() => {
        if (!barber?.isOwner || !barber?.barbershopId) {
            return;
        }

        const loadBarbershopInfo = async () => {
            setLoadingBarbershopInfo(true);
            try {
                const shop = await getBarbershopById(barber.barbershopId);
                if (!shop) return;

                setBarbershopForm({
                    name: shop.name || '',
                    address: shop.address || '',
                    latitude: shop.latitude ?? null,
                    longitude: shop.longitude ?? null,
                });

                setBarbershopMedia({
                    logoUrl: shop.logoUrl || '',
                    bannerUrl: shop.bannerUrl || '',
                });
            } catch {
                toast.error('Não foi possível carregar os dados da barbearia.');
            } finally {
                setLoadingBarbershopInfo(false);
            }
        };

        loadBarbershopInfo();
    }, [barber?.barbershopId, barber?.isOwner]);

    // ── Funções auxiliares do schedule ──────────────────────────────────────────
    const toggleDay = useCallback((dayKey) => {
        setWeekSchedule(prev => {
            const copy = { ...prev };
            if (copy[dayKey]) {
                delete copy[dayKey];
            } else {
                copy[dayKey] = [{ ...EMPTY_BLOCK }];
            }
            return copy;
        });
    }, []);

    const addBlock = useCallback((dayKey) => {
        setWeekSchedule(prev => ({
            ...prev,
            [dayKey]: [...(prev[dayKey] || []), { ...EMPTY_BLOCK }],
        }));
    }, []);

    const removeBlock = useCallback((dayKey, blockIdx) => {
        setWeekSchedule(prev => {
            const blocks = [...(prev[dayKey] || [])];
            blocks.splice(blockIdx, 1);
            if (blocks.length === 0) {
                const copy = { ...prev };
                delete copy[dayKey];
                return copy;
            }
            return { ...prev, [dayKey]: blocks };
        });
    }, []);

    const updateBlock = useCallback((dayKey, blockIdx, field, value) => {
        setWeekSchedule(prev => {
            const blocks = [...(prev[dayKey] || [])];
            blocks[blockIdx] = { ...blocks[blockIdx], [field]: value };
            return { ...prev, [dayKey]: blocks };
        });
    }, []);

    // ── Copiar horário de um dia para outros ───────────────────────────────────
    const handleOpenCopyModal = (dayKey) => {
        setCopySource(dayKey);
        setCopyTargets([]);
        setIsCopyModalOpen(true);
    };
    const handleCopyConfirm = () => {
        if (!copySource || copyTargets.length === 0) return;
        const srcBlocks = weekSchedule[copySource];
        if (!srcBlocks || srcBlocks.length === 0) { toast.warn('O dia de origem não tem blocos.'); return; }
        setWeekSchedule(prev => {
            const copy = { ...prev };
            copyTargets.forEach(target => { copy[target] = srcBlocks.map(b => ({ ...b })); });
            return copy;
        });
        setIsCopyModalOpen(false);
        toast.success(`Horário copiado para ${copyTargets.length} dia(s)!`);
    };
    const toggleCopyTarget = (dayKey) => {
        setCopyTargets(prev => prev.includes(dayKey) ? prev.filter(k => k !== dayKey) : [...prev, dayKey]);
    };

    // ── Carrega convites pendentes quando barbeiro não está vinculado ───────
    useEffect(() => {
        if (!barber) return;

        if (barber?.barbershopId) {
            setPendingInvites([]);
            setInvitesError(false);
            setLoadingInvites(false);
            return;
        }

        let isMounted = true;

        const fetchInvites = async () => {
            try {
                const data = await getMyInvites();
                if (isMounted) {
                    setPendingInvites(data);
                    setInvitesError(false);
                }
            } catch {
                if (isMounted) {
                    setPendingInvites([]);
                    setInvitesError(true);
                }
            }
        };

        setLoadingInvites(true);
        fetchInvites().finally(() => {
            if (isMounted) setLoadingInvites(false);
        });

        // Polling a cada 30s para captar novos convites sem reload manual
        const interval = setInterval(fetchInvites, 30_000);

        return () => {
            isMounted = false;
            clearInterval(interval);
        };
    }, [barber]);

    const refreshBarberProfileAfterInvite = async () => {
        const maxAttempts = 5;
        for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
            const res = await api.get('/auth/me', { params: { t: Date.now() } });
            const data = res.data;
            setBarber(data);

            if (data?.barbershopId) {
                localStorage.setItem('barbershopId', String(data.barbershopId));
                return true;
            }

            if (attempt < maxAttempts) {
                await new Promise(resolve => setTimeout(resolve, 350));
            }
        }

        return false;
    };

    const handleAcceptInvite = async (requestId) => {
        setInviteActionLoading(requestId);
        try {
            await acceptInvite(requestId);
            toast.success('Convite aceito! Você foi vinculado à barbearia.');
            setPendingInvites(prev => prev.filter(inv => inv.requestId !== requestId));
            const linked = await refreshBarberProfileAfterInvite();

            if (!linked) {
                toast.warn('Convite aceito, mas o vínculo ainda está sincronizando. Atualize a página em alguns segundos.');
            }
        } catch (err) {
            toast.error(err?.response?.data?.message || 'Erro ao aceitar convite.');
        } finally {
            setInviteActionLoading(null);
        }
    };

    const handleRejectInvite = async (requestId) => {
        setInviteActionLoading(requestId);
        try {
            await rejectInvite(requestId);
            toast.info('Convite recusado.');
            setPendingInvites(prev => prev.filter(inv => inv.requestId !== requestId));
        } catch (err) {
            toast.error(err?.response?.data?.message || 'Erro ao recusar convite.');
        } finally {
            setInviteActionLoading(null);
        }
    };

    const handleLeaveShop = async () => {
        if (!barber?.barbershopId || barber?.isOwner) return;

        const confirmed = window.confirm('Deseja sair da barbearia atual? Você perderá o vínculo com o estabelecimento.');
        if (!confirmed) return;

        setLeavingShop(true);
        try {
            await leaveShop();
            toast.success('Você saiu da barbearia com sucesso.');

            const meResponse = await api.get('/auth/me', { params: { t: Date.now() } });
            const updatedBarber = meResponse.data;
            setBarber(updatedBarber);

            if (!updatedBarber?.barbershopId) {
                localStorage.removeItem('barbershopId');
            }

            const invites = await getMyInvites();
            setPendingInvites(invites);
            setInvitesError(false);
        } catch (err) {
            toast.error(err?.response?.data?.message || 'Erro ao sair da barbearia.');
        } finally {
            setLeavingShop(false);
        }
    };

    const handleLogout = async () => {
        await logoutUser();
        navigate('/');
    };

    const closeCropModal = useCallback(() => {
        if (cropObjectUrlRef.current) {
            URL.revokeObjectURL(cropObjectUrlRef.current);
            cropObjectUrlRef.current = null;
        }
        setCropModal(null);
    }, []);

    const openCropModal = useCallback((target, file) => {
        if (cropObjectUrlRef.current) {
            URL.revokeObjectURL(cropObjectUrlRef.current);
        }

        const src = URL.createObjectURL(file);
        cropObjectUrlRef.current = src;
        setCropModal({
            target,
            src,
            fileName: file.name,
        });
    }, []);

    const handleUploadProfilePhoto = async (event) => {
        const file = event.target.files?.[0];
        event.target.value = '';
        if (!file) return;

        openCropModal('profile', file);
    };

    const handleBarbershopFormChange = (event) => {
        const { name, value } = event.target;
        setBarbershopForm(prev => ({ ...prev, [name]: value }));
    };

    const handleSaveBarbershopInfo = async (event) => {
        event.preventDefault();
        setSavingBarbershopInfo(true);

        try {
            const trimmedAddress = barbershopForm.address.trim();
            let lat = barbershopForm.latitude;
            let lng = barbershopForm.longitude;

            if (trimmedAddress) {
                const coords = await geocodeAddress(trimmedAddress);
                if (coords) {
                    lat = coords.lat;
                    lng = coords.lng;
                }
            }

            await updateMyBarbershop({
                name: barbershopForm.name.trim(),
                address: trimmedAddress,
                latitude: lat,
                longitude: lng,
            });
            setBarbershopForm(prev => ({ ...prev, latitude: lat, longitude: lng }));
            localStorage.setItem('barbershopName', barbershopForm.name.trim());
            toast.success('Dados da barbearia atualizados com sucesso!');
        } catch (error) {
            toast.error(error?.response?.data?.message || 'Erro ao atualizar dados da barbearia.');
        } finally {
            setSavingBarbershopInfo(false);
        }
    };

    const handleUploadLogo = async (event) => {
        const file = event.target.files?.[0];
        event.target.value = '';
        if (!file) return;

        openCropModal('logo', file);
    };

    const handleConfirmCrop = async (blob) => {
        if (!cropModal?.target) return;

        const croppedFile = new File(
            [blob],
            cropModal.target === 'profile' ? 'foto-perfil.jpg' : 'logo-barbearia.jpg',
            { type: blob.type || 'image/jpeg' },
        );

        if (cropModal.target === 'profile') {
            setUploadingProfilePhoto(true);
            try {
                const response = await uploadBarberProfilePhoto(croppedFile);
                const imageUrl = typeof response === 'string' ? response : response?.imageUrl;

                if (imageUrl) {
                    setBarber(prev => ({ ...prev, imageUrl }));
                    localStorage.setItem('userProfileImage', imageUrl);
                }

                toast.success('Foto de perfil atualizada!');
                closeCropModal();
            } catch (error) {
                toast.error(error?.response?.data?.message || 'Erro ao enviar foto de perfil.');
            } finally {
                setUploadingProfilePhoto(false);
            }
            return;
        }

        setUploadingLogo(true);
        try {
            const response = await uploadMyBarbershopLogo(croppedFile);
            const logoUrl = typeof response === 'string' ? response : response?.logoUrl;
            if (logoUrl) {
                setBarbershopMedia(prev => ({ ...prev, logoUrl }));
            }
            toast.success('Logo da barbearia atualizada!');
            closeCropModal();
        } catch (error) {
            toast.error(error?.response?.data?.message || 'Erro ao atualizar logo da barbearia.');
        } finally {
            setUploadingLogo(false);
        }
    };

    const handleUploadBanner = async (event) => {
        const file = event.target.files?.[0];
        event.target.value = '';
        if (!file) return;

        setUploadingBanner(true);
        try {
            const response = await uploadMyBarbershopBanner(file);
            const bannerUrl = typeof response === 'string' ? response : response?.bannerUrl;
            if (bannerUrl) {
                setBarbershopMedia(prev => ({ ...prev, bannerUrl }));
            }
            toast.success('Banner da barbearia atualizado!');
        } catch (error) {
            toast.error(error?.response?.data?.message || 'Erro ao atualizar banner da barbearia.');
        } finally {
            setUploadingBanner(false);
        }
    };

    // ── actAsBarber ────────────────────────────────────────────────────────────
    const handleActAsBarberToggle = async (newValue) => {
        setSavingActAsBarber(true);
        try {
            await api.put(`/barbers/${barber.id}`, { actAsBarber: newValue });
            setActAsBarber(newValue);
            toast.success(
                newValue
                    ? 'Você voltou a aparecer como barbeiro nos agendamentos.'
                    : 'Você não aparecerá como barbeiro para novos agendamentos.'
            );
        } catch {
            toast.error('Erro ao salvar configuração. Tente novamente.');
        } finally {
            setSavingActAsBarber(false);
        }
    };

    // ── Salvar horário de trabalho (multi-bloco) ─────────────────────────────
    const handleSaveSchedule = async (e) => {
        e.preventDefault();

        // Validação: pelo menos um dia selecionado
        const activeDays = Object.keys(weekSchedule);
        if (activeDays.length === 0) {
            toast.warn('Selecione pelo menos um dia de trabalho.');
            return;
        }

        // Validação: cada bloco deve ter início e fim, e início < fim
        for (const dayKey of activeDays) {
            const blocks = weekSchedule[dayKey];
            const dayLabel = DAYS_OF_WEEK.find(d => d.key === dayKey)?.label || dayKey;
            for (let i = 0; i < blocks.length; i++) {
                const b = blocks[i];
                if (!b.startTime || !b.endTime) {
                    toast.warn(`${dayLabel} — Bloco ${i + 1}: preencha início e fim.`);
                    return;
                }
                if (b.startTime >= b.endTime) {
                    toast.warn(`${dayLabel} — Bloco ${i + 1}: o início deve ser anterior ao fim.`);
                    return;
                }
            }
            // Validação: blocos não podem se sobrepor
            const sorted = [...blocks].sort((a, b) => a.startTime.localeCompare(b.startTime));
            for (let i = 1; i < sorted.length; i++) {
                if (sorted[i].startTime < sorted[i - 1].endTime) {
                    toast.warn(`${dayLabel}: os blocos de horário não podem se sobrepor.`);
                    return;
                }
            }
        }

        setSavingSchedule(true);
        try {
            // Monta payload: { schedule: [{ dayOfWeek: 'MONDAY', blocks: [...] }, ...] }
            const schedule = activeDays.map(dayKey => ({
                dayOfWeek: dayKey,
                blocks: weekSchedule[dayKey],
            }));
            await saveMyWorkSchedule({ schedule });
            toast.success('Horário de trabalho salvo com sucesso!');
        } catch {
            toast.error('Erro ao salvar horário. Tente novamente.');
        } finally {
            setSavingSchedule(false);
        }
    };

    const hasLinkedBarbershop = !!barber?.barbershopId;
    const phoneValue = formatPhoneForProfile(barber?.tell || barber?.phone || barber?.phoneNumber);
    const cpfValue = formatCpfForProfile(barber?.documentCPF || barber?.documentCpf || barber?.cpf);

    const handleTabChange = (tab) => {
        if (tab === 'home')              navigate('/barberHome');
        else if (tab === 'perfil')       navigate('/barberHome/perfil');
        else if (tab === 'agenda')       navigate('/meus-agendamentos');
        else if (tab === 'servicos')     navigate('/barberHome/servicos');
        else if (tab === 'estoque')      navigate('/barberHome/estoque');
        else if (tab === 'time')         navigate('/barberHome/time');
        else if (tab === 'dashboards')   navigate('/barberHome/dashboard');
        else if (tab === 'agenda-equipe')     navigate('/meus-agendamentos?view=team');
        else if (tab === 'novo-agendamento') navigate('/barberHome/novo-agendamento');
        else if (tab === 'indisponibilidade') navigate('/barber/indisponibilidade');
    };

    if (loading) return <div className={styles.loadingContainer}>Carregando perfil...</div>;

    return (
        <div className={`${styles.pageContainer} ${hasLinkedBarbershop ? styles.withNavbar : styles.withoutNavbar}`}>
            <div className={styles.contentWrapper}>
                <BarberHeader
                    barber={barber}
                    onLogout={handleLogout}
                    activeTab="perfil"
                    isOwner={barber?.isOwner === true}
                    barbershopId={barber?.barbershopId}
                    onTabChange={handleTabChange}
                />

                <section className={styles.heroSection}>
                    <p className={styles.heroKicker}>MEU PERFIL</p>
                    <h1>Suas informações</h1>
                </section>

                <section className={`${styles.dashboardSection} ${styles.animateItem} ${styles.delay2}`}>
                    {barber && (
                        <div className={styles.profileStack}>

                            {/* ── Dados pessoais ───────────────────────────────── */}
                            <div className={styles.profileCard}>
                                {barber.imageUrl && (
                                    <img
                                        src={barber.imageUrl}
                                        alt="Foto de perfil"
                                        className={styles.profilePhoto}
                                    />
                                )}
                                {!barber.imageUrl && (
                                    <div className={styles.profilePhotoPlaceholder}>Sem foto</div>
                                )}
                                <input
                                    ref={profilePhotoInputRef}
                                    type="file"
                                    accept="image/*"
                                    onChange={handleUploadProfilePhoto}
                                    className={styles.hiddenFileInput}
                                />
                                <button
                                    type="button"
                                    onClick={() => profilePhotoInputRef.current?.click()}
                                    disabled={uploadingProfilePhoto}
                                    className={styles.profileUploadButton}
                                >
                                    {uploadingProfilePhoto ? 'Enviando foto...' : 'Alterar foto'}
                                </button>
                                <div><strong>Nome:</strong> {barber.name}</div>
                                <div><strong>E-mail:</strong> {barber.email}</div>
                                <div><strong>Telefone:</strong> {phoneValue}</div>
                                <div><strong>CPF:</strong> {cpfValue}</div>
                                <div><strong>Barbearia:</strong> {barber.barbershopId ? (barber.barbershopName || 'Vinculado') : 'Sem barbearia'}</div>
                                <div>
                                    <strong>Função:</strong>{' '}
                                    {barber.isOwner ? 'Dono do estabelecimento' : 'Colaborador'}
                                </div>

                                {barber.barbershopId && !barber.isOwner && (
                                    <div className={styles.profileLeaveWrap}>
                                        <button
                                            type="button"
                                            onClick={handleLeaveShop}
                                            disabled={leavingShop}
                                            className={styles.leaveShopButton}
                                        >
                                            {leavingShop ? 'Saindo...' : 'Sair da barbearia'}
                                        </button>
                                    </div>
                                )}
                            </div>

                            {/* ── Convites pendentes ─ */}
                            {!hasLinkedBarbershop ? (
                                <div className={`${styles.profileCard} ${styles.profileCardInvite}`}>
                                    <p className={`${styles.profileSectionTitle} ${styles.profileSectionTitleInvite}`}>📩 Convites de Barbearias</p>
                                    <p className={styles.profileMutedText}>
                                        Quando um dono de barbearia convida você, o convite aparece aqui.
                                    </p>
                                    {loadingInvites ? (
                                        <p className={styles.profileMutedText}>Carregando convites...</p>
                                    ) : invitesError ? (
                                        <p className={styles.profileMutedText}>
                                            Não foi possível carregar os convites agora. Tente novamente em instantes.
                                        </p>
                                    ) : pendingInvites.length === 0 ? (
                                        <p className={styles.profileMutedText}>Nenhum convite pendente no momento.</p>
                                    ) : (
                                        pendingInvites.map(inv => (
                                            <div key={inv.requestId} className={styles.inviteItem}>
                                                <div>
                                                    <p className={styles.inviteName}>
                                                        {inv.barbershopName || 'Barbearia'}
                                                    </p>
                                                    <p className={styles.inviteMeta}>
                                                        Convite recebido
                                                    </p>
                                                </div>
                                                <div className={styles.inviteActions}>
                                                    <button
                                                        onClick={() => handleAcceptInvite(inv.requestId)}
                                                        disabled={inviteActionLoading === inv.requestId}
                                                        className={styles.inviteAcceptButton}
                                                    >
                                                        {inviteActionLoading === inv.requestId ? '...' : 'Aceitar'}
                                                    </button>
                                                    <button
                                                        onClick={() => handleRejectInvite(inv.requestId)}
                                                        disabled={inviteActionLoading === inv.requestId}
                                                        className={styles.inviteRejectButton}
                                                    >
                                                        {inviteActionLoading === inv.requestId ? '...' : 'Recusar'}
                                                    </button>
                                                </div>
                                            </div>
                                        ))
                                    )}
                                </div>
                            ) : null}

                            {/* ── Horário de trabalho (multi-bloco) ──────── */}
                            <div className={styles.profileCard}>
                                <p className={styles.profileSectionTitle}>🕐 Horário de Trabalho</p>
                                <p className={styles.profileMutedText}>
                                    Selecione os dias e adicione blocos de horário. Ex.: 9h–12h e 13h–18h.
                                </p>

                                {loadingSchedule ? (
                                    <p className={styles.profileMutedText}>Carregando horários...</p>
                                ) : (
                                    <form onSubmit={handleSaveSchedule} className={styles.scheduleForm}>

                                        {/* Seletores de dia */}
                                        <div className={styles.scheduleDaysGroup}>
                                            <span className={styles.scheduleLabel}>Dias de trabalho</span>
                                            <div className={styles.scheduleDaysList}>
                                                {DAYS_OF_WEEK.map(({ key, label }) => {
                                                    const active = !!weekSchedule[key];
                                                    return (
                                                        <button
                                                            key={key}
                                                            type="button"
                                                            onClick={() => toggleDay(key)}
                                                            className={`${styles.scheduleDayButton} ${active ? styles.scheduleDayButtonActive : ''}`}
                                                        >
                                                            {label}
                                                        </button>
                                                    );
                                                })}
                                            </div>
                                        </div>

                                        {/* Blocos por dia selecionado */}
                                        {DAYS_OF_WEEK.filter(({ key }) => !!weekSchedule[key]).map(({ key, label }) => (
                                            <div key={key} className={styles.dayBlock}>
                                                <div className={styles.dayBlockHeader}>
                                                    <span className={styles.dayBlockTitle}>
                                                        {label}
                                                    </span>
                                                    <div className={styles.dayBlockActions}>
                                                        <button type="button" onClick={() => handleOpenCopyModal(key)}
                                                            className={`${styles.dayBlockActionButton} ${styles.dayBlockActionButtonCopy}`}
                                                            title="Copiar este horário para outros dias">
                                                            📋 Copiar
                                                        </button>
                                                        <button type="button" onClick={() => addBlock(key)} className={styles.dayBlockActionButton} title="Adicionar bloco de horário">
                                                            + Bloco
                                                        </button>
                                                    </div>
                                                </div>

                                                {(weekSchedule[key] || []).map((block, idx) => (
                                                    <div key={idx} className={styles.blockRow}>
                                                        <TimePicker value={block.startTime} onChange={v => updateBlock(key, idx, 'startTime', v)} disabled={savingSchedule} />
                                                        <span className={styles.blockUntil}>até</span>
                                                        <TimePicker value={block.endTime} onChange={v => updateBlock(key, idx, 'endTime', v)} disabled={savingSchedule} />
                                                        {(weekSchedule[key] || []).length > 1 && (
                                                            <button type="button" onClick={() => removeBlock(key, idx)} className={styles.removeBlockButton} title="Remover bloco">✕</button>
                                                        )}
                                                    </div>
                                                ))}
                                            </div>
                                        ))}

                                        {/* Resumo visual */}
                                        {Object.keys(weekSchedule).length > 0 && (
                                            <div className={styles.scheduleSummary}>
                                                <span className={styles.scheduleSummaryTitle}>
                                                    Resumo
                                                </span>
                                                {DAYS_OF_WEEK.filter(({ key }) => !!weekSchedule[key]).map(({ key, label }) => (
                                                    <div key={key} className={styles.scheduleSummaryRow}>
                                                        <strong className={styles.scheduleSummaryDay}>{label}:</strong>
                                                        <span>
                                                            {(weekSchedule[key] || []).map((b, i) => (
                                                                <span key={i}>
                                                                    {b.startTime || '??'}–{b.endTime || '??'}
                                                                    {i < weekSchedule[key].length - 1 ? '  /  ' : ''}
                                                                </span>
                                                            ))}
                                                        </span>
                                                    </div>
                                                ))}
                                            </div>
                                        )}

                                        <button
                                            type="submit"
                                            disabled={savingSchedule}
                                            className={`${styles.saveScheduleButton} ${savingSchedule ? styles.saveScheduleButtonDisabled : ''}`}
                                        >
                                            {savingSchedule ? 'Salvando...' : 'Salvar horário'}
                                        </button>
                                    </form>
                                )}
                            </div>

                            {/* ── Configurações do Owner (actAsBarber) ─────────── */}
                            {barber.isOwner && (
                                <div className={`${styles.profileCard} ${styles.profileCardOwner}`}>
                                    <p className={`${styles.profileSectionTitle} ${styles.profileSectionTitleOwner}`}>⚙️ Configurações da Barbearia</p>
                                    <label className={`${styles.ownerSettingsLabel} ${savingActAsBarber ? styles.ownerSettingsLabelDisabled : ''}`}>
                                        <input
                                            type="checkbox"
                                            checked={actAsBarber}
                                            disabled={savingActAsBarber}
                                            onChange={e => handleActAsBarberToggle(e.target.checked)}
                                            className={styles.ownerSettingsCheckbox}
                                        />
                                        <span className={styles.ownerSettingsText}>
                                            <strong>Atuar como barbeiro</strong> — aparecer na lista de profissionais disponíveis para agendamento
                                        </span>
                                    </label>

                                    {loadingBarbershopInfo ? (
                                        <p className={styles.profileMutedText}>Carregando dados da barbearia...</p>
                                    ) : (
                                        <form onSubmit={handleSaveBarbershopInfo} className={styles.shopEditForm}>
                                            <label className={styles.shopField}>
                                                <span>Nome da barbearia</span>
                                                <input
                                                    name="name"
                                                    value={barbershopForm.name}
                                                    onChange={handleBarbershopFormChange}
                                                    maxLength={80}
                                                    required
                                                />
                                            </label>

                                            <label className={styles.shopField}>
                                                <span>Endereço</span>
                                                <input
                                                    name="address"
                                                    value={barbershopForm.address}
                                                    onChange={handleBarbershopFormChange}
                                                    maxLength={140}
                                                    required
                                                />
                                            </label>

                                            <div className={styles.shopGeoRow}>
                                                <button
                                                    type="button"
                                                    className={styles.geoBtn}
                                                    onClick={() => {
                                                        if (!navigator.geolocation) {
                                                            toast.error('Geolocalização não suportada pelo navegador.');
                                                            return;
                                                        }
                                                        navigator.geolocation.getCurrentPosition(
                                                            ({ coords }) => {
                                                                setBarbershopForm(prev => ({
                                                                    ...prev,
                                                                    latitude: coords.latitude,
                                                                    longitude: coords.longitude,
                                                                }));
                                                                toast.success('Localização capturada! Salve para confirmar.');
                                                            },
                                                            () => toast.error('Não foi possível obter a localização. Verifique as permissões.')
                                                        );
                                                    }}
                                                >
                                                    📍 Usar minha localização atual
                                                </button>

                                                {barbershopForm.latitude && barbershopForm.longitude && (
                                                    <span className={styles.geoCoords}>
                                                        {barbershopForm.latitude.toFixed(5)}, {barbershopForm.longitude.toFixed(5)}
                                                    </span>
                                                )}
                                            </div>

                                            <div className={styles.shopMediaGrid}>
                                                <div className={styles.shopMediaCard}>
                                                    <span className={styles.shopMediaLabel}>Logo</span>
                                                    {barbershopMedia.logoUrl ? (
                                                        <img src={barbershopMedia.logoUrl} alt="Logo da barbearia" className={styles.shopMediaImage} />
                                                    ) : (
                                                        <div className={styles.shopMediaPlaceholder}>Sem logo</div>
                                                    )}
                                                    <input
                                                        ref={logoInputRef}
                                                        type="file"
                                                        accept="image/*"
                                                        onChange={handleUploadLogo}
                                                        className={styles.hiddenFileInput}
                                                    />
                                                    <button
                                                        type="button"
                                                        onClick={() => logoInputRef.current?.click()}
                                                        disabled={uploadingLogo}
                                                        className={styles.shopMediaButton}
                                                    >
                                                        {uploadingLogo ? 'Enviando...' : 'Trocar logo'}
                                                    </button>
                                                </div>
                                            </div>

                                            <button
                                                type="submit"
                                                disabled={savingBarbershopInfo}
                                                className={styles.saveShopButton}
                                            >
                                                {savingBarbershopInfo ? 'Salvando dados...' : 'Salvar dados da barbearia'}
                                            </button>
                                        </form>
                                    )}
                                </div>
                            )}

                        </div>
                    )}
                </section>
            </div>

            {hasLinkedBarbershop && (
                <BarberNavbar
                    activeTab="perfil"
                    onTabChange={handleTabChange}
                    isOwner={barber?.isOwner === true}
                    barbershopId={barber?.barbershopId}
                />
            )}

            {cropModal && (
                <CropImageModal
                    src={cropModal.src}
                    title={cropModal.target === 'profile' ? 'Ajustar foto de perfil' : 'Ajustar logo da barbearia'}
                    aspect={1}
                    outputSize={{ width: 600, height: 600 }}
                    onCancel={closeCropModal}
                    onConfirm={handleConfirmCrop}
                />
            )}

            {/* ── Modal copiar horário ──────────────────────────────────── */}
            {isCopyModalOpen && (
                <div className={styles.modalBackdrop} onClick={() => setIsCopyModalOpen(false)}>
                    <div className={`${styles.modalCard} ${styles.copyModalCard}`} onClick={e => e.stopPropagation()}>
                        <p className={styles.copyModalTitle}>📋 Copiar horário</p>
                        <p className={styles.copyModalSubtitle}>
                            Copiar de <strong style={{ color: '#d4af37' }}>{DAYS_OF_WEEK.find(d => d.key === copySource)?.label}</strong> para:
                        </p>
                        <div className={styles.copyTargetsGrid}>
                            {DAYS_OF_WEEK.filter(d => d.key !== copySource).map(({ key, label }) => {
                                const selected = copyTargets.includes(key);
                                return (
                                    <button key={key} type="button" onClick={() => toggleCopyTarget(key)}
                                        className={`${styles.copyTargetButton} ${selected ? styles.copyTargetButtonActive : ''}`}>
                                        {label}
                                    </button>
                                );
                            })}
                        </div>
                        <div className={styles.copyActionsRow}>
                            <button onClick={() => setIsCopyModalOpen(false)}
                                className={`${styles.copyActionButton} ${styles.copyCancelButton}`}>
                                Cancelar
                            </button>
                            <button onClick={handleCopyConfirm} disabled={copyTargets.length === 0}
                                className={`${styles.copyActionButton} ${styles.copyConfirmButton} ${copyTargets.length === 0 ? styles.copyConfirmButtonDisabled : ''}`}>
                                Copiar ({copyTargets.length})
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default BarberProfilePage;
