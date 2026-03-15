import { useState } from "react"
import Carousel_barbershops from "./carousel_barbershops"
import Styles from "./CSS/Favorite_barbershops.module.css"

function Favorite_barbershops() {
    const [favoriteBarbershops, setFavoriteBarbershops] = useState([]);
    const [loading, setLoading] = useState(true);

    return (
        <div className={Styles.favorite_barbershops_container}>
            <h3>Minhas Barbearias Favoritas</h3>

            {loading ? (
                "Carregando Barbearias Favoritas..."
            ) : (favoriteBarbershops.length > 0 ? ('') : (
                <p>Nenhuma Barbearia Favoritqda</p>
            )
            )}
            
        </div>
    )
}

export default Favorite_barbershops