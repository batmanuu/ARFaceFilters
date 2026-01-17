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
import java.nio.ShortBuffer

class FaceMeshRenderer {

    private var program = 0
    private var textureId = 0

    private var aPosition = 0
    private var aTexCoord = 0
    private var uMvp = 0
    private var uTexture = 0

    // buffers (recriados conforme o face mesh muda)
    private var vertexBuffer: FloatBuffer? = null
    private var texBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    private var indexCount = 0

    fun createOnGlThread(context: Context, textureResId: Int) {
        val vShader = """
            uniform mat4 u_Mvp;
            attribute vec3 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
              v_TexCoord = a_TexCoord;
              gl_Position = u_Mvp * vec4(a_Position, 1.0);
            }
        """.trimIndent()

        val fShader = """
            precision mediump float;
            uniform sampler2D u_Texture;
            varying vec2 v_TexCoord;
            void main() {
              vec4 c = texture2D(u_Texture, v_TexCoord);
              // mantém o alpha do PNG (óculos com transparência)
              gl_FragColor = c;
            }
        """.trimIndent()

        program = createProgram(vShader, fShader)
        aPosition = GLES20.glGetAttribLocation(program, "a_Position")
        aTexCoord = GLES20.glGetAttribLocation(program, "a_TexCoord")
        uMvp = GLES20.glGetUniformLocation(program, "u_Mvp")
        uTexture = GLES20.glGetUniformLocation(program, "u_Texture")

        textureId = loadTexture2D(context, textureResId)
    }

    fun draw(face: AugmentedFace, mvp: FloatArray) {
        if (face.trackingState != TrackingState.TRACKING) return

        // 1) vertices (FloatBuffer com x,y,z)
        val vb = face.meshVertices
        vb.rewind()
        val vCount = vb.limit() / 3

        // 2) uvs (FloatBuffer com u,v)
        val tb = face.meshTextureCoordinates
        tb.rewind()

        // 3) indices (ShortBuffer)
        val ib = face.meshTriangleIndices
        ib.rewind()
        indexCount = ib.limit()

        // Copia para buffers NIO “diretos” (mais seguro para GLES)
        vertexBuffer = ensureFloatBuffer(vertexBuffer, vb.limit())
        texBuffer = ensureFloatBuffer(texBuffer, tb.limit())
        indexBuffer = ensureShortBuffer(indexBuffer, indexCount)

        vertexBuffer!!.put(vb).position(0)
        texBuffer!!.put(tb).position(0)
        indexBuffer!!.put(ib).position(0)

        // Render state (óculos com alpha)
        GLES20.glUseProgram(program)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // (Opcional) desenhar por cima de tudo:
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTexture, 0)

        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(aTexCoord)
        GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 0, texBuffer)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aTexCoord)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun ensureFloatBuffer(existing: FloatBuffer?, floatCount: Int): FloatBuffer {
        if (existing != null && existing.capacity() >= floatCount) {
            existing.clear()
            return existing
        }
        return ByteBuffer.allocateDirect(floatCount * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }

    private fun ensureShortBuffer(existing: ShortBuffer?, shortCount: Int): ShortBuffer {
        if (existing != null && existing.capacity() >= shortCount) {
            existing.clear()
            return existing
        }
        return ByteBuffer.allocateDirect(shortCount * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
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
        return s
    }

    private fun createProgram(vsCode: String, fsCode: String): Int {
        val vs = loadShader(GLES20.GL_VERTEX_SHADER, vsCode)
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fsCode)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vs)
        GLES20.glAttachShader(p, fs)
        GLES20.glLinkProgram(p)
        return p
    }
}
