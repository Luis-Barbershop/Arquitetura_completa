import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import styles from './BarbershopMap.module.css';

// Corrige o ícone padrão do Leaflet que quebra com bundlers
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

/**
 * @param {number} latitude
 * @param {number} longitude
 * @param {string} barbershopName
 */
export default function BarbershopMap({ latitude, longitude, barbershopName }) {
    if (!latitude || !longitude) return null;

    const googleMapsUrl = `https://www.google.com/maps/search/?api=1&query=${latitude},${longitude}`;

    return (
        <div className={styles.wrapper}>
            <MapContainer
                center={[latitude, longitude]}
                zoom={16}
                scrollWheelZoom={false}
                className={styles.map}
                key={`${latitude}-${longitude}`}
            >
                <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                <Marker position={[latitude, longitude]}>
                    <Popup>{barbershopName || 'Barbearia'}</Popup>
                </Marker>
            </MapContainer>

            <a
                href={googleMapsUrl}
                target="_blank"
                rel="noopener noreferrer"
                className={styles.googleMapsLink}
            >
                📍 Abrir no Google Maps
            </a>
        </div>
    );
}
