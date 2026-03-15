import Styles from "./CSS/SearchBar.module.css"

function SearchBar({ searchTerm, onSearchChange }) {
  return (
    <div className={Styles.search_bar_container}>
        <div><img src="./Icons/search_icon.png" alt="Icone de Lupa" /></div>
        <input
          type="text"
          name="searchBar"
          id={Styles.search_input}
          placeholder="Buscar Barbearia"
          value={searchTerm}
          onChange={(e) => onSearchChange(e.target.value)}
        />
    </div>
  )
}

export default SearchBar