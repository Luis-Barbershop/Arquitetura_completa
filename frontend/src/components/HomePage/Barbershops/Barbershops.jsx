import Container_Barbericons from "./Container_Barbericons"
import Styles from "./CSS/Barbershops.module.css"
import { getAllBarbershops, getShopServices } from "../../../services/barbershopService"
import { useEffect, useState, useMemo } from "react"


function Barbershops({ searchTerm }) {
  const [barbershops, setBarbershops] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAllBarbershops = async () => {
      const shops = await getAllBarbershops();

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
  }, []);

  const filtered = useMemo(() => {
    if (!searchTerm) return barbershops;
    const term = searchTerm.toLowerCase();
    return barbershops.filter((shop) =>
      shop.name.toLowerCase().includes(term)
    );
  }, [barbershops, searchTerm]);


  return (
    <div className={Styles.barbershops_container}>
      {loading ? (
        <p>Carregando Barbearias...</p>
      ) : filtered.length > 0 ? (
        filtered.map((shop) => (
          <Container_Barbericons 
          key={shop.id}
          name={shop.name}
          address={shop.address}
          image={shop.logoUrl || "./barbershop.jpg"}
          id={shop.id}
          rating={shop.averageRating}
          reviewsCount={shop.reviewsCount}
          services={shop.services || []} />
        ))
      ) : (
        <p>Nenhuma barbearia encontrada.</p>
      )}

    </div>
  )
}

export default Barbershops