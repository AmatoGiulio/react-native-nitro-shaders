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
  MATERIAL_ORB_PRESETS,
  type MaterialOrbMaterial,
} from 'react-native-nitro-shaders';

const MATERIALS: MaterialOrbMaterial[] = ['metal', 'water', 'iridescent', 'aura', 'mercury', 'glass'];

// Runtime-switchable environments (must mirror native assets env/lab-N.png).
const ENVS = [
  require('./assets/envs/lab-0.png'),
  require('./assets/envs/lab-1.png'),
  require('./assets/envs/lab-2.png'),
  require('./assets/envs/lab-3.png'),
  require('./assets/envs/lab-4.png'),
  require('./assets/envs/lab-5.png'),
  require('./assets/envs/lab-6.png'),
  require('./assets/envs/lab-7.png'), // studio (metal default)
  require('./assets/envs/lab-8.png'), // mercury default
  require('./assets/envs/lab-9.png'), // glass default (wooden_studio_08)
  require('./assets/envs/lab-10.png'), // hangar_interior (CC0 stand-in for BlendKit metal-hangar)
  require('./assets/envs/lab-11.png'), // qwantani dawn pure sky (water/gel reference env)
  require('./assets/envs/lab-12.png'), // sunset sea (clouds + ocean) — water default
  require('./assets/envs/lab-13.png'), // calm sea reflecting the sky
  require('./assets/envs/lab-14.png'), // purple haze sky over sea
];

function presetParams(material: MaterialOrbMaterial) {
  const p = MATERIAL_ORB_PRESETS[material];
  return {
    speed: p.speed,
    wobble: p.wobble,
    distortion: p.distortion,
    detail: p.detail,
    materialColor: p.materialColor,
    density: p.density ?? 1.0,
    smooth: p.smooth ?? 0.0,
    envRot: p.envRot ?? 0,
  };
}

function presetHdr(material: MaterialOrbMaterial): boolean {
  return MATERIAL_ORB_PRESETS[material].hdr ?? true;
}

type ParamKey = keyof ReturnType<typeof presetParams>;

const PARAMS: { key: ParamKey; label: string; min: number; max: number }[] = [
  { key: 'speed', label: 'Speed', min: 0, max: 2 },
  { key: 'wobble', label: 'Wobble', min: 0, max: 1.4 },
  { key: 'distortion', label: 'Distortion', min: 0, max: 1.4 },
  { key: 'detail', label: 'Detail', min: 0, max: 2 },
  { key: 'materialColor', label: 'Color', min: 0, max: 1 },
  { key: 'density', label: 'Density', min: 0, max: 1.5 },
  { key: 'smooth', label: 'Smooth', min: 0, max: 1 },
  { key: 'envRot', label: 'Env Rot', min: 0, max: 6.283 },
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
  const [material, setMaterial] = useState<MaterialOrbMaterial>('mercury');
  const [params, setParams] = useState(presetParams('mercury'));
  const [envIndex, setEnvIndex] = useState<number | undefined>(undefined);
  const [hdr, setHdr] = useState(presetHdr('mercury'));

  const switchMaterial = (m: MaterialOrbMaterial) => {
    setMaterial(m);
    setParams(presetParams(m));
    setHdr(presetHdr(m));
  };

  return (
    <SafeAreaView style={styles.safearea}>
      <View style={styles.container}>
        <View style={styles.tabs}>
          {MATERIALS.map((m) => (
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
          <MaterialOrb
            material={material}
            speed={params.speed}
            wobble={params.wobble}
            distortion={params.distortion}
            detail={params.detail}
            materialColor={params.materialColor}
            density={params.density}
            smooth={params.smooth}
            envRot={params.envRot}
            environment={envIndex}
            hdr={hdr}
            style={styles.surface}
          />
        </View>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.envRow}>
          <Pressable onPress={() => setEnvIndex(undefined)}>
            <View style={[styles.envThumb, styles.envAuto, envIndex === undefined && styles.envSelected]}>
              <Text style={styles.envAutoText}>auto</Text>
            </View>
          </Pressable>
          {ENVS.map((src, i) => (
            <Pressable key={i} onPress={() => setEnvIndex(i)}>
              <Image source={src} style={[styles.envThumb, envIndex === i && styles.envSelected]} />
            </Pressable>
          ))}
        </ScrollView>
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
          <View style={styles.panelFooter}>
            <Pressable style={styles.hdrRow} onPress={() => setHdr((v) => !v)}>
              <View style={[styles.checkbox, hdr && styles.checkboxOn]}>
                {hdr && <Text style={styles.checkmark}>✓</Text>}
              </View>
              <Text style={styles.hdrLabel}>HDR boost</Text>
            </Pressable>
            <Pressable
              onPress={() => {
                setParams(presetParams(material));
                setHdr(presetHdr(material));
              }}
            >
              <Text style={styles.reset}>Reset to defaults</Text>
            </Pressable>
          </View>
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
  tabs: {
    flexDirection: 'row',
    backgroundColor: '#e8eaf0',
    borderRadius: 12,
    padding: 3,
    gap: 2,
  },
  tab: {
    paddingHorizontal: 12,
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
    width: 300,
    height: 300,
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
    backgroundColor: '#eef0f5',
    borderRadius: 16,
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 10,
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
    height: 36,
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
