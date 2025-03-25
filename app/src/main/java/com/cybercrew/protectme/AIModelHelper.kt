package com.cybercrew.protectme

import android.content.Context
import android.util.Log
import ai.onnxruntime.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.nio.FloatBuffer

class AIModelHelper(context: Context) {
    private var ortSession: OrtSession? = null
    private val vectorizer: Map<String, Int>

    init {
        try {
            val env = OrtEnvironment.getEnvironment()
            val vectorizerPath = copyVectorizerToInternalStorage(context, "vectorizer_multilinual.json")
            val modelPath = copyModelToInternalStorage(context, "converted_model_opset19.onnx")
            val sessionOptions = OrtSession.SessionOptions()
            ortSession = env.createSession(modelPath, sessionOptions)
            Log.d("AI_MODEL", "✅ ONNX Model Loaded Successfully!")
        } catch (e: Exception) {
            Log.e("AI_MODEL", "❌ Error loading ONNX model: ${e.message}")
        }

        vectorizer = loadVectorizer(context)
    }

    private fun loadVectorizer(context: Context): Map<String, Int> {
        val file = File(context.filesDir, "vectorizer.json")
        return if (file.exists()) {
            val json = file.readText()
            val type = object : TypeToken<Map<String, Int>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyMap()
        }
    }

    private fun copyModelToInternalStorage(context: Context, modelFileName: String): String {
        val file = File(context.filesDir, modelFileName)

        context.assets.open(modelFileName).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }

        Log.d("AI_MODEL", "✅ Model copied to: ${file.absolutePath}")
        return file.absolutePath
    }

    private fun copyVectorizerToInternalStorage(context: Context, fileName: String): String {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            context.assets.open(fileName).use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return file.absolutePath
    }

    fun vectorize(text: String): FloatArray {
        val vector = FloatArray(5492) { 0f }

        text.split(" ").forEach { word ->
            vectorizer[word]?.let { index ->
                vector[index] += 1f
            }
        }

        Log.d(
            "AI_MODEL", "📝 Vectorized Non-Zero Features: ${
                vector.mapIndexed { i, v -> if (v > 0) "[$i] = $v" else "" }
                    .filter { it.isNotEmpty() }.joinToString()
            }"
        )

        return vector
    }


    fun predict(inputData: FloatArray): Float {
        return try {
            val floatBuffer = FloatBuffer.wrap(inputData)
            val inputTensor = OnnxTensor.createTensor(
                OrtEnvironment.getEnvironment(), floatBuffer, longArrayOf(1, 5492)
            )

            val output = ortSession?.run(mapOf("float_input" to inputTensor))

            if (output == null) {
                Log.e("AI_MODEL", "❌ ONNX Output is null!")
                return -1f
            }

            // ✅ Extract probability dictionary
            val outputProb = output["output_probability"] as? OnnxTensor ?: return -1f

            val resultArray = FloatArray(outputProb.floatBuffer.remaining())
            outputProb.floatBuffer.get(resultArray)

            if (resultArray.isEmpty()) {
                Log.e("AI_MODEL", "❌ Output tensor is empty!")
                return -1f
            }

            // Assuming resultArray contains probabilities in order: ["ham", "spam"]
            val spamProbability = resultArray[1]  // Index 1 corresponds to spam

            Log.d("AI_MODEL", "🔍 Spam Probability: $spamProbability")

            return spamProbability
        } catch (e: Exception) {
            Log.e("AI_MODEL", "❌ Prediction Error: ${e.message}")
            return -1f
        }
    }


    fun classifyMessage(sender: String, message: String) {
        Log.d("AI_MODEL", "📩 Received SMS: $message")

        val vectorizedInput = vectorize(message)
        Log.d("AI_MODEL", "📝 Vectorized SMS Features: ${vectorizedInput.joinToString()}")

        val spamProbability = predict(vectorizedInput)

        Log.d("AI_MODEL", "🔍 Spam Probability: $spamProbability")

        if (spamProbability > 0.3) {  // Lower threshold
            Log.d("AI_MODEL", "⚠ SPAM Detected from $sender: $message")
        } else {
            Log.d("AI_MODEL", "✅ Safe SMS from $sender: $message")
        }

    }
}