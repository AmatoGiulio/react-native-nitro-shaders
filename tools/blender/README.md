# Blender ground-truth lab

Renderizza la sfera chrome di riferimento (Metallic 1, Roughness 0.05) dentro un
environment equirettangolare, per confrontarla col render dello shader Android.

## Uso da riga di comando

macOS:
```
/Applications/Blender.app/Contents/MacOS/Blender --background --python tools/blender/ground_truth.py -- <env.png> <output.png>
```

Windows (PowerShell):
```
& "C:\Program Files\Blender Foundation\Blender 5.1\blender.exe" --background --python tools\blender\ground_truth.py -- <env.png> <output.png>
```

Esempio con un env del laboratorio:
```
... ground_truth.py -- packages/react-native-nitro-shaders/android/src/main/assets/env/lab-1.png gt-lab-1.png
```

## Uso in GUI (per esplorare)

1. Sfera UV + Shade Smooth, materiale Principled: Metallic 1.0, Roughness 0.05.
2. World -> Color -> Environment Texture -> apri un equirect (2:1).
3. Viewport shading: Rendered (quarta sferetta / Z -> Rendered).
4. Quando un ambiente convince: esportalo/salvalo come PNG equirect 1024x512 e
   aggiungilo come `assets/env/lab-N.png` (+ anteprima 192x96 in
   `apps/example/assets/envs/lab-N.png` e voce ENVS in App.tsx).

Gli env runtime del laboratorio si selezionano nella demo (riga di anteprime);
l'indice N corrisponde a `lab-N.png`.
