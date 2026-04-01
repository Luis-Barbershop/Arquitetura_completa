import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createBarbershop } from '../services/barbershopService';
// Vamos usar um estilo inline básico para ser rápido, mas compatível com seu tema dark
import Styles from './CSS/HomePage.module.css'; 

function CreateBarbershopPage() {
    const navigate = useNavigate();
    
    const [name, setName] = useState("");
    const [cnpj, setCnpj] = useState("");
    const [address, setAddress] = useState("");
    const [file, setFile] = useState(null);
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        try {
            // Chama o serviço
            await createBarbershop({ name, cnpj, address }, file);
            
            alert("Barbearia criada com sucesso! Para liberar as permissoes de dono, entre novamente.");
            // O backend coloca a role de dono no JWT. Forcamos novo login para atualizar o token.
            localStorage.clear();
            navigate('/identificacao', { state: { mode: 'login', role: 'barber' } });
        } catch (error) {
            console.error(error);
            alert("Erro ao criar barbearia. Verifique os dados.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ backgroundColor: '#1E1E1E', minHeight: '100vh', color: 'white', padding: '20px', display:'flex', justifyContent:'center', alignItems:'center' }}>
            <div style={{ width: '100%', maxWidth: '500px', background: '#2A2A2A', padding: '30px', borderRadius: '10px' }}>
                
                <h2 style={{ textAlign: 'center', color: '#D4AF37', marginBottom: '20px' }}>Registrar Minha Barbearia</h2>
                
                <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                    
                    <div>
                        <label style={{ display: 'block', marginBottom: '5px' }}>Nome da Barbearia</label>
                        <input 
                            type="text" 
                            value={name} 
                            onChange={e => setName(e.target.value)} 
                            required 
                            style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #444', background: '#0f0f0f', color: 'white' }}
                        />
                    </div>

                    <div>
                        <label style={{ display: 'block', marginBottom: '5px' }}>CNPJ</label>
                        <input 
                            type="text" 
                            value={cnpj} 
                            onChange={e => setCnpj(e.target.value)} 
                            required 
                            placeholder="00.000.000/0001-00"
                            style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #444', background: '#0f0f0f', color: 'white' }}
                        />
                    </div>

                    <div>
                        <label style={{ display: 'block', marginBottom: '5px' }}>Endereço Completo</label>
                        <input 
                            type="text" 
                            value={address} 
                            onChange={e => setAddress(e.target.value)} 
                            required 
                            style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #444', background: '#0f0f0f', color: 'white' }}
                        />
                    </div>

                    <div>
                        <label style={{ display: 'block', marginBottom: '5px' }}>Logo / Banner (Opcional)</label>
                        <input 
                            type="file" 
                            accept="image/*"
                            onChange={e => setFile(e.target.files[0])}
                            style={{ color: 'white' }}
                        />
                    </div>

                    <button 
                        type="submit" 
                        disabled={loading}
                        style={{ 
                            marginTop: '20px', 
                            padding: '15px', 
                            background: '#D4AF37', 
                            border: 'none', 
                            borderRadius: '5px', 
                            fontWeight: 'bold', 
                            cursor: 'pointer',
                            opacity: loading ? 0.7 : 1
                        }}
                    >
                        {loading ? "Criando..." : "Confirmar Criação"}
                    </button>

                    <button 
                        type="button" 
                        onClick={() => navigate('/barberHome')}
                        style={{ background: 'transparent', border: 'none', color: '#aaa', cursor: 'pointer', textDecoration: 'underline' }}
                    >
                        Cancelar
                    </button>
                </form>
            </div>
        </div>
    );
}

export default CreateBarbershopPage;