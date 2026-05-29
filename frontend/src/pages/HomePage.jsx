import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import Barbershops from "../components/HomePage/Barbershops/Barbershops"
import Favorite_barbershops from "../components/HomePage/Favorite_barbershops/Favorite_barbershops"
import SearchBar from "../components/HomePage/SearchBar"
import CustomerHeader from "../components/HomePage/CustomerHeader";
import CustomerNavbar from "../components/HomePage/CustomerNavbar";
import { logoutUser } from "../services/authService";
import { isBarber } from "../services/userContext";
import { getMyProfile } from "../services/userProfileService";
import {
  addFavoriteBarbershop,
  getMyFavoriteBarbershopsIds,
  removeFavoriteBarbershop,
} from "../services/barbershopService";
import Styles from "./CSS/HomePage.module.css"

function HomePage() {
  const [searchTerm, setSearchTerm] = useState("");
  const [favoriteIds, setFavoriteIds] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    if (isBarber()) {
      navigate('/barberHome', { replace: true });
    }
  }, [navigate]);

  useEffect(() => {
    if (isBarber()) return;
    getMyProfile()
      .then((data) => {
        if (data?.name) localStorage.setItem('userName', data.name);
        if (data?.imageUrl) localStorage.setItem('userProfileImage', data.imageUrl);
      })
      .catch(() => { /* silencia — não crítico para renderização */ });
  }, []);

  useEffect(() => {
    const loadFavorites = async () => {
      const ids = await getMyFavoriteBarbershopsIds();
      setFavoriteIds(ids);
    };
    loadFavorites();
  }, []);

  const handleLogout = () => {
    logoutUser();
    navigate("/");
  };

  const handleToggleFavorite = async (shopId, currentlyFavorite) => {
    try {
      if (currentlyFavorite) {
        await removeFavoriteBarbershop(shopId);
        setFavoriteIds((prev) => prev.filter((id) => id !== shopId));
      } else {
        await addFavoriteBarbershop(shopId);
        setFavoriteIds((prev) => (prev.includes(shopId) ? prev : [...prev, shopId]));
      }
    } catch {
      toast.error("Não foi possível atualizar suas favoritas agora.");
    }
  };

  return (
    <div className={Styles.homepage_container}>
      <CustomerHeader activeTab="home" onLogout={handleLogout} />
      <CustomerNavbar activeTab="home" onLogout={handleLogout} />

      <div className={Styles.glow_top} />

      <div className={Styles.content_wrapper}>
        <section
          className={`${Styles.hero_section} ${Styles.animate_item} ${Styles.delay_1}`}
          data-onboarding-id="customer-home-hero"
        >
          <div className={Styles.hero_text}>
            <span className={Styles.hero_badge}>Painel do Cliente</span>
            <h1>Pronto para o seu <br/><span className={Styles.highlight}>próximo estilo?</span></h1>
            <p>Explore barbearias premium, compare serviços e agende no horário ideal para você.</p>
          </div>

          <div
            className={`${Styles.search_wrapper} ${Styles.animate_item} ${Styles.delay_2}`}
            data-onboarding-id="customer-home-search"
          >
            <SearchBar searchTerm={searchTerm} onSearchChange={setSearchTerm} />
          </div>
        </section>

        {favoriteIds.length > 0 && (
          <section className={`${Styles.favorites_section} ${Styles.animate_item} ${Styles.delay_3}`}>
            <div className={Styles.section_header}>
              <h3>Suas Favoritas</h3>
              <span className={Styles.section_subtitle}>Acesso rápido</span>
            </div>
            <Favorite_barbershops favoriteIds={favoriteIds} />
          </section>
        )}

        <section
          className={`${Styles.list_section} ${Styles.animate_item} ${Styles.delay_4}`}
          data-onboarding-id="customer-home-list"
        >
          <div className={Styles.section_header}>
            <h3>Descobrir Barbearias</h3>
            <span className={Styles.section_subtitle}>Perto de você</span>
          </div>
          <Barbershops
            searchTerm={searchTerm}
            favoriteIds={favoriteIds}
            onToggleFavorite={handleToggleFavorite}
          />
        </section>
      </div>
    </div>
  )
}

export default HomePage
