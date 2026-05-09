import Styles from "./CSS/ContainerBarberIcons.module.css"
import { useNavigate } from "react-router-dom";
import { FaStar, FaStarHalfAlt, FaRegStar } from "react-icons/fa";

function Container_Barbericons({ name, address, image, id, isFavorite = false, onToggleFavorite, rating, reviewsCount = 0, services = [], distanceKm = null }) {

  const navigate = useNavigate();

  const handleClick = () => {
    navigate(`/barbearia/${id}`);
  };

  const handleFavorite = (e) => {
    e.stopPropagation();
    if (onToggleFavorite) onToggleFavorite(id, isFavorite);
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

  const numericRating = typeof rating === "number" ? rating : null;
  const safeRating = numericRating ?? 0;
  const hasReviews = (reviewsCount || 0) > 0 && numericRating !== null;

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
        {distanceKm !== null && (
          <span className={Styles.distance_badge}>📍 {distanceKm} km</span>
        )}
        <div className={Styles.rating_container}>
          <div className={Styles.stars}>{renderStars(safeRating)}</div>
          <span className={Styles.rating_text}>
            {hasReviews ? `${safeRating.toFixed(1)} estrelas` : "Sem avaliacoes"}
          </span>
        </div>
        {services.length > 0 && (
          <p className={Styles.services_summary}>
            {services.length} servicos disponiveis
          </p>
        )}
        <p>{address}</p>
      </div>
    </div>
  )
}

export default Container_Barbericons