package com.emanuelle.arfacefilters

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.sqrt

class GlassesRenderer {

    private var program = 0

    private var aPosition = 0
    private var aTexCoord = 0
    private var uMvp = 0
    private var uTexture = 0

    private var glassesTexId = 0
    private var textTexId = 0
    private var maskTexId = 0
    private var paintTexId = 0

    enum class TextureSlot { GLASSES, TEXT, MASK, PAINT }

    // Quad base (aspect vem da escala em X/Y)
    private val quadVerts = floatArrayOf(
        -0.5f, -0.5f, 0f,
        0.5f, -0.5f, 0f,
        -0.5f,  0.5f, 0f,
        0.5f,  0.5f, 0f
    )

    private val quadUV = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    private val vbo: FloatBuffer =
        ByteBuffer.allocateDirect(quadVerts.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(quadVerts); position(0) }

    private val tbo: FloatBuffer =
        ByteBuffer.allocateDirect(quadUV.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(quadUV); position(0) }

    /**
     * Carrega 4 texturas:
     *  - glassesResId = óculos
     *  - textResId    = texto
     *  - maskResId    = máscara
     *  - paintResId   = pintura (facepaint)
     */
    fun createOnGlThread(
        context: Context,
        glassesResId: Int,
        textResId: Int,
        maskResId: Int,
        paintResId: Int
    ) {
        val vs = """
            uniform mat4 u_Mvp;
            attribute vec3 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
              v_TexCoord = a_TexCoord;
              gl_Position = u_Mvp * vec4(a_Position, 1.0);
            }
        """.trimIndent()

        val fs = """
            precision mediump float;
            uniform sampler2D u_Texture;
            varying vec2 v_TexCoord;
            void main() {
              vec4 c = texture2D(u_Texture, v_TexCoord);
              gl_FragColor = c; // respeita alpha do PNG
            }
        """.trimIndent()

        program = createProgram(vs, fs)

        aPosition = GLES20.glGetAttribLocation(program, "a_Position")
        aTexCoord = GLES20.glGetAttribLocation(program, "a_TexCoord")
        uMvp = GLES20.glGetUniformLocation(program, "u_Mvp")
        uTexture = GLES20.glGetUniformLocation(program, "u_Texture")

        glassesTexId = loadTexture2D(context, glassesResId)
        textTexId = loadTexture2D(context, textResId)
        maskTexId = loadTexture2D(context, maskResId)
        paintTexId = loadTexture2D(context, paintResId)
    }

    fun drawAnchored(
        face: AugmentedFace,
        view: FloatArray,
        proj: FloatArray,
        anchor: AugmentedFace.RegionType,
        texture: TextureSlot,
        offsetX: Float,
        offsetY: Float,
        offsetZ: Float,
        sizeY: Float,
        wide: Float
    ) {
        if (face.trackingState != TrackingState.TRACKING) return

        val texId = when (texture) {
            TextureSlot.GLASSES -> glassesTexId
            TextureSlot.TEXT -> textTexId
            TextureSlot.MASK -> maskTexId
            TextureSlot.PAINT -> paintTexId
        }
        if (texId == 0) return

        val anchorPose = face.getRegionPose(anchor)

        // escala baseada na largura do rosto
        val left = face.getRegionPose(AugmentedFace.RegionType.FOREHEAD_LEFT)
        val right = face.getRegionPose(AugmentedFace.RegionType.FOREHEAD_RIGHT)
        val dx = right.tx() - left.tx()
        val dy = right.ty() - left.ty()
        val dz = right.tz() - left.tz()
        val faceWidth = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.10f)

        val model = FloatArray(16)
        anchorPose.toMatrix(model, 0)

        val translate = FloatArray(16)
        android.opengl.Matrix.setIdentityM(translate, 0)
        android.opengl.Matrix.translateM(translate, 0, offsetX, offsetY, offsetZ)

        val scale = FloatArray(16)
        android.opengl.Matrix.setIdentityM(scale, 0)
        android.opengl.Matrix.scaleM(scale, 0, faceWidth * wide, faceWidth * sizeY, 1f)

        val modelTmp = FloatArray(16)
        val modelFinal = FloatArray(16)
        android.opengl.Matrix.multiplyMM(modelTmp, 0, model, 0, translate, 0)
        android.opengl.Matrix.multiplyMM(modelFinal, 0, modelTmp, 0, scale, 0)

        val mv = FloatArray(16)
        val mvp = FloatArray(16)
        android.opengl.Matrix.multiplyMM(mv, 0, view, 0, modelFinal, 0)
        android.opengl.Matrix.multiplyMM(mvp, 0, proj, 0, mv, 0)

        drawQuad(mvp, texId)
    }

    private fun drawQuad(mvp: FloatArray, texId: Int) {
        GLES20.glUseProgram(program)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glUniform1i(uTexture, 0)

        vbo.position(0)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, vbo)

        tbo.position(0)
        GLES20.glEnableVertexAttribArray(aTexCoord)
        GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 0, tbo)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aTexCoord)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun loadTexture2D(context: Context, resId: Int): Int {
        val bmp = BitmapFactory.decodeResource(context.resources, resId)
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        bmp.recycle()
        return tex[0]
    }

    private fun loadShader(type: Int, code: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, code)
        GLES20.glCompileShader(s)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(s)
            GLES20.glDeleteShader(s)
            throw RuntimeException("Shader compile error: $log")
        }
        return s
    }

    private fun createProgram(vsCode: String, fsCode: String): Int {
        val vs = loadShader(GLES20.GL_VERTEX_SHADER, vsCode)
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fsCode)

        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vs)
        GLES20.glAttachShader(p, fs)
        GLES20.glLinkProgram(p)

        val linked = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, linked, 0)
        if (linked[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(p)
            GLES20.glDeleteProgram(p)
            throw RuntimeException("Program link error: $log")
        }
        return p
    }
}
