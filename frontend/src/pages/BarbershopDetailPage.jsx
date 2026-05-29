import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { FaStar, FaStarHalfAlt, FaRegStar } from "react-icons/fa";
import { FiScissors, FiMapPin, FiClock, FiArrowRight } from "react-icons/fi";
import { toast } from "react-toastify";
import { getBarbershopById, getShopServices, getShopBarbers, geocodeAddress, updateMyBarbershop } from "../services/barbershopService";
import BarbershopMap from "../components/BarbershopMap/BarbershopMap";
import CustomerHeader from "../components/HomePage/CustomerHeader";
import CustomerNavbar from "../components/HomePage/CustomerNavbar";
import { logoutUser } from "../services/authService";
import Styles from "./CSS/BarbershopDetailPage.module.css";

const BarbershopDetailPage = () => {
  const { barbershopId } = useParams();
  const navigate = useNavigate();

  const [shopInfo, setShopInfo] = useState(null);
  const [services, setServices] = useState([]);
  const [barbers, setBarbers] = useState([]);
  const [loading, setLoading] = useState(true);

  const handleLogout = () => {
    logoutUser();
    navigate("/");
  };

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [shopData, servicesData, barbersData] = await Promise.all([
          getBarbershopById(barbershopId),
          getShopServices(barbershopId),
          getShopBarbers(barbershopId),
        ]);

        // Geocodifica o endereço se lat/lng não vieram do backend
        let enrichedShop = shopData;
        const noCoords = shopData && (shopData.latitude == null || shopData.longitude == null
            || (shopData.latitude === 0 && shopData.longitude === 0));
        if (noCoords && shopData.address) {
          const coords = await geocodeAddress(shopData.address).catch(() => null);
          if (coords) {
            enrichedShop = { ...shopData, latitude: coords.lat, longitude: coords.lng };

            // Persiste as coordenadas no banco se o usuário logado for o dono desta barbearia,
            // evitando que futuros visitantes precisem refazer o geocoding.
            const isOwnerOfThisShop =
              localStorage.getItem('userRole') === 'ROLE_BARBER' &&
              localStorage.getItem('isOwner') === 'true' &&
              String(localStorage.getItem('barbershopId')) === String(barbershopId);

            if (isOwnerOfThisShop) {
              updateMyBarbershop({ latitude: coords.lat, longitude: coords.lng }).catch(() => {});
            }
          }
        }

        setShopInfo(enrichedShop);
        setServices(servicesData || []);
        setBarbers(barbersData || []);
      } catch {
        toast.error("Erro ao carregar informações da barbearia.");
      } finally {
        setLoading(false);
      }
    };
    if (barbershopId) fetchData();
  }, [barbershopId]);

  const renderStars = (value = 0) => {
    const stars = [];
    for (let i = 1; i <= 5; i++) {
      if (value >= i) stars.push(<FaStar key={i} className={Styles.starFilled} />);
      else if (value >= i - 0.5) stars.push(<FaStarHalfAlt key={i} className={Styles.starFilled} />);
      else stars.push(<FaRegStar key={i} className={Styles.starEmpty} />);
    }
    return stars;
  };

  const formatCurrency = (v) =>
    new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(v);

  const hasLocation = Boolean(
    shopInfo?.address ||
    (Number.isFinite(Number(shopInfo?.latitude)) && Number.isFinite(Number(shopInfo?.longitude)))
  );

  if (loading) {
    return (
      <div className={`ca-page ${Styles.page}`}>
        <CustomerHeader activeTab="agendamentos" onLogout={handleLogout} />
        <CustomerNavbar activeTab="agendamentos" onLogout={handleLogout} />
        <div className={Styles.loadingState}>Carregando...</div>
      </div>
    );
  }

  if (!shopInfo) {
    return (
      <div className={`ca-page ${Styles.page}`}>
        <CustomerHeader activeTab="agendamentos" onLogout={handleLogout} />
        <CustomerNavbar activeTab="agendamentos" onLogout={handleLogout} />
        <p className={Styles.emptyState}>Barbearia não encontrada.</p>
      </div>
    );
  }

  return (
    <div className={`ca-page ${Styles.page}`}>
      <div className={`ca-container ${Styles.content}`}>
        <CustomerHeader activeTab="agendamentos" onLogout={handleLogout} />
        <CustomerNavbar activeTab="agendamentos" onLogout={handleLogout} />

        {/* Banner */}
        <div className={Styles.banner} data-onboarding-id="shop-detail-banner">
          {shopInfo.bannerUrl ? (
            <img src={shopInfo.bannerUrl} alt={`Banner de ${shopInfo.name}`} className={Styles.bannerImg} />
          ) : (
            <div className={Styles.bannerPlaceholder}>{shopInfo.name}</div>
          )}
          <div className={Styles.bannerOverlay}>
            <div className={Styles.shopMeta}>
              {shopInfo.logoUrl && (
                <img src={shopInfo.logoUrl} alt="Logo" className={Styles.shopLogo} />
              )}
              <div>
                <h1 className={Styles.shopName}>{shopInfo.name}</h1>
                {shopInfo.address && (
                  <p className={Styles.shopAddress}>
                    <FiMapPin size={13} /> {shopInfo.address}
                  </p>
                )}
              </div>
            </div>
            {(shopInfo.averageRating || shopInfo.reviewsCount > 0) && (
              <div className={Styles.ratingBadge}>
                <div className={Styles.stars}>{renderStars(shopInfo.averageRating ?? 0)}</div>
                <span>{(shopInfo.averageRating ?? 0).toFixed(1)}</span>
                {shopInfo.reviewsCount > 0 && (
                  <span className={Styles.reviewCount}>({shopInfo.reviewsCount} avaliações)</span>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Descrição */}
        {shopInfo.description && (
          <section className={Styles.section}>
            <p className={Styles.description}>{shopInfo.description}</p>
          </section>
        )}

        {/* Serviços */}
        {services.length > 0 && (
          <section className={Styles.section} data-onboarding-id="shop-detail-services">
            <h2 className={Styles.sectionTitle}><FiScissors size={16} /> Serviços</h2>
            <div className={Styles.serviceGrid}>
              {services.map((s) => (
                <div key={s.id} className={Styles.serviceCard}>
                  <span className={Styles.serviceName}>{s.activityName}</span>
                  <div className={Styles.serviceMeta}>
                    {s.durationMinutes && (
                      <span><FiClock size={12} /> {s.durationMinutes} min</span>
                    )}
                    <strong>{formatCurrency(s.price)}</strong>
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Profissionais */}
        {barbers.length > 0 && (
          <section className={Styles.section}>
            <h2 className={Styles.sectionTitle}>Profissionais</h2>
            <div className={Styles.barberRow}>
              {barbers.map((b) => (
                <div key={b.id} className={Styles.barberChip}>
                  {b.imageUrl ? (
                    <img src={b.imageUrl} alt={b.name} className={Styles.barberAvatarImg}
                         onError={(e) => { e.currentTarget.style.display = 'none'; }} />
                  ) : (
                    <span className={Styles.barberAvatar}>
                      {b.name?.split(' ').slice(0, 2).map((p) => p[0]).join('').toUpperCase()}
                    </span>
                  )}
                  <span className={Styles.barberName}>{b.name}</span>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Mapa / Localização */}
        {hasLocation && (
          <section className={Styles.section}>
            <h2 className={Styles.sectionTitle}><FiMapPin size={16} /> Localização</h2>
            <BarbershopMap
              latitude={shopInfo.latitude}
              longitude={shopInfo.longitude}
              barbershopName={shopInfo.name}
              address={shopInfo.address}
            />
          </section>
        )}

        {/* CTA Agendar */}
        <div className={Styles.ctaBar} data-onboarding-id="shop-detail-cta">
          <button
            className={Styles.ctaBtn}
            onClick={() => navigate(`/agendamentoPage/${barbershopId}`)}
          >
            Agendar agora <FiArrowRight size={16} />
          </button>
        </div>
      </div>
    </div>
  );
};

export default BarbershopDetailPage;
