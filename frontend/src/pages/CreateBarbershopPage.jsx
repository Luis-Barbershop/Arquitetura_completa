import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { createBarbershop } from '../services/barbershopService';
import { refreshSession } from '../services/authService';
import Styles from './CSS/HomePage.module.css'; 

function CreateBarbershopPage() {
    const navigate = useNavigate();
    
    const [name, setName] = useState("");
    const [cnpj, setCnpj] = useState("");
    const [file, setFile] = useState(null);
    const [loading, setLoading] = useState(false);

    // ── Campos de endereço via ViaCEP ────────────────────────────────────────
    const [cep, setCep] = useState("");
    const [logradouro, setLogradouro] = useState("");
    const [bairro, setBairro] = useState("");
    const [cidade, setCidade] = useState("");
    const [uf, setUf] = useState("");
    const [numero, setNumero] = useState("");
    const [complemento, setComplemento] = useState("");
    const [cepLoading, setCepLoading] = useState(false);
    const [cepError, setCepError] = useState("");

    // ── Busca automática ao sair do campo CEP ────────────────────────────────
    const handleCepBlur = async () => {
        const cleaned = cep.replace(/\D/g, "");
        if (cleaned.length !== 8) {
            setCepError("CEP inválido. Informe 8 dígitos.");
            return;
        }
        setCepError("");
        setCepLoading(true);
        try {
            const res = await fetch(`https://viacep.com.br/ws/${cleaned}/json/`);
            const data = await res.json();
            if (data.erro) {
                setCepError("CEP não encontrado.");
                setLogradouro(""); setBairro(""); setCidade(""); setUf("");
            } else {
                setLogradouro(data.logradouro || "");
                setBairro(data.bairro || "");
                setCidade(data.localidade || "");
                setUf(data.uf || "");
            }
        } catch {
            setCepError("Erro ao consultar o CEP. Verifique sua conexão.");
        } finally {
            setCepLoading(false);
        }
    };

    // ── Máscara de CEP ───────────────────────────────────────────────────────
    const handleCepChange = (e) => {
        const v = e.target.value.replace(/\D/g, "").slice(0, 8);
        const masked = v.length > 5 ? `${v.slice(0, 5)}-${v.slice(5)}` : v;
        setCep(masked);
        setCepError("");
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (cep.replace(/\D/g, "").length !== 8 || !logradouro) {
            toast.warn("Consulte um CEP válido antes de continuar.");
            return;
        }
        if (!numero.trim()) {
            toast.warn("Informe o número do endereço.");
            return;
        }

        // Monta string de endereço completo para o backend
        const addressParts = [
            `${logradouro}, ${numero}`,
            complemento ? complemento : null,
            bairro,
            `${cidade} - ${uf}`,
            `CEP: ${cep}`,
        ].filter(Boolean);
        const address = addressParts.join(", ");

        setLoading(true);
        try {
            await createBarbershop({ name, cnpj, address }, file);

            // Atualiza role/isOwner no localStorage sem precisar de logout
            await refreshSession();

            toast.success("Barbearia criada com sucesso!");
            navigate('/barberHome', { replace: true, state: { activeTab: 'home' } });
        } catch (error) {
            console.error(error);
            toast.error("Erro ao criar barbearia. Verifique os dados.");
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
                            onChange={e => {
                                const digits = e.target.value.replace(/\D/g, '').slice(0, 14);
                                // Mascara: 00.000.000/0001-00
                                let masked = digits;
                                if (digits.length > 2) masked = digits.slice(0, 2) + '.' + digits.slice(2);
                                if (digits.length > 5) masked = masked.slice(0, 6) + '.' + digits.slice(5);
                                if (digits.length > 8) masked = masked.slice(0, 10) + '/' + digits.slice(8);
                                if (digits.length > 12) masked = masked.slice(0, 15) + '-' + digits.slice(12);
                                setCnpj(masked);
                            }}
                            required 
                            placeholder="00.000.000/0001-00"
                            maxLength={18}
                            inputMode="numeric"
                            style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #444', background: '#0f0f0f', color: 'white' }}
                        />
                    </div>

                    {/* ── Endereço via ViaCEP ── */}
                    <div style={{ borderTop: '1px solid #444', paddingTop: '15px' }}>
                        <p style={{ color: '#D4AF37', fontWeight: 600, marginBottom: '12px', fontSize: '14px' }}>
                            📍 Endereço
                        </p>

                        {/* CEP */}
                        <div style={{ marginBottom: '12px' }}>
                            <label style={{ display: 'block', marginBottom: '5px' }}>CEP *</label>
                            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                                <input
                                    type="text"
                                    value={cep}
                                    onChange={handleCepChange}
                                    onBlur={handleCepBlur}
                                    required
                                    placeholder="00000-000"
                                    maxLength={9}
                                    style={{ flex: 1, padding: '10px', borderRadius: '5px', border: `1px solid ${cepError ? '#e74c3c' : '#444'}`, background: '#0f0f0f', color: 'white' }}
                                />
                                {cepLoading && (
                                    <span style={{ color: '#D4AF37', fontSize: '13px', whiteSpace: 'nowrap' }}>Buscando...</span>
                                )}
                            </div>
                            {cepError && <p style={{ color: '#e74c3c', fontSize: '12px', marginTop: '4px' }}>{cepError}</p>}
                        </div>

                        {/* Logradouro */}
                        <div style={{ marginBottom: '12px' }}>
                            <label style={{ display: 'block', marginBottom: '5px' }}>Rua / Logradouro</label>
                            <input
                                type="text"
                                value={logradouro}
                                onChange={e => setLogradouro(e.target.value)}
                                readOnly={!!logradouro}
                                required
                                placeholder="Preenchido automaticamente"
                                style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #444', background: logradouro ? '#1a1a1a' : '#0f0f0f', color: logradouro ? '#aaa' : 'white', boxSizing: 'border-box' }}
                            />
                        </div>

                        {/* Número + Complemento lado a lado */}
                        <div style={{ display: 'flex', gap: '10px', marginBottom: '12px' }}>
                            <div style={{ flex: '0 0 120px' }}>
                                <label style={{ display: 'block', marginBottom: '5px' }}>Número *</label>
                                <input
                                    type="text"
                                    value={numero}
                                    onChange={e => setNumero(e.target.value)}
                                    required
                                    placeholder="Ex: 123"
                                    maxLength={10}
                                    style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #444', background: '#0f0f0f', color: 'white', boxSizing: 'border-box' }}
                                />
                            </div>
                            <div style={{ flex: 1 }}>
                                <label style={{ display: 'block', marginBottom: '5px' }}>Complemento</label>
                                <input
                                    type="text"
                                    value={complemento}
                                    onChange={e => setComplemento(e.target.value)}
                                    placeholder="Sala, Loja, Bloco..."
                                    maxLength={60}
                                    style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #444', background: '#0f0f0f', color: 'white', boxSizing: 'border-box' }}
                                />
                            </div>
                        </div>

                        {/* Bairro */}
                        <div style={{ marginBottom: '12px' }}>
                            <label style={{ display: 'block', marginBottom: '5px' }}>Bairro</label>
                            <input
                                type="text"
                                value={bairro}
                                onChange={e => setBairro(e.target.value)}
                                readOnly={!!bairro}
                                required
                                placeholder="Preenchido automaticamente"
                                style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #444', background: bairro ? '#1a1a1a' : '#0f0f0f', color: bairro ? '#aaa' : 'white', boxSizing: 'border-box' }}
                            />
                        </div>

                        {/* Cidade + UF lado a lado */}
                        <div style={{ display: 'flex', gap: '10px' }}>
                            <div style={{ flex: 1 }}>
                                <label style={{ display: 'block', marginBottom: '5px' }}>Cidade</label>
                                <input
                                    type="text"
                                    value={cidade}
                                    readOnly
                                    required
                                    placeholder="Preenchido automaticamente"
                                    style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #444', background: '#1a1a1a', color: '#aaa', boxSizing: 'border-box' }}
                                />
                            </div>
                            <div style={{ flex: '0 0 70px' }}>
                                <label style={{ display: 'block', marginBottom: '5px' }}>UF</label>
                                <input
                                    type="text"
                                    value={uf}
                                    readOnly
                                    placeholder="--"
                                    style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #444', background: '#1a1a1a', color: '#aaa', boxSizing: 'border-box', textAlign: 'center' }}
                                />
                            </div>
                        </div>
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