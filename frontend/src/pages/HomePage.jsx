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
      toast.error("Nao foi possivel atualizar suas favoritas agora.");
    }
  };

  return (
    <div className={Styles.homepage_container}>
      <CustomerHeader activeTab="home" onLogout={handleLogout} />
      <CustomerNavbar activeTab="home" onLogout={handleLogout} />

      <section className={Styles.hero_section}>
        <p className={Styles.hero_kicker}>HOME DO CLIENTE</p>
        <h1>Ola, pronto para o proximo corte?</h1>
        <p>Explore barbearias, compare servicos e agende no horario ideal para voce.</p>
      </section>

      <section className={Styles.search_section}>
        <SearchBar searchTerm={searchTerm} onSearchChange={setSearchTerm} />
      </section>

      <section className={Styles.favorites_section}>
        <Favorite_barbershops favoriteIds={favoriteIds} />
      </section>

      <div className={Styles.section_header}>
        <h3>Barbearias disponiveis</h3>
      </div>

      <Barbershops
        searchTerm={searchTerm}
        favoriteIds={favoriteIds}
        onToggleFavorite={handleToggleFavorite}
      />
    </div>
  )
}

export default HomePage