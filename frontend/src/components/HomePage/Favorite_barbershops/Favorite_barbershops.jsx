import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom";
import Styles from "./CSS/Favorite_barbershops.module.css"
import { getAllBarbershops } from "../../../services/barbershopService";

function Favorite_barbershops() {
    const [favoriteBarbershops, setFavoriteBarbershops] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    const loadFavorites = async () => {
        setLoading(true);

        try {
            const favoritesIds = JSON.parse(localStorage.getItem("favoriteBarbershops") || "[]");

            if (favoritesIds.length === 0) {
                setFavoriteBarbershops([]);
                setLoading(false);
                return;
            }

            const shops = await getAllBarbershops();
            const filteredFavorites = shops.filter((shop) => favoritesIds.includes(shop.id));
            setFavoriteBarbershops(filteredFavorites);
        } catch (error) {
            console.error("Erro ao carregar favoritos:", error);
            setFavoriteBarbershops([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadFavorites();

        const handleStorageChange = (event) => {
            if (event.key === "favoriteBarbershops") {
                loadFavorites();
            }
        };

        window.addEventListener("storage", handleStorageChange);
        return () => window.removeEventListener("storage", handleStorageChange);
    }, []);

    return (
        <div className={Styles.favorite_barbershops_container}>
            <div className={Styles.favorite_header}>
                <h3>Minhas barbearias favoritas</h3>
                <span>{favoriteBarbershops.length} salva(s)</span>
            </div>

            {loading ? (
                <p className={Styles.info_text}>Carregando favoritas...</p>
            ) : favoriteBarbershops.length > 0 ? (
                <div className={Styles.favorites_list}>
                    {favoriteBarbershops.map((shop) => (
                        <button
                            key={shop.id}
                            className={Styles.favorite_item}
                            onClick={() => navigate(`/agendamentoPage/${shop.id}`)}
                        >
                            <div className={Styles.favorite_item_thumb}>
                                <img
                                    src={shop.logoUrl || "./barbershop.jpg"}
                                    alt={`Logo da ${shop.name}`}
                                    onError={(e) => { e.target.src = "./barbershop.png"; }}
                                />
                            </div>
                            <div className={Styles.favorite_item_text}>
                                <h4>{shop.name}</h4>
                                <p>{shop.address}</p>
                            </div>
                        </button>
                    ))}
                </div>
            ) : (
                <p className={Styles.info_text}>Voce ainda nao favoritou nenhuma barbearia.</p>
            )}

        </div>
    )
}

export default Favorite_barbershops