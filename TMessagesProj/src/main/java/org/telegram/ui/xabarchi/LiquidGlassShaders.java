package org.telegram.ui.xabarchi;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public final class LiquidGlassShaders {

    public static final String LIQUID_GLASS_AGSL = ""
            + "uniform shader inputShader;\n"
            + "uniform float2 resolution;\n"
            + "uniform float cornerRadius;\n"
            + "uniform float edgeBandPx;\n"
            + "uniform float refractionPx;\n"
            + "uniform float2 tilt;\n"
            + "uniform float4 tintColor;\n"
            + "uniform float glareAlpha;\n"
            + "\n"
            + "float sdRoundedBox(float2 p, float2 b, float r) {\n"
            + "    float2 q = abs(p) - b + float2(r);\n"
            + "    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;\n"
            + "}\n"
            + "\n"
            + "float2 calcNormal(float2 p, float2 b, float r) {\n"
            + "    const float eps = 1.5;\n"
            + "    float dx = sdRoundedBox(p + float2(eps, 0.0), b, r) - sdRoundedBox(p - float2(eps, 0.0), b, r);\n"
            + "    float dy = sdRoundedBox(p + float2(0.0, eps), b, r) - sdRoundedBox(p - float2(0.0, eps), b, r);\n"
            + "    float2 g = float2(dx, dy);\n"
            + "    float len = max(length(g), 0.0001);\n"
            + "    return g / len;\n"
            + "}\n"
            + "\n"
            + "half4 main(float2 fragCoord) {\n"
            + "    float2 halfSize = max(resolution * 0.5, float2(1.0));\n"
            + "    float r = min(cornerRadius, min(halfSize.x, halfSize.y));\n"
            + "    float2 p = fragCoord - halfSize;\n"
            + "    float dist = sdRoundedBox(p, halfSize, r);\n"
            + "    if (dist > 0.5) {\n"
            + "        return half4(0.0);\n"
            + "    }\n"
            + "\n"
            + "    float distInside = max(-dist, 0.0);\n"
            + "    float band = max(edgeBandPx, 1.0);\n"
            + "    float edgeT = 1.0 - clamp(distInside / band, 0.0, 1.0);\n"
            + "    float lensCurve = edgeT * edgeT * (3.0 - 2.0 * edgeT);\n"
            + "\n"
            + "    float2 normal = calcNormal(p, halfSize, r);\n"
            + "    float2 bendOffset = -normal * (lensCurve * refractionPx);\n"
            + "    float2 magnifyOffset = -p * (0.025 * lensCurve);\n"
            + "    float2 baseCoord = clamp(fragCoord + bendOffset + magnifyOffset, float2(0.0), resolution);\n"
            + "\n"
            + "    float disp = lensCurve * 1.4;\n"
            + "    half4 sampleR = inputShader.eval(clamp(baseCoord + normal * disp, float2(0.0), resolution));\n"
            + "    half4 sampleG = inputShader.eval(baseCoord);\n"
            + "    half4 sampleB = inputShader.eval(clamp(baseCoord - normal * disp, float2(0.0), resolution));\n"
            + "    half3 refracted = half3(sampleR.r, sampleG.g, sampleB.b);\n"
            + "\n"
            + "    half3 tinted = mix(refracted, half3(tintColor.rgb), half(tintColor.a));\n"
            + "\n"
            + "    float2 uv = fragCoord / max(resolution, float2(1.0));\n"
            + "    float2 lightDir = normalize(float2(-0.65 + tilt.x * 0.45, -0.75 + tilt.y * 0.45));\n"
            + "    float rimDot = max(dot(-normal, lightDir), 0.0);\n"
            + "    float rimSpec = pow(edgeT, 2.2) * (0.28 + 0.55 * rimDot);\n"
            + "    float sweepPos = dot(uv - float2(0.5), float2(0.75, 0.65)) - (tilt.x * 0.28 + tilt.y * 0.18);\n"
            + "    float movingGlare = exp(-sweepPos * sweepPos * 22.0) * glareAlpha;\n"
            + "\n"
            + "    half3 finalRgb = clamp(tinted + half3(rimSpec * 0.24 + movingGlare), half3(0.0), half3(1.0));\n"
            + "    return half4(finalRgb, 1.0);\n"
            + "}\n";

    private static ColorMatrixColorFilter cachedSaturationFilter;

    private LiquidGlassShaders() {
    }

    public static ColorMatrixColorFilter getSaturationFilter() {
        if (cachedSaturationFilter == null) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(LiquidGlassConfig.SATURATION_BOOST);
            cachedSaturationFilter = new ColorMatrixColorFilter(matrix);
        }
        return cachedSaturationFilter;
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Nullable
    public static RuntimeShader createRefractionShader() {
        try {
            return new RuntimeShader(LIQUID_GLASS_AGSL);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    public static RenderEffect createBlurAndSaturationEffect(float blurRadiusPx) {
        RenderEffect blur = RenderEffect.createBlurEffect(
                blurRadiusPx,
                blurRadiusPx,
                Shader.TileMode.CLAMP
        );
        RenderEffect saturate = RenderEffect.createColorFilterEffect(getSaturationFilter());
        return RenderEffect.createChainEffect(saturate, blur);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static RenderEffect createFullLiquidGlassEffect(
            RuntimeShader shader,
            float width,
            float height,
            float cornerRadiusPx,
            float edgeBandPx,
            float refractionPx,
            float tiltX,
            float tiltY,
            int tintArgb,
            float glareAlpha,
            RenderEffect baseBlurAndSatEffect
    ) {
        shader.setFloatUniform("resolution", Math.max(width, 1f), Math.max(height, 1f));
        shader.setFloatUniform("cornerRadius", cornerRadiusPx);
        shader.setFloatUniform("edgeBandPx", edgeBandPx);
        shader.setFloatUniform("refractionPx", refractionPx);
        shader.setFloatUniform("tilt", tiltX, tiltY);
        float a = ((tintArgb >>> 24) & 0xFF) / 255f;
        float r = ((tintArgb >>> 16) & 0xFF) / 255f;
        float g = ((tintArgb >>> 8) & 0xFF) / 255f;
        float b = (tintArgb & 0xFF) / 255f;
        shader.setFloatUniform("tintColor", r, g, b, a);
        shader.setFloatUniform("glareAlpha", glareAlpha);

        RenderEffect shaderEffect = RenderEffect.createRuntimeShaderEffect(shader, "inputShader");
        return RenderEffect.createChainEffect(shaderEffect, baseBlurAndSatEffect);
    }
}
