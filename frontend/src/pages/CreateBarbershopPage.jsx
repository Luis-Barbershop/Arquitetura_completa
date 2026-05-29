import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { createBarbershop } from '../services/barbershopService';
import { refreshSession } from '../services/authService';
import CropImageModal from '../components/CropImageModal/CropImageModal';
import styles from './CSS/CreateBarbershopPage.module.css';

function CreateBarbershopPage() {
    const navigate = useNavigate();
    
    const [name, setName] = useState("");
    const [cnpj, setCnpj] = useState("");
    const [file, setFile] = useState(null);
    const [previewUrl, setPreviewUrl] = useState(null);
    const [cropSrc, setCropSrc] = useState(null);
    const logoInputRef = useRef(null);
    const [submitting, setSubmitting] = useState(false);

    const [cep, setCep] = useState("");
    const [logradouro, setLogradouro] = useState("");
    const [bairro, setBairro] = useState("");
    const [cidade, setCidade] = useState("");
    const [uf, setUf] = useState("");
    const [numero, setNumero] = useState("");
    const [complemento, setComplemento] = useState("");
    const [cepLoading, setCepLoading] = useState(false);
    const [cepError, setCepError] = useState("");

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

    const handleCepChange = (e) => {
        const v = e.target.value.replace(/\D/g, "").slice(0, 8);
        const masked = v.length > 5 ? `${v.slice(0, 5)}-${v.slice(5)}` : v;
        setCep(masked);
        setCepError("");
    };

    const handleLogoChange = (e) => {
        const selected = e.target.files?.[0];
        e.target.value = '';
        if (!selected) return;
        const objectUrl = URL.createObjectURL(selected);
        setCropSrc(objectUrl);
    };

    const handleCropConfirm = (blob) => {
        const croppedFile = new File([blob], 'logo-barbearia.jpg', { type: blob.type || 'image/jpeg' });
        setFile(croppedFile);
        setPreviewUrl(URL.createObjectURL(croppedFile));
        setCropSrc(null);
    };

    const handleCropCancel = () => {
        setCropSrc(null);
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

        const addressParts = [
            `${logradouro}, ${numero}`,
            complemento ? complemento : null,
            bairro,
            `${cidade} - ${uf}`,
            `CEP: ${cep}`,
        ].filter(Boolean);
        const address = addressParts.join(", ");

        setSubmitting(true);
        try {
            await createBarbershop({ name, cnpj, address }, file);
            await refreshSession();
            toast.success("Barbearia criada com sucesso!");
            navigate('/barberHome', { replace: true, state: { activeTab: 'home' } });
        } catch (error) {
            console.error(error);
            toast.error("Erro ao criar barbearia. Verifique os dados.");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className={styles.stage} data-onboarding-id="barber-create-shop-page">
            <div className={styles.shell}>
                {/* Painel de branding */}
                <aside className={styles.brandPanel}>
                    <div>
                        <div className={styles.brandBadge}>
                            <img src="/Icons/scissors_icon.png" alt="Icone CortaAI" />
                            <span>CortaAI</span>
                        </div>

                        <p className={styles.kicker}>Transforme Seu Negócio</p>
                        <h1 className={styles.title}>Barbearias que crescem com você.</h1>
                        <p className={styles.subtitle}>Crie um perfil profissional, gerencie agendamentos e conecte-se com seus clientes em um só lugar.</p>

                        <div className={styles.tagRow}>
                            <span className={styles.roleTag}>Novo proprietário</span>
                            <span className={styles.softTag}>Cadastro seguro</span>
                        </div>

                        <ul className={styles.featuresList}>
                            <li>Painel de controle completo para sua barbearia</li>
                            <li>Agenda conectada com clientes em tempo real</li>
                            <li>Portfólio visual de seus serviços</li>
                            <li>Gestão de equipe e horários simplificada</li>
                        </ul>
                    </div>
                </aside>

                {/* Painel de formulário */}
                <section className={styles.formPanel}>
                    <h2>Registre sua barbearia</h2>
                    <p>Configure os dados básicos para começar agora mesmo.</p>

                    <form onSubmit={handleSubmit} className={styles.form}>
                        <div className={styles.row}>
                            <label className={styles.label}>Nome da Barbearia</label>
                            <input 
                                className={styles.input} 
                                type="text" 
                                value={name} 
                                onChange={e => setName(e.target.value)} 
                                placeholder="Ex: Barbearia Central"
                                required 
                            />
                        </div>

                        <div className={styles.row}>
                            <label className={styles.label}>CNPJ</label>
                            <input
                                className={styles.input}
                                type="text"
                                value={cnpj}
                                onChange={e => {
                                    const digits = e.target.value.replace(/\D/g, '').slice(0, 14);
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
                            />
                        </div>

                        <section className={styles.addressSection}>
                            <div className={styles.addressHeader}>
                                <h3>Endereço</h3>
                                <small className={styles.hint}>Preencha o CEP para auto completar</small>
                            </div>

                            <div className={styles.row}>
                                <label className={styles.label}>CEP</label>
                                <div className={styles.rowInline}>
                                    <input
                                        className={`${styles.input} ${cepError ? styles.inputError : ''}`}
                                        type="text"
                                        value={cep}
                                        onChange={handleCepChange}
                                        onBlur={handleCepBlur}
                                        required
                                        placeholder="00000-000"
                                        maxLength={9}
                                    />
                                    {cepLoading && <div className={styles.cepStatus}>Buscando...</div>}
                                </div>
                                {cepError && <div className={styles.errorText}>{cepError}</div>}
                            </div>

                            <div className={styles.row}>
                                <label className={styles.label}>Rua / Logradouro</label>
                                <input 
                                    className={styles.input} 
                                    type="text" 
                                    value={logradouro} 
                                    onChange={e => setLogradouro(e.target.value)} 
                                    readOnly={!!logradouro} 
                                    required 
                                    placeholder="Preenchido automaticamente" 
                                />
                            </div>

                            <div className={styles.twoCols}>
                                <div className={styles.row}>
                                    <label className={styles.label}>Número</label>
                                    <input 
                                        className={styles.input} 
                                        type="text" 
                                        value={numero} 
                                        onChange={e => setNumero(e.target.value)} 
                                        required 
                                        placeholder="Ex: 123" 
                                        maxLength={10} 
                                    />
                                </div>
                                <div className={styles.row}>
                                    <label className={styles.label}>Complemento</label>
                                    <input 
                                        className={styles.input} 
                                        type="text" 
                                        value={complemento} 
                                        onChange={e => setComplemento(e.target.value)} 
                                        placeholder="Sala, Loja..." 
                                        maxLength={60} 
                                    />
                                </div>
                            </div>

                            <div className={styles.row}>
                                <label className={styles.label}>Bairro</label>
                                <input 
                                    className={styles.input} 
                                    type="text" 
                                    value={bairro} 
                                    onChange={e => setBairro(e.target.value)} 
                                    readOnly={!!bairro} 
                                    required 
                                    placeholder="Preenchido automaticamente" 
                                />
                            </div>

                            <div className={styles.twoColsSmall}>
                                <div className={styles.row}>
                                    <label className={styles.label}>Cidade</label>
                                    <input 
                                        className={styles.input} 
                                        type="text" 
                                        value={cidade} 
                                        readOnly 
                                        required 
                                        placeholder="Preenchido automaticamente" 
                                    />
                                </div>
                                <div className={styles.row}>
                                    <label className={styles.label}>UF</label>
                                    <input 
                                        className={styles.input} 
                                        id={styles.uf}
                                        type="text" 
                                        value={uf} 
                                        readOnly 
                                        placeholder="--" 
                                    />
                                </div>
                            </div>
                        </section>

                        <div className={styles.rowUpload}>
                            <label className={styles.label}>Logo da Barbearia (Opcional)</label>
                            <input ref={logoInputRef} type="file" accept="image/*" onChange={handleLogoChange} className={styles.fileInput} />
                            <div className={styles.uploadControls}>
                                <button type="button" className={styles.fileButton} onClick={() => logoInputRef.current?.click()}>
                                    {file ? '✔ Trocar imagem' : '📷 Selecionar logo'}
                                </button>
                                {previewUrl && (
                                    <div className={styles.previewWrap}>
                                        <img src={previewUrl} alt="Preview da logo" className={styles.preview} />
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className={styles.actions}>
                            <button type="button" onClick={() => navigate('/barberHome')} className={styles.secondaryBtn}>
                                Cancelar
                            </button>
                            <button type="submit" disabled={submitting} className={styles.primaryBtn}>
                                {submitting ? 'Criando...' : 'Confirmar Criação'}
                            </button>
                        </div>
                    </form>
                </section>
            </div>

            {cropSrc && (
                <CropImageModal
                    src={cropSrc}
                    title="Ajustar logo da barbearia"
                    aspect={1}
                    outputSize={{ width: 600, height: 600 }}
                    onConfirm={handleCropConfirm}
                    onCancel={handleCropCancel}
                />
            )}
        </div>
    );
}

export default CreateBarbershopPage;