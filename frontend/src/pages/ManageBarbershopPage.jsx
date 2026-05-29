import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../services/api';
import {
    getBarbershopById,
    updateMyBarbershop,
    uploadMyBarbershopLogo,
    uploadMyBarbershopBanner,
    geocodeAddress,
} from '../services/barbershopService';
import { logoutUser } from '../services/authService';
import { isOwnerUser } from '../services/userContext';
import { navigateToBarberTab } from '../services/navigationService';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import CropImageModal from '../components/CropImageModal/CropImageModal';
import styles from './CSS/BarberHomePage.module.css';

function ManageBarbershopPage() {
    const navigate = useNavigate();
    const logoInputRef = useRef(null);
    const bannerInputRef = useRef(null);
    const cropObjectUrlRef = useRef(null);

    const [barber, setBarber] = useState(null);
    const [loading, setLoading] = useState(true);

    const [shopForm, setShopForm] = useState({
        name: '',
        address: '',
        description: '',
        phone: '',
        latitude: null,
        longitude: null,
    });
    const [media, setMedia] = useState({ logoUrl: '', bannerUrl: '' });
    const [loadingShop, setLoadingShop] = useState(false);
    const [saving, setSaving] = useState(false);
    const [uploadingLogo, setUploadingLogo] = useState(false);
    const [uploadingBanner, setUploadingBanner] = useState(false);
    const [cropModal, setCropModal] = useState(null);

    // ── Auth guard ───────────────────────────────────────────────────────────
    useEffect(() => {
        const token = localStorage.getItem('token');
        if (!token) { navigate('/', { replace: true }); return; }

        api.get('/auth/me')
            .then(res => {
                const data = res.data;
                if (!data.isOwner && !isOwnerUser()) {
                    navigate('/barberHome', { replace: true });
                    return;
                }
                setBarber(data);
                setLoading(false);
            })
            .catch(() => { setLoading(false); navigate('/'); });
    }, [navigate]);

    // ── Carrega dados da barbearia ────────────────────────────────────────────
    useEffect(() => {
        if (!barber?.barbershopId) return;

        setLoadingShop(true);
        getBarbershopById(barber.barbershopId)
            .then(shop => {
                if (!shop) return;
                setShopForm({
                    name: shop.name || '',
                    address: shop.address || '',
                    description: shop.description || '',
                    phone: shop.phone || '',
                    latitude: shop.latitude ?? null,
                    longitude: shop.longitude ?? null,
                });
                setMedia({
                    logoUrl: shop.logoUrl || '',
                    bannerUrl: shop.bannerUrl || '',
                });
            })
            .catch(() => toast.error('Erro ao carregar dados da barbearia.'))
            .finally(() => setLoadingShop(false));
    }, [barber?.barbershopId]);

    // ── Handlers ──────────────────────────────────────────────────────────────
    const handleChange = (e) => {
        const { name, value } = e.target;
        setShopForm(prev => ({ ...prev, [name]: value }));
    };

    const handleUseLocation = () => {
        if (!navigator.geolocation) { toast.error('Geolocalização não suportada.'); return; }
        navigator.geolocation.getCurrentPosition(
            ({ coords }) => {
                setShopForm(prev => ({ ...prev, latitude: coords.latitude, longitude: coords.longitude }));
                toast.success('Localização capturada! Salve para confirmar.');
            },
            () => toast.error('Não foi possível obter a localização. Verifique as permissões.')
        );
    };

    const handleSave = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            const trimmedAddress = shopForm.address.trim();
            let { latitude, longitude } = shopForm;

            if (trimmedAddress && !latitude) {
                const coords = await geocodeAddress(trimmedAddress);
                if (coords) { latitude = coords.lat; longitude = coords.lng; }
            }

            await updateMyBarbershop({
                name: shopForm.name.trim(),
                address: trimmedAddress,
                description: shopForm.description?.trim() || null,
                phone: shopForm.phone?.trim() || null,
                latitude,
                longitude,
            });
            setShopForm(prev => ({ ...prev, latitude, longitude }));
            localStorage.setItem('barbershopName', shopForm.name.trim());
            toast.success('Dados da barbearia atualizados!');
        } catch (err) {
            toast.error(err?.response?.data?.message || 'Erro ao salvar dados.');
        } finally {
            setSaving(false);
        }
    };

    // ── Crop / upload miniatura ───────────────────────────────────────────────
    const handleLogoChange = (e) => {
        const file = e.target.files?.[0];
        e.target.value = '';
        if (!file) return;
        if (cropObjectUrlRef.current) URL.revokeObjectURL(cropObjectUrlRef.current);
        cropObjectUrlRef.current = URL.createObjectURL(file);
        setCropModal({ target: 'logo', src: cropObjectUrlRef.current, fileName: file.name });
    };

    // ── Crop / upload banner ──────────────────────────────────────────────────
    const handleBannerChange = (e) => {
        const file = e.target.files?.[0];
        e.target.value = '';
        if (!file) return;
        if (cropObjectUrlRef.current) URL.revokeObjectURL(cropObjectUrlRef.current);
        cropObjectUrlRef.current = URL.createObjectURL(file);
        setCropModal({ target: 'banner', src: cropObjectUrlRef.current, fileName: file.name });
    };

    const closeCropModal = useCallback(() => {
        if (cropObjectUrlRef.current) { URL.revokeObjectURL(cropObjectUrlRef.current); cropObjectUrlRef.current = null; }
        setCropModal(null);
    }, []);

    const handleConfirmCrop = async (blob) => {
        if (cropModal?.target === 'banner') {
            const croppedFile = new File([blob], 'banner-barbearia.jpg', { type: blob.type || 'image/jpeg' });
            setUploadingBanner(true);
            try {
                const res = await uploadMyBarbershopBanner(croppedFile);
                const url = typeof res === 'string' ? res : res?.bannerUrl;
                if (url) setMedia(prev => ({ ...prev, bannerUrl: url }));
                toast.success('Banner atualizado!');
                closeCropModal();
            } catch (err) {
                toast.error(err?.response?.data?.message || 'Erro ao enviar banner.');
            } finally {
                setUploadingBanner(false);
            }
            return;
        }

        // miniatura
        const croppedFile = new File([blob], 'miniatura-barbearia.jpg', { type: blob.type || 'image/jpeg' });
        setUploadingLogo(true);
        try {
            const res = await uploadMyBarbershopLogo(croppedFile);
            const url = typeof res === 'string' ? res : res?.logoUrl;
            if (url) setMedia(prev => ({ ...prev, logoUrl: url }));
            toast.success('Miniatura atualizada!');
            closeCropModal();
        } catch (err) {
            toast.error(err?.response?.data?.message || 'Erro ao enviar miniatura.');
        } finally {
            setUploadingLogo(false);
        }
    };

    const handleTabChange = (tab) => navigateToBarberTab(tab, navigate, {
        isOwner: true,
        currentPath: '/barberHome/gerenciar-barbearia',
    });
    const handleLogout = async () => { await logoutUser(); navigate('/'); };

    if (loading) return <div className={styles.loadingContainer}>Carregando...</div>;

    return (
        <div className={`${styles.pageContainer} ${styles.withNavbar}`} data-onboarding-id="owner-manage-shop-page">
            <div className={styles.contentWrapper}>
                <BarberHeader
                    barber={barber}
                    onLogout={handleLogout}
                    activeTab="gerenciar-barbearia"
                    onTabChange={handleTabChange}
                    isOwner
                    barbershopId={barber?.barbershopId}
                />

                <section className={styles.heroSection}>
                    <p className={styles.heroKicker}>GESTÃO</p>
                    <h1>Gerenciar Barbearia</h1>
                    <p>Edite todas as informações públicas da sua barbearia.</p>
                </section>

                <section className={`${styles.dashboardSection} ${styles.animateItem} ${styles.delay2}`}>
                    {loadingShop ? (
                        <p className={styles.profileMutedText}>Carregando dados...</p>
                    ) : (
                        <div className={styles.profileCard}>
                            {/* ── Banner ── */}
                            <div className={styles.shopBannerPreview} style={{ position: 'relative', marginBottom: '1.5rem' }}>
                                {media.bannerUrl ? (
                                    <img
                                        src={media.bannerUrl}
                                        alt="Banner da barbearia"
                                        style={{ width: '100%', height: '160px', objectFit: 'cover', borderRadius: '12px' }}
                                    />
                                ) : (
                                    <div style={{
                                        width: '100%', height: '160px', borderRadius: '12px',
                                        background: 'linear-gradient(135deg,#1a1a1a,#2a2a2a)',
                                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        color: '#666', fontSize: '0.9rem',
                                    }}>
                                        Sem banner
                                    </div>
                                )}
                                <input ref={bannerInputRef} type="file" accept="image/*" onChange={handleBannerChange} style={{ display: 'none' }} />
                                <button
                                    type="button"
                                    onClick={() => bannerInputRef.current?.click()}
                                    disabled={uploadingBanner}
                                    style={{
                                        position: 'absolute', bottom: 8, right: 8,
                                        background: 'rgba(0,0,0,0.7)', color: '#fff',
                                        border: '1px solid #555', borderRadius: 8,
                                        padding: '0.35rem 0.75rem', fontSize: '0.8rem', cursor: 'pointer',
                                    }}
                                >
                                    {uploadingBanner ? 'Ajustando...' : '📷 Trocar banner'}
                                </button>
                            </div>

                            {/* ── Miniatura ── */}
                            <div className={styles.shopMediaGrid} style={{ marginBottom: '1.5rem' }}>
                                <div className={styles.shopMediaCard}>
                                    <span className={styles.shopMediaLabel}>Miniatura</span>
                                    {media.logoUrl ? (
                                        <img src={media.logoUrl} alt="Miniatura" className={styles.shopMediaImage} />
                                    ) : (
                                        <div className={styles.shopMediaPlaceholder}>Sem miniatura</div>
                                    )}
                                    <input ref={logoInputRef} type="file" accept="image/*" onChange={handleLogoChange} className={styles.hiddenFileInput} />
                                    <button
                                        type="button"
                                        onClick={() => logoInputRef.current?.click()}
                                        disabled={uploadingLogo}
                                        className={styles.shopMediaButton}
                                    >
                                        {uploadingLogo ? 'Ajustando...' : 'Trocar miniatura'}
                                    </button>
                                </div>
                            </div>

                            {/* ── Formulário de dados ── */}
                            <form onSubmit={handleSave} className={styles.shopEditForm}>
                                <label className={styles.shopField}>
                                    <span>Nome da barbearia</span>
                                    <input name="name" value={shopForm.name} onChange={handleChange} maxLength={80} required />
                                </label>

                                <label className={styles.shopField}>
                                    <span>Endereço</span>
                                    <input name="address" value={shopForm.address} onChange={handleChange} maxLength={140} required />
                                </label>

                                <label className={styles.shopField}>
                                    <span>Descrição (opcional)</span>
                                    <textarea
                                        name="description"
                                        value={shopForm.description}
                                        onChange={handleChange}
                                        maxLength={400}
                                        rows={3}
                                        style={{ resize: 'vertical' }}
                                    />
                                </label>

                                <label className={styles.shopField}>
                                    <span>Telefone / WhatsApp (opcional)</span>
                                    <input
                                        name="phone"
                                        value={shopForm.phone}
                                        onChange={handleChange}
                                        maxLength={20}
                                        placeholder="(11) 99999-9999"
                                    />
                                </label>

                                <div className={styles.shopGeoRow}>
                                    <button type="button" className={styles.geoBtn} onClick={handleUseLocation}>
                                        📍 Usar minha localização atual
                                    </button>
                                    {shopForm.latitude && shopForm.longitude && (
                                        <span className={styles.geoCoords}>
                                            {Number(shopForm.latitude).toFixed(5)}, {Number(shopForm.longitude).toFixed(5)}
                                        </span>
                                    )}
                                </div>

                                <button type="submit" disabled={saving} className={styles.saveShopButton}>
                                    {saving ? 'Salvando...' : 'Salvar dados da barbearia'}
                                </button>
                            </form>
                        </div>
                    )}
                </section>
            </div>

            <BarberNavbar
                activeTab="gerenciar-barbearia"
                onTabChange={handleTabChange}
                onLogout={handleLogout}
                isOwner
                barbershopId={barber?.barbershopId}
            />

            {cropModal && (
                <CropImageModal
                    src={cropModal.src}
                    title={cropModal.target === 'banner' ? 'Ajustar banner da barbearia' : 'Ajustar miniatura da barbearia'}
                    aspect={cropModal.target === 'banner' ? 16 / 9 : 1}
                    outputSize={cropModal.target === 'banner' ? { width: 1200, height: 675 } : { width: 600, height: 600 }}
                    onCancel={closeCropModal}
                    onConfirm={handleConfirmCrop}
                />
            )}
        </div>
    );
}

export default ManageBarbershopPage;
