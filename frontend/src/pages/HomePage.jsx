import { useState } from "react"
import Barbershops from "../components/HomePage/Barbershops/Barbershops"
import MenuNavBar from "../components/HomePage/Barbershops/MenuNavbar/MenuNavBar"
import Favorite_barbershops from "../components/HomePage/Favorite_barbershops/Favorite_barbershops"
import Navbar from "../components/HomePage/Navbar"
import SearchBar from "../components/HomePage/SearchBar"
import Styles from "./CSS/HomePage.module.css"

function HomePage() {
  const [searchTerm, setSearchTerm] = useState("");

  return (
    <div className={Styles.homepage_container}>
      <Navbar/>
      <SearchBar searchTerm={searchTerm} onSearchChange={setSearchTerm}/>
      <Favorite_barbershops/>
      <div className={Styles.h3_container_homepage}>
      <h3>Barbearias Disponiveis</h3>
      </div>
      <Barbershops searchTerm={searchTerm}/>
      <MenuNavBar/>
    </div>
  )
}

export default HomePage