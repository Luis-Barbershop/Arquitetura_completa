// Login.js
import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Alert,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const navigation = useNavigation(); // Hook de navegação do React Navigation

  // Função para simular o login e navegar
  const handleSubmit = () => {
    // 1. Evita o e.preventDefault() necessário no React Web
    // A lógica de validação permanece a mesma
    if (email === 'admin@email.com' && password === '1234') {
      // 2. Usa navigation.navigate('NomeDoEcran') para redirecionar
      navigation.navigate('Home'); 
    } else {
      // 3. Usa o componente nativo Alert em vez do alert() do browser
      Alert.alert('❌ Login Incorreto', 'Tente novamente.'); 
    }
  };

  //Função abaixo para o spring boot

//    const handleSubmit = async () => {
//     setLoading(true); // Começa a carregar
    
//     // --- SUA LÓGICA FETCH (ADAPTADA) ---
//     try {
//       const response = await fetch('http://localhost:8080/api/login', {
//         method: 'POST',
//         headers: { 'Content-Type': 'application/json' },
//         body: JSON.stringify({ email, password })
//       });

//       // 1. Verifica se a resposta foi bem-sucedida (status 200-299)
//       if (response.ok) {
//         // Opcional: Processar o token/dados de resposta
//         // const data = await response.json(); 
        
//         // Redireciona para página de sucesso
//         navigation.navigate('Home'); 

//       } else {
//         // 2. Se a resposta não for ok (e.g., 401 Unauthorized, 404 Not Found)
//         const errorData = await response.json(); // Tenta ler o corpo do erro
//         const errorMessage = errorData.message || 'Credenciais inválidas. Tente novamente.';
        
//         Alert.alert('❌ Login Falhou', errorMessage);
//       }
//     } catch (error) {
//       // 3. Lida com erros de rede (e.g., servidor offline, timeout)
//       console.error("Erro na chamada de API:", error);
//       Alert.alert('Erro de Conexão', 'Não foi possível conectar-se ao servidor.');
//     } finally {
//       setLoading(false); // Termina o carregamento, independentemente do sucesso/falha
//     }
//     // ------------------------------------
//   };

  return (
    <View style={styles.container}>
      <View style={styles.loginForm}>
        <Text style={styles.title}>Login</Text>
        <TextInput
          style={styles.input}
          placeholder="Digite seu e-mail"
          placeholderTextColor="#999" // Cor do placeholder para contraste
          value={email}
          onChangeText={setEmail} // Usa onChangeText em vez de onChange
          keyboardType="email-address"
          autoCapitalize="none"
          required
        />
        <TextInput
          style={styles.input}
          placeholder="Digite sua senha"
          placeholderTextColor="#999"
          value={password}
          onChangeText={setPassword}
          secureTextEntry={true} // Oculta a senha
          required
        />
        {/* 4. Substitui <button> por TouchableOpacity para estilos customizados */}
        <TouchableOpacity style={styles.button} onPress={handleSubmit}>
          <Text style={styles.buttonText}>Entrar</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

// 5. Estilos adaptados do seu src/App.css
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#333333',
    width: '100%',
  },
  loginForm: {
    backgroundColor: '#222',
    padding: 30,
    borderRadius: 16,
    width: 320,
    // Estilização de sombra para iOS (shadow...) e Android (elevation)
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 12,
    elevation: 5,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    color: '#c19006',
    textAlign: 'center',
  },
  input: {
    width: '100%',
    padding: 10,
    marginVertical: 8,
    borderRadius: 8,
    borderColor: '#c19006',
    borderWidth: 1,
    backgroundColor: '#333',
    color: 'white',
    fontSize: 14,
  },
  button: {
    width: '100%',
    backgroundColor: '#c19006',
    padding: 10,
    borderRadius: 8,
    marginTop: 10,
    alignItems: 'center',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
});