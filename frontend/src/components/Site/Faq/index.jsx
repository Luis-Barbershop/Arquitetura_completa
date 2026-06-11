import { useState } from 'react'
import Styles from './Faq.module.css'

const faqItems = [
  {
    question: 'Como faco para agendar um horario?',
    answer:
      'Basta entrar na sua conta, escolher a barbearia, selecionar o serviço e confirmar o melhor horário disponível.',
  },
  {
    question: 'Posso escolher um barbeiro especifico?',
    answer:
      'Sim. Durante o agendamento você pode filtrar os profissionais e selecionar aquele que preferir para o atendimento.',
  },
  {
    question: 'Como funciona para os profissionais?',
    answer:
      'O barbeiro recebe os pedidos em tempo real, organiza a agenda em um painel unico e acompanha os atendimentos do dia.',
  },
  {
    question: 'Consigo remarcar ou cancelar um agendamento?',
    answer:
      'Sim. Na área de agendamentos você pode remarcar ou cancelar dentro das regras definidas pela barbearia.',
  },
  {
    question: 'A plataforma envia lembretes de horario?',
    answer:
      'Envia sim. O sistema notifica você antes do atendimento para reduzir esquecimentos e manter a rotina organizada.',
  },
]

function Faq() {
  const [openIndex, setOpenIndex] = useState(0)

  const toggleQuestion = (index) => {
    setOpenIndex((current) => (current === index ? -1 : index))
  }

  return (
    <section className={Styles.faqSection}>
      <div className={Styles.leftSide}>
        <p className={Styles.label}>SUPORTE</p>
        <h2>Tire às suas dúvidas</h2>
      </div>

      <div className={Styles.rightSide}>
        <h3 className={Styles.rightTitle}>Algumas perguntas</h3>
        {faqItems.map((item, index) => {
          const isOpen = openIndex === index

          return (
            <article className={Styles.faqItem} key={item.question}>
              <button
                type="button"
                className={Styles.questionButton}
                onClick={() => toggleQuestion(index)}
                aria-expanded={isOpen}
              >
                <span>{item.question}</span>
                <span className={Styles.icon}>{isOpen ? '-' : '+'}</span>
              </button>

              <div className={`${Styles.answerWrapper} ${isOpen ? Styles.answerOpen : ''}`}>
                <p className={Styles.answer}>{item.answer}</p>
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}

export default Faq
