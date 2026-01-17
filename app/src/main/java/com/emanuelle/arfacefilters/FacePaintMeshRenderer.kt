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

class FacePaintMeshRenderer {

    private var program = 0
    private var aPosition = 0
    private var aUv = 0
    private var uMvp = 0
    private var uTexture = 0

    private var textureId = 0

    // buffers (atualizados por frame)
    private var vertexBuffer: FloatBuffer? = null
    private var uvBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    private var indexCount = 0

    fun createOnGlThread(context: Context, textureResId: Int) {
        val vs = """
            uniform mat4 u_Mvp;
            attribute vec3 a_Position;
            attribute vec2 a_Uv;
            varying vec2 v_Uv;
            void main() {
              v_Uv = a_Uv;
              gl_Position = u_Mvp * vec4(a_Position, 1.0);
            }
        """.trimIndent()

        val fs = """
            precision mediump float;
            uniform sampler2D u_Texture;
            varying vec2 v_Uv;
            void main() {
              vec4 c = texture2D(u_Texture, v_Uv);
              // respeita alpha do PNG (pintura pode ter partes transparentes)
              gl_FragColor = c;
            }
        """.trimIndent()

        program = createProgram(vs, fs)
        aPosition = GLES20.glGetAttribLocation(program, "a_Position")
        aUv = GLES20.glGetAttribLocation(program, "a_Uv")
        uMvp = GLES20.glGetUniformLocation(program, "u_Mvp")
        uTexture = GLES20.glGetUniformLocation(program, "u_Texture")

        textureId = loadTexture2D(context, textureResId)
    }

    fun draw(face: AugmentedFace, view: FloatArray, proj: FloatArray) {
        if (face.trackingState != TrackingState.TRACKING) return
        if (textureId == 0 || program == 0) return

        // Pega malha do rosto (ARCore fornece vertices + UV + indices)
        val verts = face.meshVertices
        val uvs = face.meshTextureCoordinates
        val indices = face.meshTriangleIndices

        // Aloca/reusa buffers no tamanho correto
        if (vertexBuffer == null || vertexBuffer!!.capacity() != verts.limit()) {
            vertexBuffer = ByteBuffer.allocateDirect(verts.limit() * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        }
        if (uvBuffer == null || uvBuffer!!.capacity() != uvs.limit()) {
            uvBuffer = ByteBuffer.allocateDirect(uvs.limit() * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        }
        if (indexBuffer == null || indexBuffer!!.capacity() != indices.limit()) {
            indexBuffer = ByteBuffer.allocateDirect(indices.limit() * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
        }

        // Copia dados (buffers do ARCore mudam por frame)
        vertexBuffer!!.position(0)
        vertexBuffer!!.put(verts)
        vertexBuffer!!.position(0)

        uvBuffer!!.position(0)
        uvBuffer!!.put(uvs)
        uvBuffer!!.position(0)

        indexBuffer!!.position(0)
        indexBuffer!!.put(indices)
        indexBuffer!!.position(0)

        indexCount = indices.limit()

        // Model do rosto
        val model = FloatArray(16)
        face.centerPose.toMatrix(model, 0)

        val mv = FloatArray(16)
        val mvp = FloatArray(16)
        android.opengl.Matrix.multiplyMM(mv, 0, view, 0, model, 0)
        android.opengl.Matrix.multiplyMM(mvp, 0, proj, 0, mv, 0)

        GLES20.glUseProgram(program)

        // Para “pintura”, normalmente fica bom com depth ligado (fica colado)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // Alpha para partes transparentes do PNG
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTexture, 0)

        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(aUv)
        GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 0, uvBuffer)

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            indexCount,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aUv)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDisable(GLES20.GL_BLEND)
        // depth pode ficar ligado para o resto
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
