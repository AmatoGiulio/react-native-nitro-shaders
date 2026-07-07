import { StatusBar } from 'expo-status-bar';
import { useState } from 'react';
import {
  Image,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
  SafeAreaView,
} from 'react-native';
import RNCSlider from '@react-native-community/slider';
import {
  MaterialOrb,
  MATERIAL_NAMES,
  MATERIAL_PRESETS,
  type MaterialName,
  type OrbPattern,
  type ResolvedOrbParams,
} from 'react-native-nitro-shaders';

// Runtime-switchable environments (must mirror native assets env/lab-N.png).
const ENVS = [
  require('./assets/envs/lab-0.png'),
  require('./assets/envs/lab-1.png'),
  require('./assets/envs/lab-2.png'), // glass default (dark teal)
  require('./assets/envs/lab-3.png'),
  require('./assets/envs/lab-4.png'),
  require('./assets/envs/lab-5.png'),
  require('./assets/envs/lab-6.png'),
  require('./assets/envs/lab-7.png'), // studio (metal/iridescent default)
  require('./assets/envs/lab-8.png'),
  require('./assets/envs/lab-9.png'), // wooden_studio_08
  require('./assets/envs/lab-10.png'), // hangar_interior
  require('./assets/envs/lab-11.png'), // qwantani dawn pure sky
  require('./assets/envs/lab-12.png'), // sunset sea — water default
  require('./assets/envs/lab-13.png'), // calm sea reflecting the sky
  require('./assets/envs/lab-14.png'), // purple haze sky over sea
];

const PATTERNS: OrbPattern[] = ['folds', 'bands', 'ripples'];

type LabParams = ResolvedOrbParams;

function presetParams(material: MaterialName): LabParams {
  return { ...MATERIAL_PRESETS[material] };
}

type NumericKey =
  | 'speed'
  | 'morph'
  | 'orbit'
  | 'patternScale'
  | 'patternDistortion'
  | 'tint'
  | 'opacity'
  | 'lightAzimuth'
  | 'lightElevation'
  | 'envRotation'
  | 'density'
  | 'smoothness';

const SLIDERS: { key: NumericKey; label: string; min: number; max: number }[] = [
  { key: 'speed', label: 'Speed', min: 0, max: 2.5 },
  { key: 'morph', label: 'Morph', min: 0, max: 1.4 },
  { key: 'orbit', label: 'Orbit', min: 0, max: 2 },
  { key: 'patternScale', label: 'Scale', min: 0, max: 2 },
  { key: 'patternDistortion', label: 'Distortion', min: 0, max: 1.4 },
  { key: 'tint', label: 'Tint', min: 0, max: 1 },
  { key: 'opacity', label: 'Opacity', min: 0, max: 1 },
  { key: 'lightAzimuth', label: 'Light Az', min: 0, max: 6.283 },
  { key: 'lightElevation', label: 'Light El', min: 0, max: 3.1415 },
  { key: 'envRotation', label: 'Env Rot', min: 0, max: 6.283 },
  { key: 'density', label: 'Density', min: 0, max: 1.5 },
  { key: 'smoothness', label: 'Smooth', min: 0, max: 1 },
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
  return (
    <View style={styles.sliderRow}>
      <Text style={styles.sliderLabel}>{label}</Text>
      <RNCSlider
        style={styles.nativeSlider}
        minimumValue={min}
        maximumValue={max}
        value={value}
        onValueChange={onChange}
        minimumTrackTintColor="#2f80ff"
        maximumTrackTintColor="#d5d9e2"
        thumbTintColor="#ffffff"
      />
      <Text style={styles.sliderValue}>{value.toFixed(2)}</Text>
    </View>
  );
}

