import Styles from "./CSS/SearchBar.module.css"

function SearchBar({ searchTerm, onSearchChange }) {
  return (
    <div className={Styles.search_bar_container}>
      <div className={Styles.search_box}>
        <div className={Styles.search_icon_box}><img src="/Icons/search_icon.png" alt="Icone de Lupa" /></div>
        <input
          type="text"
          name="searchBar"
          id={Styles.search_input}
          placeholder="Busque por nome da barbearia"
          value={searchTerm}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </div>
    </div>
  )
}

export default SearchBar