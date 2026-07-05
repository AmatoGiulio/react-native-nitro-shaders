import { StatusBar } from 'expo-status-bar';
import { useState } from 'react';
import {
  GestureResponderEvent,
  Pressable,
  StyleSheet,
  Text,
  View,
  SafeAreaView,
} from 'react-native';
import { MaterialOrb } from 'react-native-nitro-shaders';

const DEFAULTS = {
  speed: 0.8,
  wobble: 0.72,
  distortion: 0.56,
  detail: 1,
  materialColor: 0.42,
};

type ParamKey = keyof typeof DEFAULTS;

const PARAMS: { key: ParamKey; label: string; min: number; max: number }[] = [
  { key: 'speed', label: 'Speed', min: 0, max: 2 },
  { key: 'wobble', label: 'Wobble', min: 0, max: 1.4 },
  { key: 'distortion', label: 'Distortion', min: 0, max: 1.4 },
  { key: 'detail', label: 'Detail', min: 0, max: 2 },
  { key: 'materialColor', label: 'Color', min: 0, max: 1 },
];

function Slider({
  label,
  value,
  min,
  max,
  onChange,
}: {
  label: string;
  value: number;
  min: number;
  max: number;
  onChange: (v: number) => void;
}) {
  const [trackWidth, setTrackWidth] = useState(0);

  const handleTouch = (e: GestureResponderEvent) => {
    if (trackWidth <= 0) return;
    const pct = Math.min(1, Math.max(0, e.nativeEvent.locationX / trackWidth));
    onChange(min + pct * (max - min));
  };

  const pct = (value - min) / (max - min);

  return (
    <View style={styles.sliderRow}>
      <Text style={styles.sliderLabel}>{label}</Text>
      <View
        style={styles.track}
        onLayout={(e) => setTrackWidth(e.nativeEvent.layout.width)}
        onStartShouldSetResponder={() => true}
        onMoveShouldSetResponder={() => true}
        onResponderGrant={handleTouch}
        onResponderMove={handleTouch}
      >
        <View style={styles.trackBase} />
        <View style={[styles.trackFill, { width: `${pct * 100}%` }]} />
        <View style={[styles.thumb, { left: `${pct * 100}%` }]} />
      </View>
      <Text style={styles.sliderValue}>{value.toFixed(2)}</Text>
    </View>
  );
}

export default function App() {
  const [params, setParams] = useState(DEFAULTS);

  return (
    <SafeAreaView style={styles.safearea}>
      <View style={styles.container}>
        <View style={styles.orbArea}>
          <MaterialOrb
            material="metal"
            speed={params.speed}
            wobble={params.wobble}
            distortion={params.distortion}
            detail={params.detail}
            materialColor={params.materialColor}
            style={styles.surface}
          />
          <Text style={styles.label}>metal</Text>
        </View>
        <View style={styles.panel}>
          {PARAMS.map((p) => (
            <Slider
              key={p.key}
              label={p.label}
              min={p.min}
              max={p.max}
              value={params[p.key]}
              onChange={(v) => setParams((prev) => ({ ...prev, [p.key]: v }))}
            />
          ))}
          <Pressable onPress={() => setParams(DEFAULTS)}>
            <Text style={styles.reset}>Reset to defaults</Text>
          </Pressable>
        </View>
        <StatusBar style="auto" />
      </View>
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
    backgroundColor: '#f7f8fb',
    alignItems: 'center',
    justifyContent: 'space-evenly',
    paddingVertical: 24,
  },
  orbArea: {
    alignItems: 'center',
    gap: 8,
  },
  surface: {
    width: 300,
    height: 300,
  },
  label: {
    color: '#15181f',
    fontSize: 13,
    fontWeight: '600',
  },
  panel: {
    width: '90%',
    backgroundColor: '#eef0f5',
    borderRadius: 16,
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 10,
  },
  sliderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  sliderLabel: {
    width: 76,
    color: '#15181f',
    fontSize: 13,
  },
  track: {
    flex: 1,
    height: 28,
    justifyContent: 'center',
  },
  trackBase: {
    position: 'absolute',
    left: 0,
    right: 0,
    height: 4,
    borderRadius: 2,
    backgroundColor: '#d5d9e2',
  },
  trackFill: {
    position: 'absolute',
    left: 0,
    height: 4,
    borderRadius: 2,
    backgroundColor: '#2f80ff',
  },
  thumb: {
    position: 'absolute',
    marginLeft: -12,
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: '#ffffff',
    shadowColor: '#000',
    shadowOpacity: 0.15,
    shadowRadius: 3,
    shadowOffset: { width: 0, height: 1 },
    elevation: 3,
  },
  sliderValue: {
    width: 42,
    textAlign: 'right',
    color: '#5c6270',
    fontSize: 12,
    fontVariant: ['tabular-nums'],
  },
  reset: {
    color: '#2f80ff',
    fontSize: 13,
    fontWeight: '600',
    textAlign: 'center',
    paddingTop: 4,
  },
});
