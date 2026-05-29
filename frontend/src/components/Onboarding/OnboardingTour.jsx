import { useEffect, useId, useMemo, useRef, useState } from 'react';
import styles from './OnboardingTour.module.css';

const VIEWPORT_PADDING = 12;
const DEFAULT_OFFSET = 14;
const DEFAULT_SPOTLIGHT_PADDING = 10;

const clamp = (value, min, max) => Math.min(Math.max(value, min), max);

const isElementVisible = (element) => {
  if (!element) return false;
  const style = window.getComputedStyle(element);
  if (style.display === 'none' || style.visibility === 'hidden' || Number(style.opacity) === 0) {
    return false;
  }

  const rect = element.getBoundingClientRect();
  return rect.width > 0 && rect.height > 0;
};

const resolveTarget = (step) => {
  if (!step) return null;

  const selectors = [step.selector, ...(step.fallbackSelectors || [])].filter(Boolean);
  if (!selectors.length) return null;

  for (const selector of selectors) {
    const element = document.querySelector(selector);
    if (!isElementVisible(element)) continue;

    return {
      element,
      rect: element.getBoundingClientRect(),
    };
  }

  return null;
};

const buildPlacementCandidates = (preferredPlacement) => {
  const fallback = ['bottom', 'top', 'right', 'left'];
  if (!preferredPlacement) return fallback;
  return [preferredPlacement, ...fallback.filter((item) => item !== preferredPlacement)];
};

const calculatePositionByPlacement = ({ placement, targetRect, cardRect, offset }) => {
  if (placement === 'top') {
    return {
      top: targetRect.top - cardRect.height - offset,
      left: targetRect.left + (targetRect.width - cardRect.width) / 2,
    };
  }

  if (placement === 'left') {
    return {
      top: targetRect.top + (targetRect.height - cardRect.height) / 2,
      left: targetRect.left - cardRect.width - offset,
    };
  }

  if (placement === 'right') {
    return {
      top: targetRect.top + (targetRect.height - cardRect.height) / 2,
      left: targetRect.right + offset,
    };
  }

  return {
    top: targetRect.bottom + offset,
    left: targetRect.left + (targetRect.width - cardRect.width) / 2,
  };
};

const fitsViewport = ({ top, left }, cardRect) => (
  top >= VIEWPORT_PADDING
  && left >= VIEWPORT_PADDING
  && top + cardRect.height <= window.innerHeight - VIEWPORT_PADDING
  && left + cardRect.width <= window.innerWidth - VIEWPORT_PADDING
);

const getCardPosition = ({ targetRect, cardRect, preferredPlacement, offset }) => {
  const placements = buildPlacementCandidates(preferredPlacement);

  for (const placement of placements) {
    const candidate = calculatePositionByPlacement({
      placement,
      targetRect,
      cardRect,
      offset,
    });

    if (fitsViewport(candidate, cardRect)) {
      return {
        top: candidate.top,
        left: candidate.left,
      };
    }
  }

  const fallback = calculatePositionByPlacement({
    placement: preferredPlacement || 'bottom',
    targetRect,
    cardRect,
    offset,
  });

  return {
    top: clamp(fallback.top, VIEWPORT_PADDING, Math.max(VIEWPORT_PADDING, window.innerHeight - cardRect.height - VIEWPORT_PADDING)),
    left: clamp(fallback.left, VIEWPORT_PADDING, Math.max(VIEWPORT_PADDING, window.innerWidth - cardRect.width - VIEWPORT_PADDING)),
  };
};

const isTargetFullyVisible = (rect) => {
  if (!rect) return false;
  return (
    rect.top >= 40
    && rect.left >= 0
    && rect.bottom <= window.innerHeight - 40
    && rect.right <= window.innerWidth
  );
};

const findNextValidStep = (resolvedSteps, startIndex, direction = 1) => {
  if (!resolvedSteps.length) return -1;

  let index = startIndex;
  while (index >= 0 && index < resolvedSteps.length) {
    if (resolvedSteps[index]) return index;
    index += direction;
  }

  return -1;
};

