import { useRef, useState, useEffect } from "react"
import { useNavigate } from "react-router-dom";
import { FiHome, FiSearch, FiHeart, FiScissors, FiCalendar, FiLogOut, FiLock } from "react-icons/fi";
import Barbershops from "../components/HomePage/Barbershops/Barbershops"
import Favorite_barbershops from "../components/HomePage/Favorite_barbershops/Favorite_barbershops"
import SearchBar from "../components/HomePage/SearchBar"
import { logoutUser } from "../services/authService";
import Styles from "./CSS/HomePage.module.css"

const desktopNavItems = [
  { key: "topo", label: "Topo", icon: FiHome },
  { key: "buscar", label: "Buscar", icon: FiSearch },
  { key: "favoritas", label: "Favoritas", icon: FiHeart },
  { key: "barbearias", label: "Barbearias", icon: FiScissors },
];

function HomePage() {
  const [searchTerm, setSearchTerm] = useState("");
  const navigate = useNavigate();

  // Guarda de rota: barbeiro/owner não deve ver a homepage do cliente
  useEffect(() => {
    const role = localStorage.getItem('userRole');
    if (role === 'ROLE_BARBER' || role === 'ROLE_OWNER') {
      navigate('/barberHome', { replace: true });
    }
  }, [navigate]);

  const userName = localStorage.getItem("userName") || "Cliente";
  const firstName = userName.split(" ")[0];
  const heroRef = useRef(null);
  const searchRef = useRef(null);
  const favoritesRef = useRef(null);
  const shopsRef = useRef(null);
  const [activeDesktopItem, setActiveDesktopItem] = useState("topo");

  const scrollToSection = (sectionRef, sectionKey) => {
    if (!sectionRef?.current) return;
    setActiveDesktopItem(sectionKey);
    sectionRef.current.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  const handleLogout = () => {
    logoutUser();
    navigate("/identificacao", { state: { mode: "login", role: "customer" } });
  };

  const getDesktopItemClass = (itemKey) => (
    activeDesktopItem === itemKey ? Styles.desktop_menu_item_active : Styles.desktop_menu_item
  );

  const handleDesktopNavigation = (itemKey) => {
    if (itemKey === "topo") scrollToSection(heroRef, "topo");
    if (itemKey === "buscar") scrollToSection(searchRef, "buscar");
    if (itemKey === "favoritas") scrollToSection(favoritesRef, "favoritas");
    if (itemKey === "barbearias") scrollToSection(shopsRef, "barbearias");
  };

  return (
    <div className={Styles.homepage_container}>
      <header className={Styles.desktop_nav_shell}>
        <div className={Styles.desktop_brand_block}>
          <div className={Styles.desktop_brand_badge}>CA</div>
          <div className={Styles.desktop_brand_text}>
            <h3>CortaAI</h3>
            <p>Painel do cliente</p>
          </div>
        </div>

        <nav className={Styles.desktop_center_nav} aria-label="Navegacao desktop da home">
          {desktopNavItems.map((item) => {
            const Icon = item.icon;
            return (
              <button
                key={item.key}
                className={getDesktopItemClass(item.key)}
                onClick={() => handleDesktopNavigation(item.key)}
              >
                <Icon className={Styles.desktop_menu_icon} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        <div className={Styles.desktop_actions_block}>
          <button
            className={Styles.desktop_secondary_button}
            onClick={() => navigate("/meus-agendamentos")}
          >
            <FiCalendar className={Styles.desktop_action_icon} />
            Meus agendamentos
          </button>
          <button
            className={Styles.desktop_secondary_button}
            onClick={() => navigate("/change-password")}
          >
            <FiLock className={Styles.desktop_action_icon} />
            Alterar senha
          </button>
          <button className={Styles.desktop_logout_button} onClick={handleLogout}>
            <FiLogOut className={Styles.desktop_action_icon} />
            Sair
          </button>
        </div>
      </header>

      <section ref={heroRef} className={Styles.hero_section}>
        <p className={Styles.hero_kicker}>HOME DO CLIENTE</p>
        <h1>Ola, {firstName}. Pronto para o proximo corte?</h1>
        <p>Explore barbearias, compare servicos e agende no horario ideal para voce.</p>
      </section>

      <section ref={searchRef} className={Styles.search_section}>
        <SearchBar searchTerm={searchTerm} onSearchChange={setSearchTerm} />
      </section>

      <section ref={favoritesRef} className={Styles.favorites_section}>
        <Favorite_barbershops />
      </section>

      <div ref={shopsRef} className={Styles.section_header}>
        <h3>Barbearias disponiveis</h3>
      </div>

      <Barbershops searchTerm={searchTerm} />
    </div>
  )
}

export default HomePage