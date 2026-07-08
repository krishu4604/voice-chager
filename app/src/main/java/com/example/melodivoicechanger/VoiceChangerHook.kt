package com.example.melodivoicechanger

import android.media.AudioRecord
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.nio.ByteBuffer

class VoiceChangerHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Only target the Melodi app
        if (lpparam.packageName != "com.melodi.android.melodi") return

        XposedBridge.log("MelodiVoiceChanger: Hooking Melodi app!")

        // Hook AudioRecord read(byte[], int, int)
        XposedHelpers.findAndHookMethod(
            AudioRecord::class.java,
            "read",
            ByteArray::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val bytesRead = param.result as Int
                    if (bytesRead > 0) {
                        val buffer = param.args[0] as ByteArray
                        applyBasicPitchShift(buffer, bytesRead)
                    }
                }
            }
        )

        // Hook AudioRecord read(short[], int, int)
        XposedHelpers.findAndHookMethod(
            AudioRecord::class.java,
            "read",
            ShortArray::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val shortsRead = param.result as Int
                    if (shortsRead > 0) {
                        val buffer = param.args[0] as ShortArray
                        applyBasicPitchShift(buffer, shortsRead)
                    }
                }
            }
        )
        
        // Note: For native C++ audio (OpenSL ES/AAudio), Java AudioRecord is bypassed.
        // If Melodi's Party Room bypasses Java, you will need to implement a native C++ hook 
        // using frameworks like AndHook or Dobby to intercept slCreateEngine or AAudioStream_read.
    }

    /**
     * A very basic "Chipmunk" pitch shift (speeds up audio by dropping every other sample).
     * For high-quality voice changing, you should use a C++ DSP library like SoundTouch via JNI.
     */
    private fun applyBasicPitchShift(buffer: ByteArray, length: Int) {
        // Fast-forward effect: take every 2nd sample (assuming 16-bit PCM, so every 4th byte)
        val temp = ByteArray(length)
        var j = 0
        for (i in 0 until length - 3 step 4) {
            if (j < length - 1) {
                temp[j] = buffer[i]
                temp[j + 1] = buffer[i + 1]
                j += 2
            }
        }
        // Duplicate to fill the buffer
        val half = j
        for (i in 0 until half) {
            buffer[i] = temp[i]
            if (i + half < length) {
                buffer[i + half] = temp[i] // Repeat to fill gaps
            }
        }
    }

    private fun applyBasicPitchShift(buffer: ShortArray, length: Int) {
        val temp = ShortArray(length)
        var j = 0
        for (i in 0 until length - 1 step 2) {
            if (j < length) {
                temp[j] = buffer[i]
                j++
            }
        }
        val half = j
        for (i in 0 until half) {
            buffer[i] = temp[i]
            if (i + half < length) {
                buffer[i + half] = temp[i]
            }
        }
    }
}
