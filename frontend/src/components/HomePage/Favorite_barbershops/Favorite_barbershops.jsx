import { useCallback, useEffect, useState } from "react"
import { useNavigate } from "react-router-dom";
import Styles from "./CSS/Favorite_barbershops.module.css"
import { getAllBarbershops } from "../../../services/barbershopService";

function Favorite_barbershops({ favoriteIds = [] }) {
    const [favoriteBarbershops, setFavoriteBarbershops] = useState([]);
    const [loading, setLoading] = useState(true);
    const [hasError, setHasError] = useState(false);
    const navigate = useNavigate();

    const loadFavorites = useCallback(async () => {
        setLoading(true);
        setHasError(false);

        try {
            if (favoriteIds.length === 0) {
                setFavoriteBarbershops([]);
                setLoading(false);
                return;
            }

            const shops = await getAllBarbershops();
            const filteredFavorites = shops.filter((shop) => favoriteIds.includes(shop.id));
            setFavoriteBarbershops(filteredFavorites);
        } catch (error) {
            console.error("Erro ao carregar favoritos:", error);
            setHasError(true);
            setFavoriteBarbershops([]);
        } finally {
            setLoading(false);
        }
    }, [favoriteIds]);

    useEffect(() => {
        loadFavorites();
    }, [loadFavorites]);

    return (
        <div className={Styles.favorite_barbershops_container}>
            <div className={Styles.favorite_header}>
                <h3>Minhas barbearias favoritas</h3>
                <span>{favoriteBarbershops.length} salva(s)</span>
            </div>

            {loading ? (
                <p className={`${Styles.info_text} ca-state ca-state--loading`}>Carregando favoritas...</p>
            ) : hasError ? (
                <p className={`${Styles.info_text} ca-state ca-state--error`}>
                    Nao foi possivel carregar suas favoritas agora. Tente novamente em instantes.
                </p>
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
                <p className={`${Styles.info_text} ca-state ca-state--empty`}>Voce ainda nao favoritou nenhuma barbearia.</p>
            )}

        </div>
    )
}

export default Favorite_barbershops