function OnboardingTour({
  open,
  pageTitle,
  steps,
  onComplete,
  onSkip,
  onClose,
}) {
  const [currentStepIndex, setCurrentStepIndex] = useState(0);
  const [layoutVersion, setLayoutVersion] = useState(0);
  const [cardPosition, setCardPosition] = useState({ top: VIEWPORT_PADDING, left: VIEWPORT_PADDING });
  const cardRef = useRef(null);
  const rafRef = useRef(null);
  const maskId = useId().replace(/:/g, '');

  const totalSteps = steps.length;

  const resolvedSteps = useMemo(() => {
    if (!open) return [];
    return steps.map((step) => resolveTarget(step));
  }, [open, steps, layoutVersion]);

  const currentStep = useMemo(() => {
    if (!steps.length) return null;
    return steps[currentStepIndex];
  }, [steps, currentStepIndex]);

  const currentTarget = resolvedSteps[currentStepIndex] || null;
  const isLastStep = findNextValidStep(resolvedSteps, currentStepIndex + 1, 1) === -1;

  useEffect(() => {
    if (open) {
      const firstValidStep = findNextValidStep(resolvedSteps, 0, 1);
      if (firstValidStep === -1) {
        onClose?.();
        return;
      }
      setCurrentStepIndex(firstValidStep);
      setLayoutVersion((prev) => prev + 1);
    }
  }, [open, resolvedSteps, onClose]);

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

  useEffect(() => {
    if (!open) return undefined;

    const scheduleUpdate = () => {
      if (rafRef.current) return;
      rafRef.current = window.requestAnimationFrame(() => {
        rafRef.current = null;
        setLayoutVersion((prev) => prev + 1);
      });
    };

    window.addEventListener('resize', scheduleUpdate);
    window.addEventListener('scroll', scheduleUpdate, true);

    const observer = new MutationObserver(scheduleUpdate);
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      attributes: true,
    });

    return () => {
      window.removeEventListener('resize', scheduleUpdate);
      window.removeEventListener('scroll', scheduleUpdate, true);
      observer.disconnect();
      if (rafRef.current) {
        window.cancelAnimationFrame(rafRef.current);
        rafRef.current = null;
      }
    };
  }, [open]);

  useEffect(() => {
    if (!open || !resolvedSteps.length) return;

    if (resolvedSteps[currentStepIndex]) return;

    const nextIndex = findNextValidStep(resolvedSteps, currentStepIndex + 1, 1);
    if (nextIndex !== -1) {
      setCurrentStepIndex(nextIndex);
      return;
    }

    const previousIndex = findNextValidStep(resolvedSteps, currentStepIndex - 1, -1);
    if (previousIndex !== -1) {
      setCurrentStepIndex(previousIndex);
      return;
    }

    onClose?.();
  }, [open, currentStepIndex, onClose, resolvedSteps]);

  useEffect(() => {
    if (!open || !currentTarget?.rect) return;
    if (isTargetFullyVisible(currentTarget.rect)) return;

    currentTarget.element.scrollIntoView({
      behavior: 'smooth',
      block: 'center',
      inline: 'nearest',
    });
  }, [open, currentTarget]);

  useEffect(() => {
    if (!open || !currentTarget?.rect || !cardRef.current) return;

    const cardRect = cardRef.current.getBoundingClientRect();
    const position = getCardPosition({
      targetRect: currentTarget.rect,
      cardRect,
      preferredPlacement: currentStep?.placement,
      offset: currentStep?.offset || DEFAULT_OFFSET,
    });

    setCardPosition(position);
  }, [open, currentStep, currentTarget, layoutVersion]);

  if (!open || !currentStep || !currentTarget?.rect) return null;

  const spotlightPadding = currentStep.spotlightPadding ?? DEFAULT_SPOTLIGHT_PADDING;
  const spotlightRect = {
    top: Math.max(0, currentTarget.rect.top - spotlightPadding),
    left: Math.max(0, currentTarget.rect.left - spotlightPadding),
    width: Math.min(window.innerWidth, currentTarget.rect.width + spotlightPadding * 2),
    height: Math.min(window.innerHeight, currentTarget.rect.height + spotlightPadding * 2),
  };
  const spotlightRadius = currentStep.spotlightRadius ?? 14;

  const handleNext = () => {
    if (isLastStep) {
      onComplete?.();
      return;
    }

    const nextIndex = findNextValidStep(resolvedSteps, currentStepIndex + 1, 1);
    if (nextIndex !== -1) {
      setCurrentStepIndex(nextIndex);
    } else {
      onComplete?.();
    }
  };

  const handlePrevious = () => {
    const previousIndex = findNextValidStep(resolvedSteps, currentStepIndex - 1, -1);
    if (previousIndex !== -1) {
      setCurrentStepIndex(previousIndex);
    }
  };

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true" aria-label="Onboarding da página">
      <svg className={styles.scrim} viewBox={`0 0 ${window.innerWidth} ${window.innerHeight}`} preserveAspectRatio="none" aria-hidden="true">
        <defs>
          <mask id={maskId}>
            <rect x="0" y="0" width={window.innerWidth} height={window.innerHeight} fill="white" />
            <rect
              x={spotlightRect.left}
              y={spotlightRect.top}
              width={spotlightRect.width}
              height={spotlightRect.height}
              rx={spotlightRadius}
              ry={spotlightRadius}
              fill="black"
            />
          </mask>
        </defs>
        <rect
          x="0"
          y="0"
          width={window.innerWidth}
          height={window.innerHeight}
          className={styles.scrimFill}
          mask={`url(#${maskId})`}
        />
      </svg>

      <div
        className={styles.spotlightBorder}
        style={{
          top: spotlightRect.top,
          left: spotlightRect.left,
          width: spotlightRect.width,
          height: spotlightRect.height,
          borderRadius: `${spotlightRadius}px`,
        }}
      />

      <div
        ref={cardRef}
        className={styles.card}
        style={{
          top: cardPosition.top,
          left: cardPosition.left,
        }}
      >
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
              disabled={findNextValidStep(resolvedSteps, currentStepIndex - 1, -1) === -1}
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
