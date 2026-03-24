import Styles from './Mockup.module.css'

function Mockup() {
  return (
    <div className={Styles.Container}>
        <div className={Styles.ImageContainer}>
            <img src="public/Mockup.png" alt="Mockup" />
        </div>

        <div className={Styles.DescriptionContainer}>
            <h3>Menos bagunça, mais controle na sua barbearia</h3>
            <p>No dia a dia corrido, organizar atendimentos, horários e a gestão da barbearia pode ser desafiador, especialmente quando tudo depende de processos manuais. Por isso, oferecemos uma solução que centraliza tudo em um só lugar, facilitando o controle da agenda e dos atendimentos, trazendo mais organização e tranquilidade para a rotina. Assim, você ganha tempo para focar no que realmente importa: o atendimento, a experiência do cliente e o crescimento do seu negócio.</p>
        </div>
    </div>
  )
}

export default Mockup