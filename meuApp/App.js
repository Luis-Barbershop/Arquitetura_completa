// App.js
import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

import Login from './Login';
import Home from './Home';

// 1. Cria o Navegador Stack
const Stack = createNativeStackNavigator();

export default function App() {
  return (
    // 2. O NavigationContainer envolve toda a estrutura de navegação
    <NavigationContainer>
      {/* 3. O Stack.Navigator gere as transições entre ecrãs */}
      <Stack.Navigator
        initialRouteName="Login"
        screenOptions={{
          headerShown: false, // Oculta a barra de título padrão do cabeçalho
          contentStyle: { backgroundColor: '#333333' } // Aplica o fundo escuro global
        }}
      >
        {/* 4. Define os ecrãs ('Login' e 'Home') */}
        <Stack.Screen name="Login" component={Login} />
        <Stack.Screen name="Home" component={Home} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}