import React from 'react'
import Styles from "./CSS/invoicing.module.css"

function Invoicing() {
  return (

    <div className={Styles.containerFaturamento}>
        <div className={Styles.containerFaturamentoLeft}>
        <h2>Faturamento Hoje:</h2>
        <h1>R$ 800,00</h1>
        <p>↝ + 15% que ontem</p>
        </div>

        <div className={Styles.containerFaturamentoRight}>
            <img src="/Icons/moneyIcon.png" alt="Icone de Dinheiro" />
        </div>
    </div>
  )
}

export default Invoicing