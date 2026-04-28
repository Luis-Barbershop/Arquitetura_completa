import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import CustomerHeader from '../components/HomePage/CustomerHeader';
import CustomerNavbar from '../components/HomePage/CustomerNavbar';
import { logoutUser } from '../services/authService';
import { isBarber } from '../services/userContext';
import { maskPhone, onlyDigits } from '../utils/inputMasks';
import {
  getMyProfile,
  updateCustomerProfile,
  uploadCustomerProfilePhoto,
} from '../services/userProfileService';
import styles from './CSS/CustomerProfilePage.module.css';

function CustomerProfilePage() {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ name: '', tell: '' });

  useEffect(() => {
    if (isBarber()) {
      navigate('/barberHome', { replace: true });
      return;
    }

    const loadProfile = async () => {
      try {
        const data = await getMyProfile();
        setProfile(data);
        setForm({
          name: data?.name || '',
          tell: maskPhone(onlyDigits(data?.tell || data?.phone || '')),
        });

        if (data?.name) {
          localStorage.setItem('userName', data.name);
        }

        if (data?.imageUrl) {
          localStorage.setItem('userProfileImage', data.imageUrl);
        }
      } catch {
        toast.error('Não foi possível carregar seu perfil.');
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, [navigate]);

  const handleLogout = () => {
    logoutUser();
    navigate('/');
  };

  const handleChange = (event) => {
    const { name, value } = event.target;

    if (name === 'tell') {
      setForm((prev) => ({ ...prev, tell: maskPhone(onlyDigits(value).slice(0, 11)) }));
      return;
    }

    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSaveProfile = async (event) => {
    event.preventDefault();
    setSaving(true);

    try {
      await updateCustomerProfile({
        name: form.name.trim(),
        tell: onlyDigits(form.tell),
      });

      localStorage.setItem('userName', form.name.trim());
      setProfile((prev) => ({
        ...prev,
        name: form.name.trim(),
        tell: onlyDigits(form.tell),
      }));
      toast.success('Perfil atualizado com sucesso!');
    } catch (error) {
      toast.error(error?.response?.data?.message || 'Erro ao salvar perfil.');
    } finally {
      setSaving(false);
    }
  };

  const handlePhotoSelected = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    setUploadingPhoto(true);
    try {
      const imageUrl = await uploadCustomerProfilePhoto(file);
      const normalizedUrl = typeof imageUrl === 'string' ? imageUrl : imageUrl?.imageUrl;

      if (normalizedUrl) {
        localStorage.setItem('userProfileImage', normalizedUrl);
        setProfile((prev) => ({ ...prev, imageUrl: normalizedUrl }));
      }

      toast.success('Foto de perfil atualizada!');
    } catch (error) {
      toast.error(error?.response?.data?.message || 'Erro ao enviar foto.');
    } finally {
      setUploadingPhoto(false);
    }
  };

  if (loading) {
    return <div className={styles.loadingState}>Carregando perfil...</div>;
  }

  return (
    <div className={styles.pageContainer}>
      <CustomerHeader activeTab="perfil" onLogout={handleLogout} />
      <CustomerNavbar activeTab="perfil" onLogout={handleLogout} />

      <section className={styles.card}>
        <div className={styles.cardHeader}>
          <h1>Meu Perfil</h1>
          <p>Atualize sua foto e seus dados para manter sua conta em dia.</p>
        </div>

        <div className={styles.photoSection}>
          {profile?.imageUrl ? (
            <img src={profile.imageUrl} alt="Foto de perfil" className={styles.profilePhoto} />
          ) : (
            <div className={styles.profileInitials}>
              {(profile?.name || 'CL')
                .split(' ')
                .slice(0, 2)
                .map((name) => name[0])
                .join('')
                .toUpperCase()}
            </div>
          )}

          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            onChange={handlePhotoSelected}
            className={styles.hiddenInput}
          />

          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploadingPhoto}
            className={styles.secondaryButton}
          >
            {uploadingPhoto ? 'Enviando foto...' : 'Alterar foto'}
          </button>
        </div>

        <form className={styles.form} onSubmit={handleSaveProfile}>
          <label className={styles.field}>
            <span>Nome</span>
            <input
              name="name"
              value={form.name}
              onChange={handleChange}
              maxLength={70}
              required
            />
          </label>

          <label className={styles.field}>
            <span>Telefone</span>
            <input
              name="tell"
              value={form.tell}
              onChange={handleChange}
              maxLength={15}
              placeholder="(11) 99999-9999"
            />
          </label>

          <label className={styles.field}>
            <span>E-mail</span>
            <input value={profile?.email || ''} disabled readOnly />
          </label>

          <button type="submit" disabled={saving} className={styles.primaryButton}>
            {saving ? 'Salvando...' : 'Salvar alterações'}
          </button>
        </form>
      </section>
    </div>
  );
}

export default CustomerProfilePage;
