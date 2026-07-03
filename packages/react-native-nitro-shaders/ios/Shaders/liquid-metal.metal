uniform float2 u_resolution;
uniform float u_time;
uniform float u_speed;
uniform float u_orbMaterial; // 0: Chrome, 1: Glass, 2: Iridescent
uniform float u_wobble;
uniform float u_distortion;
uniform float u_detail;
uniform float u_materialColor;

float hash21(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + float2(1.0, 0.0)), u.x),
               mix(hash21(i + float2(0.0, 1.0)), hash21(i + float2(1.0, 1.0)), u.x), u.y);
}

float fbm(float2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 4; i++) { 
        v += a * noise(p); 
        p = p * 2.0 + float2(11.5, 17.2); 
        a *= 0.5; 
    }
    return v;
}

// Studio di luce MatCap sferico (coordinate 0..1)
float3 getMatCapEnv(float2 uvEnv) {
    // Gradiente sferico morbido di base
    float3 baseBg = mix(float3(0.1, 0.12, 0.15), float3(0.7, 0.75, 0.85), uvEnv.y);
    
    // Softbox principale in alto a sinistra (molto intenso)
    float mainLight = smoothstep(0.45, 0.0, distance(uvEnv, float2(0.35, 0.75)));
    // Luce di riempimento morbida in basso a destra
    float fillLight = smoothstep(0.5, 0.0, distance(uvEnv, float2(0.7, 0.25)));
    // Controluce (Rim) per staccare i bordi
    float rimLight = smoothstep(0.2, 0.5, distance(uvEnv, float2(0.5, 0.5)));

    float3 col = baseBg;
    col += float3(1.4, 1.35, 1.3) * mainLight;
    col += float3(0.3, 0.4, 0.5) * fillLight;
    col += float3(0.4, 0.45, 0.5) * rimLight * 0.4;
    
    return col;
}

half4 main(float2 fragCoord) {
    float2 res = max(u_resolution, float2(1.0));
    float size = min(res.x, res.y);
    
    // Proporzioni centrate
    float2 uv = (fragCoord - 0.5 * res) / (0.5 * size);
    uv.y = -uv.y; // Flip Y per orientazione corretta delle luci[cite: 1]

    float t = u_time * u_speed;
    
    // --- 1. DEFORMAZIONE DELLA SILHOUETTE (Bordi Fluidi come in metal.png) ---
    float angle = atan(uv.y, uv.x);
    float edgeNoise = fbm(float2(cos(angle), sin(angle)) * 1.2 + t * 0.4);
    float maxRadius = 0.88 + edgeNoise * (0.15 * u_wobble * u_distortion);
    
    float dist = length(uv);
    float alpha = smoothstep(maxRadius, maxRadius - 0.015, dist);
    if (alpha <= 0.0) return half4(0.0);

    // Normalizziamo le coordinate interne rispetto alla silhouette fluida
    float2 normUV = uv / maxRadius;
    
    // --- 2. GENERAZIONE DEL RUMORE DI FLUIDITÀ INTERNA ---
    float2 warpUV = normUV * (mix(1.0, 2.5, u_detail)) + float2(t * 0.1, t * 0.08);
    float n1 = fbm(warpUV);
    float n2 = fbm(warpUV + vec2(n1 * 1.5)); // Domain warping per curve liquide reali
    
    // --- 3. CALCOLO GEOMETRIA 3D (Normali della Sfera) ---
    // Applichiamo la distorsione liquida direttamente prima del calcolo della Z
    float2 distortedUV = normUV + float2(n2 - 0.5, fbm(warpUV + 2.0) - 0.5) * (0.22 * u_distortion);
    distortedUV = clamp(distortedUV, -0.99, 0.99); // Evita radici quadrate negative
    
    float z = sqrt(1.0 - dot(distortedUV, distortedUV));
    float3 N = normalize(float3(distortedUV.x, distortedUV.y, z));
    
    // Vettori ottici
    float3 V = float3(0.0, 0.0, 1.0);
    float3 R = reflect(-V, N);
    float NdotV = max(dot(N, V), 0.0);
    float fresnel = pow(1.0 - NdotV, 3.5);

    // --- 4. MAPPATURA CORRETTA MATCAP (0.0 a 1.0) ---
    float2 matcapUV = R.xy * 0.5 + 0.5;
    
    // Campioniamo l'ambiente fluido distorto
    float3 envColor = getMatCapEnv(matcapUV);
    
    float3 color = float3(0.0);
    float mode = u_orbMaterial;

    if (mode < 0.5) {
        // === 1. LIQUID CHROME (metal.png) ===
        // Contrasti esasperati tramite curve di potenza, riflessi metallici argentati puri
        float3 chrome = pow(envColor, float3(2.2)) * 2.0;
        
        // Specular highlight acuto
        float spec = pow(max(R.z, 0.0), 50.0);
        
        color = chrome + float3(1.0) * spec * 1.5;
        // Scurisci leggermente le parti in ombra profonda per dare contrasto liquido
        color *= smoothstep(-0.2, 0.6, N.z);
        
    } else if (mode < 1.5) {
        // === 2. LIQUID GLASS / WATER (water.png) ===
        // Sfumature di blu profondo e azzurro liquido (Assorbimento e sss interno)
        float3 deepColor = float3(0.02, 0.18, 0.4);
        float3 shallowColor = float3(0.35, 0.72, 0.98);
        
        // L'interno reagisce al rumore di flusso n2 creando le onde interne di water.png
        float internalFlow = smoothstep(0.1, 0.9, n2);
        float3 glassBody = mix(deepColor, shallowColor, internalFlow * 0.7 + fresnel * 0.3);
        
        // Riflesso ambientale sopra lo strato vitreo
        float3 glassReflect = pow(envColor, float3(1.2)) * (fresnel * 0.8 + 0.1);
        float spec = pow(max(R.z, 0.0), 80.0);
        
        color = glassBody + glassReflect + float3(0.9, 0.95, 1.0) * spec * 1.2;
        
    } else {
        // === 3. IRIDESCENT (iridescent.png) ===
        // Struttura perlata traslucida con spettro arcobaleno guidato sia dal fresnel che dal flusso liquido
        float3 pearlBase = float3(0.93, 0.92, 0.95) * (NdotV * 0.3 + 0.7);
        
        // Arcobaleno dinamico basato sull'angolo di incidenza e deformazione iridescente
        float iridPhase = fresnel * 1.3 + n2 * 0.4 - (t * 0.05);
        float3 rainbow = 0.5 + 0.5 * cos(6.28318 * (iridPhase + float3(0.0, 0.33, 0.67)));
        
        // Combina i riflessi: l'iridescenza predomina sul bordo (fresnel alto)
        color = mix(pearlBase, rainbow, smoothstep(0.2, 0.9, fresnel));
        
        // Riflesso ambientale leggero e speculare finissimo (vetroso)
        float3 glassReflect = envColor * 0.2;
        float spec = pow(max(R.z, 0.0), 90.0);
        
        color += glassReflect + float3(1.0) * spec * 0.9;
    }

    color = clamp(color, 0.0, 1.0);
    return half4(half3(color), half(alpha));
}