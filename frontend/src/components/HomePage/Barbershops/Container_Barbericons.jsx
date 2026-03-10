import { useState, useEffect } from "react";
import Styles from "./CSS/ContainerBarberIcons.module.css"
import { useNavigate } from "react-router-dom";
import { FaStar, FaStarHalfAlt, FaRegStar } from "react-icons/fa";

function Container_Barbericons({ name, address, image, id, onFavoriteChange, rating = 4.3, services = [] }) {

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

  const renderStars = (value) => {
    const stars = [];
    for (let i = 1; i <= 5; i++) {
      if (value >= i) {
        stars.push(<FaStar key={i} className={Styles.star_filled} />);
      } else if (value >= i - 0.5) {
        stars.push(<FaStarHalfAlt key={i} className={Styles.star_filled} />);
      } else {
        stars.push(<FaRegStar key={i} className={Styles.star_empty} />);
      }
    }
    return stars;
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
        <div className={Styles.rating_container}>
          <div className={Styles.stars}>{renderStars(rating)}</div>
          <span className={Styles.rating_text}>{rating.toFixed(1)} estrelas</span>
        </div>
        {services.length > 0 && (
          <div className={Styles.services_container}>
            {services.slice(0, 2).map((service) => (
              <span key={service.id} className={Styles.service_tag}>
                {service.activityName}
              </span>
            ))}
            {services.length > 2 && (
              <span className={Styles.service_more}>+{services.length - 2}</span>
            )}
          </div>
        )}
        <p>{address}</p>
      </div>
    </div>
  )
}

export default Container_Barbericons