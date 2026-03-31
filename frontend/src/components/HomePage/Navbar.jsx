// src/components/HomePage/Navbar.jsx
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import Styles from "./CSS/Navbar.module.css";
import { logoutUser } from "../../services/authService"; // Importação da função de logout

function Navbar() {
  const navigate = useNavigate();
  const userName = localStorage.getItem("userName") || "Cliente";
  const firstName = userName.split(" ")[0];
  const [isLogoutModalOpen, setIsLogoutModalOpen] = useState(false);

  const handleOpenLogoutModal = () => {
    setIsLogoutModalOpen(true);
  };

  const handleCloseLogoutModal = () => {
    setIsLogoutModalOpen(false);
  };

  const handleConfirmLogout = () => {
    logoutUser();
    navigate("/identificacao", { state: { mode: "login", role: "customer" } });
  };

  return (
    <div className={Styles.navbar_container}>
      <div className={Styles.navbar_content}>
        <div className={Styles.brand_block}>
          <h3>CortaAI</h3>
          <p>Bem-vindo, {firstName}</p>
        </div>

        <div className={Styles.actions_block}>
          <button
            className={Styles.secondary_button}
            onClick={() => navigate("/meus-agendamentos")}
          >
            Meus agendamentos
          </button>

          <button
            className={Styles.logout_button}
            onClick={handleOpenLogoutModal}
          >
            Sair
          </button>
        </div>
      </div>

      {isLogoutModalOpen && (
        <div className={Styles.modal_backdrop} onClick={handleCloseLogoutModal}>
          <div className={Styles.modal_card} onClick={(e) => e.stopPropagation()}>
            <p className={Styles.modal_kicker}>CONFIRMAR SAIDA</p>
            <h4 className={Styles.modal_title}>Deseja sair da sua conta?</h4>
            <p className={Styles.modal_subtitle}>Voce sera redirecionado para a tela de login do cliente.</p>

            <div className={Styles.modal_actions}>
              <button type="button" className={Styles.modal_secondary_button} onClick={handleCloseLogoutModal}>
                Permanecer
              </button>
              <button type="button" className={Styles.modal_danger_button} onClick={handleConfirmLogout}>
                Sair da conta
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Navbar;