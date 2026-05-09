import Container_Barbericons from "./Container_Barbericons"
import Styles from "./CSS/Barbershops.module.css"
import { getBarbershops, getShopServices } from "../../../services/barbershopService"
import { useEffect, useState, useMemo } from "react"


function Barbershops({ searchTerm, favoriteIds = [], onToggleFavorite, userLocation }) {
  const [barbershops, setBarbershops] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAllBarbershops = async () => {
      setLoading(true);
      const shops = await getBarbershops(userLocation || {});

      const shopsWithServices = await Promise.all(
        shops.map(async (shop) => {
          const services = await getShopServices(shop.id);
          return { ...shop, services };
        })
      );

      setBarbershops(shopsWithServices);
      setLoading(false);
    }

    fetchAllBarbershops()
  }, [userLocation]);

  const filtered = useMemo(() => {
    if (!searchTerm) return barbershops;
    const term = searchTerm.toLowerCase();
    return barbershops.filter((shop) =>
      shop.name.toLowerCase().includes(term)
    );
  }, [barbershops, searchTerm]);


  const skeletons = Array.from({ length: 6 });

  return (
    <div className={Styles.barbershops_container}>
      {loading ? (
        skeletons.map((_, i) => (
          <div key={i} className={Styles.skeleton_card}>
            <div className={Styles.skeleton_thumb} />
            <div className={Styles.skeleton_body}>
              <div className={`${Styles.skeleton_line} ${Styles.skeleton_line_medium}`} />
              <div className={`${Styles.skeleton_line} ${Styles.skeleton_line_long}`} />
              <div className={`${Styles.skeleton_line} ${Styles.skeleton_line_short}`} />
            </div>
          </div>
        ))
      ) : filtered.length > 0 ? (
        filtered.map((shop) => (
          <Container_Barbericons 
          key={shop.id}
          name={shop.name}
          address={shop.address}
          image={shop.logoUrl || "./barbershop.jpg"}
          id={shop.id}
          isFavorite={favoriteIds.includes(shop.id)}
          onToggleFavorite={onToggleFavorite}
          rating={shop.averageRating}
          reviewsCount={shop.reviewsCount}
          services={shop.services || []}
          distanceKm={shop.distanceKm ?? null} />
        ))
      ) : (
        <p>Nenhuma barbearia encontrada.</p>
      )}

    </div>
  )
}

export default Barbershops