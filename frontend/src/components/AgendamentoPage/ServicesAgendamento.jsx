import React from 'react';
import Styles from './CSS/ServicesAgendamento.module.css';

// Recebe as props enviadas pelo AgendamentoPage.jsx
function ServicesAgendamento({ data, isSelected, onToggle, disabled = false }) {
    
    // Formatação de moeda para ficar bonito (R$ 25,00)
    const formattedPrice = new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL'
    }).format(data.price);

    return (
        <div 
            className={`${Styles.service_card} ${isSelected ? Styles.selected : ''} ${disabled ? Styles.disabled : ''}`} 
            onClick={() => {
                if (disabled) return;
                onToggle();
            }}
        >
            <div className={Styles.info}>
                <span className={Styles.name}>{data.activityName}</span>
                <span className={Styles.details}>{data.durationMinutes} min</span>
            </div>
            <div className={Styles.price}>
                {formattedPrice}
            </div>
        </div>
    );
}

export default ServicesAgendamento;