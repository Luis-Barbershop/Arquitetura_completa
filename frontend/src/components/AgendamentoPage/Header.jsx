import { useNavigate } from "react-router-dom"
import Styles from "./CSS/Header.module.css"

function Header() {
    const navigate = useNavigate();

    const handleBackPage = () => {
        navigate("/homepage")
    }
    return (
        <div className={Styles.header_container}>
            <div className={Styles.arrow_back_page} onClick={handleBackPage}>
                <img src="/Icons/left_arrow.png" alt="Flecha para esquerda" />
            </div>
            <h2>Agendamento</h2>
        </div>
    )
}

export default Header