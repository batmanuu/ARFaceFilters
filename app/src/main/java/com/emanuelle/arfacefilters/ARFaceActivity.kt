package com.emanuelle.arfacefilters

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.UnsupportedConfigurationException
import java.util.EnumSet
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARFaceActivity : ComponentActivity(), GLSurfaceView.Renderer {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var btnToggle: Button

    private val backgroundRenderer = BackgroundRenderer()
    private val overlayRenderer = GlassesRenderer()
    private val facePaintMeshRenderer = FacePaintMeshRenderer()

    private var session: Session? = null

    private var viewportWidth = 0
    private var viewportHeight = 0
    private var lastDisplayRotation = -1

    private enum class Filter { GLASSES_TEXT, BATMASK, FACEPAINT_MESH }
    private var currentFilter: Filter = Filter.GLASSES_TEXT

    // ===========================
    // AJUSTES (X/Y/Z/SIZE/WIDE)
    // ===========================

    // ÓCULOS
    private val GLASSES_X = 0.000f
    private val GLASSES_Y = 0.01f
    private val GLASSES_Z = 0.06f
    private val GLASSES_SIZE_Y = 1.00f
    private val GLASSES_WIDE = 1.50f

    // TEXTO
    private val TEXT_X = 0.000f
    private val TEXT_Y = 0.09f
    private val TEXT_Z = 0.10f
    private val TEXT_SIZE_Y = 3.00f
    private val TEXT_WIDE = 4.00f

    // MÁSCARA (Batman)
    private val MASK_X = 0.000f
    private val MASK_Y = 0.04f
    private val MASK_Z = 0.090f
    private val MASK_SIZE_Y = 2f
    private val MASK_WIDE = 2.10f

    // ===========================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_face)

        glSurfaceView = findViewById(R.id.glSurfaceView)
        glSurfaceView.preserveEGLContextOnPause = true
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(this)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        btnToggle = findViewById(R.id.btnToggleFilter)
        btnToggle.setOnClickListener {
            currentFilter = when (currentFilter) {
                Filter.GLASSES_TEXT -> Filter.BATMASK
                Filter.BATMASK -> Filter.FACEPAINT_MESH
                Filter.FACEPAINT_MESH -> Filter.GLASSES_TEXT
            }
            Toast.makeText(this, "Filtro: $currentFilter", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        try {
            when (ArCoreApk.getInstance().requestInstall(this, true)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> return
                ArCoreApk.InstallStatus.INSTALLED -> {}
            }

            if (session == null) {
                session = Session(this, EnumSet.of(Session.Feature.FRONT_CAMERA))
            }

            val s = session ?: return

            val config = Config(s).apply {
                augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
            }
            s.configure(config)

            s.resume()
            glSurfaceView.onResume()

        } catch (e: UnsupportedConfigurationException) {
            Toast.makeText(this, "Device não suporta ARCore Augmented Faces.", Toast.LENGTH_LONG).show()
        } catch (t: Throwable) {
            Toast.makeText(this, "Falha AR: ${t::class.java.simpleName}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
        session?.pause()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        backgroundRenderer.createOnGlThread()

        // 4 texturas: óculos + texto + máscara + facepaint (sticker)
        // (mesmo que você use "FACEPAINT_MESH", é ok carregar a textura aqui também)
        overlayRenderer.createOnGlThread(
            this,
            R.drawable.glasses,
            R.drawable.texto_computacao_grafica,
            R.drawable.batmascara,
            R.drawable.facepaint
        )

        // Pintura "de verdade" aplicada na malha do rosto (deforma com o rosto)
        facePaintMeshRenderer.createOnGlThread(this, R.drawable.facepaint)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)
    }

    @Suppress("DEPRECATION")
    override fun onDrawFrame(gl: GL10?) {
        val s = session ?: return

        try {
            val displayRotation = windowManager.defaultDisplay.rotation
            if (displayRotation != lastDisplayRotation && viewportWidth != 0 && viewportHeight != 0) {
                lastDisplayRotation = displayRotation
                s.setDisplayGeometry(displayRotation, viewportWidth, viewportHeight)
            }

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

            // Fundo (câmera)
            s.setCameraTextureName(backgroundRenderer.getTextureId())
            val frame = s.update()
            backgroundRenderer.draw(frame)

            // Matrizes
            val view = FloatArray(16)
            val proj = FloatArray(16)
            frame.camera.getViewMatrix(view, 0)
            frame.camera.getProjectionMatrix(proj, 0, 0.1f, 100.0f)

            // Um rosto tracking (evita overlays duplicados)
            val face = s.getAllTrackables(AugmentedFace::class.java)
                .firstOrNull { it.trackingState == TrackingState.TRACKING }

            if (face != null) {
                when (currentFilter) {

                    Filter.GLASSES_TEXT -> {
                        // ÓCULOS
                        overlayRenderer.drawAnchored(
                            face = face,
                            view = view,
                            proj = proj,
                            anchor = AugmentedFace.RegionType.NOSE_TIP,
                            texture = GlassesRenderer.TextureSlot.GLASSES,
                            offsetX = GLASSES_X,
                            offsetY = GLASSES_Y,
                            offsetZ = GLASSES_Z,
                            sizeY = GLASSES_SIZE_Y,
                            wide = GLASSES_WIDE
                        )

                        // TEXTO
                        overlayRenderer.drawAnchored(
                            face = face,
                            view = view,
                            proj = proj,
                            anchor = AugmentedFace.RegionType.NOSE_TIP,
                            texture = GlassesRenderer.TextureSlot.TEXT,
                            offsetX = TEXT_X,
                            offsetY = TEXT_Y,
                            offsetZ = TEXT_Z,
                            sizeY = TEXT_SIZE_Y,
                            wide = TEXT_WIDE
                        )
                    }

                    Filter.BATMASK -> {
                        overlayRenderer.drawAnchored(
                            face = face,
                            view = view,
                            proj = proj,
                            anchor = AugmentedFace.RegionType.NOSE_TIP,
                            texture = GlassesRenderer.TextureSlot.MASK,
                            offsetX = MASK_X,
                            offsetY = MASK_Y,
                            offsetZ = MASK_Z,
                            sizeY = MASK_SIZE_Y,
                            wide = MASK_WIDE
                        )
                    }

                    Filter.FACEPAINT_MESH -> {
                        // Pintura aplicada na malha do rosto (deforma com expressões)
                        facePaintMeshRenderer.draw(face, view, proj)
                    }
                }
            }

        } catch (_: Throwable) {
            // evita crash por frame
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onResume()
        } else {
            Toast.makeText(this, "Permissão de câmera é necessária.", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
