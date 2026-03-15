import { useState, useEffect } from "react";
import Styles from "./CSS/ContainerBarberIcons.module.css"
import { useNavigate } from "react-router-dom";

function Container_Barbericons({ name, address, image, id, onFavoriteChange }) {

  const navigate = useNavigate();
  const [isFavorite, setIsFavorite] = useState(false);

  useEffect(() => {
    const favorites = JSON.parse(localStorage.getItem("favoriteBarbershops") || "[]");
    setIsFavorite(favorites.includes(id));
  }, [id]);

  const handleClick = () => {
    navigate(`/agendamentoPage/${id}`);
  };

  const handleFavorite = (e) => {
    e.stopPropagation();
    const favorites = JSON.parse(localStorage.getItem("favoriteBarbershops") || "[]");
    let updated;
    if (favorites.includes(id)) {
      updated = favorites.filter((favId) => favId !== id);
    } else {
      updated = [...favorites, id];
    }
    localStorage.setItem("favoriteBarbershops", JSON.stringify(updated));
    setIsFavorite(!isFavorite);
    if (onFavoriteChange) onFavoriteChange();
  };

  return (
    <div className={Styles.barbershopsicons_container} onClick={handleClick}>
      <div className={Styles.image_icon_barbershop_container}>
        <img
          src={image}
          alt={`Logo da ${name}`}
          onError={(e) => { e.target.src = "./barbershop.png"; }}
        />
        <button
          className={`${Styles.favorite_button} ${isFavorite ? Styles.favorited : ""}`}
          onClick={handleFavorite}
          aria-label={isFavorite ? "Remover dos favoritos" : "Adicionar aos favoritos"}
        >
          {isFavorite ? "♥" : "♡"}
        </button>
      </div>

      <div className={Styles.text_icon_barbershop_container}>
        <h4>{name}</h4>
        <p>{address}</p>
      </div>
    </div>
  )
}

export default Container_Barbericons