export default function App() {
  const [material, setMaterial] = useState<MaterialName>('metal');
  const [params, setParams] = useState<LabParams>(presetParams('metal'));

  const switchMaterial = (m: MaterialName) => {
    setMaterial(m);
    setParams(presetParams(m));
  };

  return (
    <SafeAreaView style={styles.safearea}>
      <View style={styles.container}>
        <View style={styles.tabs}>
          {MATERIAL_NAMES.map((m) => (
            <Pressable key={m} onPress={() => switchMaterial(m)}>
              <View style={[styles.tab, material === m && styles.tabSelected]}>
                <Text style={[styles.tabText, material === m && styles.tabTextSelected]}>
                  {m}
                </Text>
              </View>
            </Pressable>
          ))}
        </View>
        <View style={styles.orbArea}>
          <MaterialOrb material={material} params={params} style={styles.surface} />
        </View>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.envRow}>
          <Pressable onPress={() => setParams((prev) => ({ ...prev, environment: -1 }))}>
            <View
              style={[
                styles.envThumb,
                styles.envAuto,
                params.environment < 0 && styles.envSelected,
              ]}
            >
              <Text style={styles.envAutoText}>auto</Text>
            </View>
          </Pressable>
          {ENVS.map((src, i) => (
            <Pressable key={i} onPress={() => setParams((prev) => ({ ...prev, environment: i }))}>
              <Image
                source={src}
                style={[styles.envThumb, params.environment === i && styles.envSelected]}
              />
            </Pressable>
          ))}
        </ScrollView>
        <ScrollView style={styles.panel} contentContainerStyle={styles.panelContent}>
          <View style={styles.patternRow}>
            <Text style={styles.sliderLabel}>Pattern</Text>
            {PATTERNS.map((pt) => (
              <Pressable key={pt} onPress={() => setParams((prev) => ({ ...prev, pattern: pt }))}>
                <View style={[styles.patternChip, params.pattern === pt && styles.patternChipOn]}>
                  <Text
                    style={[
                      styles.patternChipText,
                      params.pattern === pt && styles.patternChipTextOn,
                    ]}
                  >
                    {pt}
                  </Text>
                </View>
              </Pressable>
            ))}
          </View>
          {SLIDERS.map((s) => (
            <Slider
              key={s.key}
              label={s.label}
              min={s.min}
              max={s.max}
              value={params[s.key]}
              onChange={(v) => setParams((prev) => ({ ...prev, [s.key]: v }))}
            />
          ))}
          <View style={styles.panelFooter}>
            <Pressable
              style={styles.hdrRow}
              onPress={() => setParams((prev) => ({ ...prev, hdr: !prev.hdr }))}
            >
              <View style={[styles.checkbox, params.hdr && styles.checkboxOn]}>
                {params.hdr && <Text style={styles.checkmark}>✓</Text>}
              </View>
              <Text style={styles.hdrLabel}>HDR boost</Text>
            </Pressable>
            <Pressable onPress={() => setParams(presetParams(material))}>
              <Text style={styles.reset}>Reset to defaults</Text>
            </Pressable>
          </View>
        </ScrollView>
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
  tabs: {
    flexDirection: 'row',
    backgroundColor: '#e8eaf0',
    borderRadius: 12,
    padding: 3,
    gap: 2,
  },
  tab: {
    paddingHorizontal: 14,
    paddingVertical: 7,
    borderRadius: 9,
  },
  tabSelected: {
    backgroundColor: '#ffffff',
  },
  tabText: {
    color: '#5c6270',
    fontSize: 12,
    fontWeight: '600',
  },
  tabTextSelected: {
    color: '#15181f',
  },
  orbArea: {
    alignItems: 'center',
    gap: 8,
  },
  surface: {
    width: 280,
    height: 280,
  },
  envRow: {
    maxHeight: 56,
    flexGrow: 0,
    paddingHorizontal: 16,
  },
  envThumb: {
    width: 88,
    height: 44,
    borderRadius: 8,
    marginRight: 8,
    borderWidth: 2,
    borderColor: 'transparent',
  },
  envAuto: {
    backgroundColor: '#e2e5ec',
    alignItems: 'center',
    justifyContent: 'center',
  },
  envAutoText: {
    color: '#5c6270',
    fontSize: 12,
    fontWeight: '600',
  },
  envSelected: {
    borderColor: '#2f80ff',
  },
  panel: {
    width: '90%',
    maxHeight: 340,
    backgroundColor: '#eef0f5',
    borderRadius: 16,
  },
  panelContent: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 8,
  },
  patternRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  patternChip: {
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 8,
    backgroundColor: '#e2e5ec',
  },
  patternChipOn: {
    backgroundColor: '#2f80ff',
  },
  patternChipText: {
    color: '#5c6270',
    fontSize: 12,
    fontWeight: '600',
  },
  patternChipTextOn: {
    color: '#ffffff',
  },
  panelFooter: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 2,
  },
  hdrRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  checkbox: {
    width: 20,
    height: 20,
    borderRadius: 5,
    borderWidth: 2,
    borderColor: '#b6bcc9',
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkboxOn: {
    backgroundColor: '#2f80ff',
    borderColor: '#2f80ff',
  },
  checkmark: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '700',
  },
  hdrLabel: {
    color: '#15181f',
    fontSize: 13,
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
  nativeSlider: {
    flex: 1,
    height: 32,
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
  },
});
