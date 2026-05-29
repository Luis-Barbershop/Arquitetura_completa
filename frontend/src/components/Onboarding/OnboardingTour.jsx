import { useEffect, useMemo, useState } from 'react';
import styles from './OnboardingTour.module.css';

function OnboardingTour({
  open,
  pageTitle,
  steps,
  onComplete,
  onSkip,
  onClose,
}) {
  const [currentStepIndex, setCurrentStepIndex] = useState(0);

  useEffect(() => {
    if (open) {
      setCurrentStepIndex(0);
    }
  }, [open]);

  useEffect(() => {
    if (!open) return undefined;

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        onClose?.();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [open, onClose]);

  const totalSteps = steps.length;
  const isLastStep = currentStepIndex >= totalSteps - 1;

  const currentStep = useMemo(() => {
    if (!steps.length) return null;
    return steps[currentStepIndex];
  }, [steps, currentStepIndex]);

  if (!open || !currentStep) return null;

  const handleNext = () => {
    if (isLastStep) {
      onComplete?.();
      return;
    }
    setCurrentStepIndex((prev) => Math.min(prev + 1, totalSteps - 1));
  };

  const handlePrevious = () => {
    setCurrentStepIndex((prev) => Math.max(prev - 1, 0));
  };

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true" aria-label="Onboarding da página">
      <div className={styles.card}>
        <header className={styles.header}>
          <div>
            <p className={styles.kicker}>{pageTitle || 'Onboarding'}</p>
            <p className={styles.progress}>Passo {currentStepIndex + 1} de {totalSteps}</p>
          </div>
        </header>

        <h3 className={styles.title}>{currentStep.title}</h3>
        <p className={styles.description}>{currentStep.description}</p>

        <footer className={styles.footer}>
          <div className={styles.leftActions}>
            <button type="button" className={styles.button} onClick={onSkip}>
              Pular por agora
            </button>
            <button
              type="button"
              className={styles.button}
              onClick={handlePrevious}
              disabled={currentStepIndex === 0}
            >
              Voltar
            </button>
          </div>

          <div className={styles.rightActions}>
            <button type="button" className={styles.button} onClick={onClose}>
              Fechar
            </button>
            <button type="button" className={styles.primaryButton} onClick={handleNext}>
              {isLastStep ? 'Concluir' : 'Próximo'}
            </button>
          </div>
        </footer>
      </div>
    </div>
  );
}

export default OnboardingTour;
