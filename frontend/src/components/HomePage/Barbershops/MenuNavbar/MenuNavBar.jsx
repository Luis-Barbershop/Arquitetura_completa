import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { HiHome, HiOutlineHome } from 'react-icons/hi';
import { BsCalendarCheck, BsCalendarCheckFill } from 'react-icons/bs';
import { FiUser } from 'react-icons/fi';
import { FaUser } from 'react-icons/fa';
import Styles from './MenuNavbar.module.css';

const navItems = [
  { id: 'home', label: 'Home', path: '/homepage', IconActive: HiHome, IconInactive: HiOutlineHome },
  { id: 'agendamentos', label: 'Agendamentos', path: '/meus-agendamentos', IconActive: BsCalendarCheckFill, IconInactive: BsCalendarCheck },
  { id: 'perfil', label: 'Meu Perfil', path: '/perfil', IconActive: FaUser, IconInactive: FiUser },
];

function MenuNavBar() {
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <nav className={Styles.navbar_container}>
      <ul className={Styles.navbar_list}>
        {navItems.map((item) => {
          const isActive = location.pathname === item.path;
          const Icon = isActive ? item.IconActive : item.IconInactive;

          return (
            <li key={item.id}>
              <button
                className={isActive ? Styles.nav_item_active : Styles.nav_item}
                onClick={() => navigate(item.path)}
              >
                <Icon className={Styles.nav_icon} />
                <span className={Styles.nav_label}>{item.label}</span>
              </button>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}

export default MenuNavBar;