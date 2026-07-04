import { StatusBar } from 'expo-status-bar';
import { StyleSheet, ScrollView, Text, View, SafeAreaView } from 'react-native';
import { MaterialOrb } from 'react-native-nitro-shaders';

export default function App() {
  return (
    <SafeAreaView style={styles.safearea}>
      <ScrollView style={{ flex: 1 }}>
        <View style={styles.container}>
          <View style={styles.item}>
            <MaterialOrb material="metal" style={styles.surface} />
            <Text style={styles.label}>metal</Text>
          </View>
          <View style={styles.item}>
            <MaterialOrb material="water" style={styles.surface} />
            <Text style={styles.label}>water</Text>
          </View>
          <View style={styles.item}>
            <MaterialOrb material="iridescent" style={styles.surface} />
            <Text style={styles.label}>iridescent</Text>
          </View>
          <View style={styles.item}>
            <MaterialOrb material="aura" style={styles.surface} />
            <Text style={styles.label}>aura</Text>
          </View>
          <StatusBar style="auto" />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safearea: {
    flex: 1,
    backgroundColor: '#f7f8fb',
    
  },
  container: {
    flex: 1,
    paddingVertical: 64,
    backgroundColor: '#f7f8fb',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 24,
  },
  item: {
    alignItems: 'center',
    gap: 8,
  },
  surface: {
    width: 220,
    height: 220,
  },
  label: {
    color: '#15181f',
    fontSize: 13,
    fontWeight: '600',
  },
});
