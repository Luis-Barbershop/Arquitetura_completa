// Home.js
import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
// Recebe a prop 'navigation' automaticamente do Stack.Navigator
// Se o componente fosse mais profundo, usaríamos useNavigation()
export default function Home({ navigation }) {
  // Função para voltar para o ecrã de Login
  const handleBackToLogin = () => {
    navigation.navigate('Login'); // Navega para o ecrã 'Login'
  };

  return (
    <View style={styles.container}>
      <View style={styles.homeContainer}>
        <Text style={styles.title}>✅ Login Aprovado!</Text>
        <Text style={styles.text}>Bem-vindo ao sistema.</Text>
        {/* Substitui <Link> por TouchableOpacity e usa a navegação */}
        <TouchableOpacity style={styles.homeButton} onPress={handleBackToLogin}>
          <Text style={styles.homeButtonText}>Voltar</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

// Estilos adaptados do seu src/App.css
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#333333',
    padding: 20,
  },
  homeContainer: {
    alignItems: 'center', // Centraliza os elementos filhos (Texto e Botão)
    width: '100%',
  },
  title: {
    color: '#c19006',
    fontSize: 28,
    fontWeight: 'bold',
    marginBottom: 10,
    textAlign: 'center',
  },
  text: {
    color: '#fff',
    fontSize: 18,
    textAlign: 'center',
  },
  homeButton: {
    backgroundColor: '#c19006',
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 8,
    marginTop: 20,
    alignItems: 'center',
  },
  homeButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
});