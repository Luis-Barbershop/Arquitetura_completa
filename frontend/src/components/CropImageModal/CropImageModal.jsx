import { useEffect, useRef, useState } from 'react';
import ReactCrop from 'react-image-crop';
import 'react-image-crop/dist/ReactCrop.css';
import styles from './CropImageModal.module.css';

const createInitialCrop = (aspect) => ({
  unit: '%',
  width: 80,
  height: 80 / aspect,
  x: 10,
  y: Math.max(0, (100 - 80 / aspect) / 2),
});

const cropImageToBlob = (image, crop, outputSize) => new Promise((resolve, reject) => {
  const canvas = document.createElement('canvas');
  const ctx = canvas.getContext('2d');

  if (!ctx) {
    reject(new Error('Canvas indisponível para recorte.'));
    return;
  }

  canvas.width = outputSize.width;
  canvas.height = outputSize.height;

  const scaleX = image.naturalWidth / image.width;
  const scaleY = image.naturalHeight / image.height;
  const sourceX = (crop.x || 0) * scaleX;
  const sourceY = (crop.y || 0) * scaleY;
  const sourceWidth = (crop.width || image.width) * scaleX;
  const sourceHeight = (crop.height || image.height) * scaleY;

  ctx.imageSmoothingQuality = 'high';
  ctx.drawImage(
    image,
    sourceX,
    sourceY,
    sourceWidth,
    sourceHeight,
    0,
    0,
    outputSize.width,
    outputSize.height,
  );

  canvas.toBlob((blob) => {
    if (blob) resolve(blob);
    else reject(new Error('Não foi possível gerar a imagem recortada.'));
  }, 'image/jpeg', 0.92);
});

const percentCropToPixelCrop = (percentCrop, image) => ({
  x: ((percentCrop.x || 0) / 100) * image.width,
  y: ((percentCrop.y || 0) / 100) * image.height,
  width: ((percentCrop.width || 100) / 100) * image.width,
  height: ((percentCrop.height || 100) / 100) * image.height,
});

function CropImageModal({
  src,
  title = 'Ajustar imagem',
  aspect = 1,
  outputSize = { width: 600, height: 600 },
  onCancel,
  onConfirm,
}) {
  const imageRef = useRef(null);
  const [crop, setCrop] = useState(() => createInitialCrop(aspect));
  const [completedCrop, setCompletedCrop] = useState(null);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    setCrop(createInitialCrop(aspect));
    setCompletedCrop(null);
  }, [aspect, src]);

  const handleConfirm = async () => {
    if (!imageRef.current) return;

    const cropToApply = completedCrop?.width && completedCrop?.height
      ? completedCrop
      : percentCropToPixelCrop(crop, imageRef.current);

    try {
      setIsSaving(true);
      const blob = await cropImageToBlob(imageRef.current, cropToApply, outputSize);
      await onConfirm(blob);
    } finally {
      setIsSaving(false);
    }
  };

  if (!src) return null;

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true" aria-label={title}>
      <div className={styles.modal}>
        <div className={styles.header}>
          <h2>{title}</h2>
          <button type="button" className={styles.closeButton} onClick={onCancel} aria-label="Fechar">
            ×
          </button>
        </div>

        <div className={styles.cropArea}>
          <ReactCrop
            crop={crop}
            aspect={aspect}
            onChange={(_, percentCrop) => setCrop(percentCrop)}
            onComplete={(pixelCrop) => setCompletedCrop(pixelCrop)}
          >
            <img
              ref={imageRef}
              src={src}
              alt="Imagem para recorte"
              className={styles.image}
              onLoad={() => setCompletedCrop(null)}
            />
          </ReactCrop>
        </div>

        <div className={styles.actions}>
          <button type="button" className={styles.cancelButton} onClick={onCancel} disabled={isSaving}>
            Cancelar
          </button>
          <button type="button" className={styles.confirmButton} onClick={handleConfirm} disabled={isSaving}>
            {isSaving ? 'Aplicando...' : 'Aplicar recorte'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default CropImageModal;
