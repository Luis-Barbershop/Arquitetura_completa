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
            {loading ? (
                <div className={Styles.stories_list}>
                    {Array.from({ length: 5 }).map((_, i) => (
                        <div key={i} className={Styles.story_skeleton}>
                            <div className={Styles.story_skeleton_avatar} />
                            <div className={Styles.story_skeleton_label} />
                        </div>
                    ))}
                </div>
            ) : hasError ? (
                <p className={`${Styles.info_text} ca-state ca-state--error`}>
                    Nao foi possivel carregar suas favoritas agora. Tente novamente em instantes.
                </p>
            ) : favoriteBarbershops.length > 0 ? (
                <div className={Styles.stories_list}>
                    {favoriteBarbershops.map((shop) => (
                        <button
                            key={shop.id}
                            className={Styles.story_item}
                            onClick={() => navigate(`/agendamentoPage/${shop.id}`)}
                        >
                            <div className={Styles.story_avatar}>
                                <img
                                    src={shop.logoUrl || "./barbershop.jpg"}
                                    alt={`Logo da ${shop.name}`}
                                    onError={(e) => { e.target.src = "./barbershop.png"; }}
                                />
                            </div>
                            <span className={Styles.story_name}>{shop.name}</span>
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