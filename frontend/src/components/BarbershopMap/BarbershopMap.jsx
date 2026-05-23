import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import { FiExternalLink, FiMapPin } from 'react-icons/fi';
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
 * @param {string} address
 */
export default function BarbershopMap({ latitude, longitude, barbershopName, address }) {
    const lat = Number(latitude);
    const lng = Number(longitude);
    const hasCoordinates = Number.isFinite(lat) && Number.isFinite(lng);
    const hasAddress = Boolean(address?.trim());

    if (!hasCoordinates && !hasAddress) return null;

    const mapsQuery = hasCoordinates ? `${lat},${lng}` : address.trim();
    const googleMapsUrl = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(mapsQuery)}`;

    return (
        <div className={styles.wrapper}>
            {hasCoordinates && (
                <MapContainer
                    center={[lat, lng]}
                    zoom={16}
                    scrollWheelZoom={false}
                    className={styles.map}
                    key={`${lat}-${lng}`}
                >
                    <TileLayer
                        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    />
                    <Marker position={[lat, lng]}>
                        <Popup>
                            <strong>{barbershopName || 'Barbearia'}</strong>
                            {hasAddress && <span>{address}</span>}
                        </Popup>
                    </Marker>
                </MapContainer>
            )}

            <div className={styles.locationInfo}>
                {hasAddress && (
                    <p className={styles.address}>
                        <FiMapPin size={16} aria-hidden="true" />
                        <span>{address}</span>
                    </p>
                )}
                <a
                    href={googleMapsUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className={styles.googleMapsLink}
                >
                    <FiExternalLink size={15} aria-hidden="true" />
                    Abrir no Google Maps
                </a>
            </div>
        </div>
    );
}
