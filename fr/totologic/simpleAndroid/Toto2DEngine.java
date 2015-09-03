package fr.totologic.simpleAndroid;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Toto2DEngine {

    private Context _context;
    private OnSurfaceInitedListener _surfaceInitedListener;
    private OnSurfaceChangedListener _surfaceChangedListener;
    private OnSurfaceDrawListener _surfaceDrawListener;
    private GLSurfaceView _surface;
    private boolean _started = false;

    public interface OnSurfaceInitedListener
    {
        void onSurfaceInited();
    }
    public interface OnSurfaceChangedListener
    {
        void onSurfaceChanged(int width, int height);
    }
    public interface OnSurfaceDrawListener
    {
        void onSurfaceDraw();
    }

    public void setOnSurfaceInitedListener(OnSurfaceInitedListener listener)
    {
        _surfaceInitedListener = listener;
    }
    public void setOnSurfaceChangedListener(OnSurfaceChangedListener listener)
    {
        _surfaceChangedListener = listener;
    }
    public void setOnSurfaceDrawListener(OnSurfaceDrawListener listener)
    {
        _surfaceDrawListener = listener;
    }

    public boolean init(Context context)
    {
        _context = context;
        _surface = new GLSurfaceView(_context);

        final ActivityManager activityManager = (ActivityManager) _context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2)
        {
            _surface.setEGLContextClientVersion(2);
            _surface.setRenderer(new TotoRenderer());
        }
        else
        {
            return false;
        }

        return true;
    }

    public void destroy()
    {
        Log.v("log", "destroy toto2Dengine");

        _context = null;
        _surfaceInitedListener = null;
        _surfaceChangedListener = null;
        _surfaceDrawListener = null;
        _surface = null;
        _started = false;
    }

    public void setBackgroundColor(float r, float g, float b)
    {
        _backgroundRed = r;
        _backgroundGreen = g;
        _backgroundBlue = b;
        GLES20.glClearColor(_backgroundRed, _backgroundGreen, _backgroundBlue, 1.0f);
    }

    private final int[] _genTexture = new int[1];
    public boolean setAtlas(int id)
    {
        GLES20.glGenTextures(1, _genTexture, 0);
        if (_genTexture[0] == 0)
            return false;

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        final Bitmap bitmap = BitmapFactory.decodeResource(_context.getResources(), id, options);
        _textureWidth = bitmap.getWidth();
        _textureHeight = bitmap.getHeight();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _genTexture[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        //GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap, 0);
        bitmap.recycle();

        return true;
    }

    private float __mat_0_0;
    private float __mat_1_0;
    private float __mat_2_0;
    private float __mat_0_1;
    private float __mat_1_1;
    private float __mat_2_1;
    private float __cRot;
    private float __sRot;
    private float __sxcRot;
    private float __sxsRot;
    private float __sycRot;
    private float __sysRot;
    public void addSprite(float xOnAtlas, float yOnAtlas, float widthOnAtlas, float heightOnAtlas, float xOnScreen, float yOnScreen)
    {
        if (_countSprite == MAX_SPRITES)
            flushOnScreen();

        __mat_0_0 = 1.0f;
        __mat_1_0 = 0.0f;
        __mat_2_0 = xOnScreen;
        __mat_0_1 = 0.0f;
        __mat_1_1 = 1.0f;
        __mat_2_1 = yOnScreen;
        _transfMatricesDatas.add(__mat_0_0);
        _transfMatricesDatas.add(__mat_0_1);
        _transfMatricesDatas.add(0.0f);
        _transfMatricesDatas.add(__mat_1_0);
        _transfMatricesDatas.add(__mat_1_1);
        _transfMatricesDatas.add(0.0f);
        _transfMatricesDatas.add(__mat_2_0);
        _transfMatricesDatas.add(__mat_2_1);
        _transfMatricesDatas.add(1.0f);

        // 1|\
        //  | \
        //  |  \
        //  |   \
        //  |    \
        // 2------3
        _datas.add(0.0f);                       _datas.add(0.0f);                       _datas.add(0.0f);
        _datas.add(xOnAtlas);                   _datas.add(yOnAtlas);
        _datas.add((float)_countSprite);
        _datas.add(0.0f);                       _datas.add(heightOnAtlas);              _datas.add(0.0f);
        _datas.add(xOnAtlas);                   _datas.add(yOnAtlas+heightOnAtlas);
        _datas.add((float)_countSprite);
        _datas.add(widthOnAtlas);               _datas.add(heightOnAtlas);              _datas.add(0.0f);
        _datas.add(xOnAtlas+widthOnAtlas);      _datas.add(yOnAtlas+heightOnAtlas);
        _datas.add((float)_countSprite);
        //
        // 1------3
        //  \    |
        //   \   |
        //    \  |
        //     \ |
        //      \|2
        _datas.add(0.0f);                       _datas.add(0.0f);                       _datas.add(0.0f);
        _datas.add(xOnAtlas);                   _datas.add(yOnAtlas);
        _datas.add((float)_countSprite);
        _datas.add(widthOnAtlas);               _datas.add(heightOnAtlas);              _datas.add(0.0f);
        _datas.add(xOnAtlas+widthOnAtlas);      _datas.add(yOnAtlas+heightOnAtlas);
        _datas.add((float)_countSprite);
        _datas.add(widthOnAtlas);               _datas.add(0.0f);                       _datas.add(0.0f);
        _datas.add(xOnAtlas+widthOnAtlas);      _datas.add(yOnAtlas);
        _datas.add((float)_countSprite);

        _countSprite++;
    }

    public void addSprite(float xOnAtlas, float yOnAtlas, float widthOnAtlas, float heightOnAtlas
            , float rotationOnScreen, float xScaleOnScreen, float yScaleOnScreen, float xOnScreen, float yOnScreen)
    {
        if (_countSprite == MAX_SPRITES)
            flushOnScreen();

        __cRot = (float) Math.cos(rotationOnScreen);
        __sRot = (float) Math.sin(rotationOnScreen);
        __sxcRot = xScaleOnScreen*__cRot;
        __sxsRot = xScaleOnScreen*__sRot;
        __sycRot = yScaleOnScreen*__cRot;
        __sysRot = yScaleOnScreen*-__sRot;
        __mat_0_0 = __sxcRot;
        __mat_1_0 = __sysRot;
        __mat_2_0 = __sxcRot + __sysRot + xOnScreen;
        __mat_0_1 = __sxsRot;
        __mat_1_1 = __sycRot;
        __mat_2_1 = __sxsRot + __sycRot + yOnScreen;
        _transfMatricesDatas.add(__mat_0_0);
        _transfMatricesDatas.add(__mat_0_1);
        _transfMatricesDatas.add(0.0f);
        _transfMatricesDatas.add(__mat_1_0);
        _transfMatricesDatas.add(__mat_1_1);
        _transfMatricesDatas.add(0.0f);
        _transfMatricesDatas.add(__mat_2_0);
        _transfMatricesDatas.add(__mat_2_1);
        _transfMatricesDatas.add(1.0f);

        // 1|\
        //  | \
        //  |  \
        //  |   \
        //  |    \
        // 2------3
        _datas.add(0.0f);                       _datas.add(0.0f);                       _datas.add(0.0f);
        _datas.add(xOnAtlas);                   _datas.add(yOnAtlas);
        _datas.add((float)_countSprite);
        _datas.add(0.0f);                       _datas.add(heightOnAtlas);              _datas.add(0.0f);
        _datas.add(xOnAtlas);                   _datas.add(yOnAtlas+heightOnAtlas);
        _datas.add((float)_countSprite);
        _datas.add(widthOnAtlas);               _datas.add(heightOnAtlas);              _datas.add(0.0f);
        _datas.add(xOnAtlas+widthOnAtlas);      _datas.add(yOnAtlas+heightOnAtlas);
        _datas.add((float)_countSprite);
        //
        // 1------3
        //  \    |
        //   \   |
        //    \  |
        //     \ |
        //      \|2
        _datas.add(0.0f);                       _datas.add(0.0f);                       _datas.add(0.0f);
        _datas.add(xOnAtlas);                   _datas.add(yOnAtlas);
        _datas.add((float)_countSprite);
        _datas.add(widthOnAtlas);               _datas.add(heightOnAtlas);              _datas.add(0.0f);
        _datas.add(xOnAtlas+widthOnAtlas);      _datas.add(yOnAtlas+heightOnAtlas);
        _datas.add((float)_countSprite);
        _datas.add(widthOnAtlas);               _datas.add(0.0f);                       _datas.add(0.0f);
        _datas.add(xOnAtlas+widthOnAtlas);      _datas.add(yOnAtlas);
        _datas.add((float)_countSprite);

        _countSprite++;
    }

    public void addSprite(float xOnAtlas, float yOnAtlas, float widthOnAtlas, float heightOnAtlas
            , float t1x, float t1y, float xScale, float yScale, float rot, float t2x, float t2y)
    {
        if (_countSprite == MAX_SPRITES)
            flushOnScreen();

        __cRot = (float) Math.cos(rot);
        __sRot = (float) Math.sin(rot);
        __sxcRot = xScale*__cRot;
        __sxsRot = xScale*__sRot;
        __sycRot = yScale*__cRot;
        __sysRot = yScale*-__sRot;
        __mat_0_0 = __sxcRot;
        __mat_1_0 = __sysRot;
        __mat_2_0 = t1x*__sxcRot + t1y*__sysRot + t2x;
        __mat_0_1 = __sxsRot;
        __mat_1_1 = __sycRot;
        __mat_2_1 = t1x*__sxsRot + t1y*__sycRot + t2y;
        _transfMatricesDatas.add(__mat_0_0);
        _transfMatricesDatas.add(__mat_0_1);
        _transfMatricesDatas.add(0.0f);
        _transfMatricesDatas.add(__mat_1_0);
        _transfMatricesDatas.add(__mat_1_1);
        _transfMatricesDatas.add(0.0f);
        _transfMatricesDatas.add(__mat_2_0);
        _transfMatricesDatas.add(__mat_2_1);
        _transfMatricesDatas.add(1.0f);

        // 1|\
        //  | \
        //  |  \
        //  |   \
        //  |    \
        // 2------3
        _datas.add(0.0f);                       _datas.add(0.0f);                       _datas.add(0.0f);
        _datas.add(xOnAtlas);                   _datas.add(yOnAtlas);
        _datas.add((float)_countSprite);
        _datas.add(0.0f);                       _datas.add(heightOnAtlas);              _datas.add(0.0f);
        _datas.add(xOnAtlas);                   _datas.add(yOnAtlas+heightOnAtlas);
        _datas.add((float)_countSprite);
        _datas.add(widthOnAtlas);               _datas.add(heightOnAtlas);              _datas.add(0.0f);
        _datas.add(xOnAtlas+widthOnAtlas);      _datas.add(yOnAtlas+heightOnAtlas);
        _datas.add((float)_countSprite);
        //
        // 1------3
        //  \    |
        //   \   |
        //    \  |
        //     \ |
        //      \|2
        _datas.add(0.0f);                       _datas.add(0.0f);                       _datas.add(0.0f);
        _datas.add(xOnAtlas);                   _datas.add(yOnAtlas);
        _datas.add((float)_countSprite);
        _datas.add(widthOnAtlas);               _datas.add(heightOnAtlas);              _datas.add(0.0f);
        _datas.add(xOnAtlas+widthOnAtlas);      _datas.add(yOnAtlas+heightOnAtlas);
        _datas.add((float)_countSprite);
        _datas.add(widthOnAtlas);               _datas.add(0.0f);                       _datas.add(0.0f);
        _datas.add(xOnAtlas+widthOnAtlas);      _datas.add(yOnAtlas);
        _datas.add((float)_countSprite);

        _countSprite++;
    }

    public void start()
    {
        _started = true;
    }

    public void stop()
    {
        _started = false;
    }


    private final int BYTES_PER_FLOAT = 4;
    private final int MAX_SPRITES = 50;

    private final String VERTEX_SHADER_CODE =
            "uniform vec2 screenSize;                                                                                           \n" +
            "uniform vec2 textureSize;                                                                                          \n" +
            "uniform mat3 transfMatricesList[" + String.valueOf(MAX_SPRITES) + "];                                              \n" +
            "attribute vec4 position;                                                                                           \n" +
            "attribute vec2 uv;                                                                                                 \n" +
            "attribute float spriteIndex;                                                                                       \n" +
            "varying vec2 texCoord;                                                                                             \n" +
            "void main()                                                                                                        \n" +
            "{                                                                                                                  \n" +
            "   int i = int(spriteIndex);                                                                                       \n" +
            "   texCoord = vec2(uv.x / textureSize.x, uv.y / textureSize.y);                                                    \n" +
            "   vec4 transfPos;                                                                                                 \n" +
            "   transfPos.xyw = transfMatricesList[i] * position.xyw;                                                           \n" +
            "   gl_Position = vec4(2.0 * transfPos.x / screenSize.x - 1.0, 1.0 - 2.0 * transfPos.y / screenSize.y, 0.0, 1.0);   \n" +
            "}                                                                                                                  \n" +
            "";

    private final String FRAGMENT_SHADER_CODE =
            "precision mediump float;                                       \n" +
            "uniform sampler2D texture;                                     \n" +
            "varying vec2 texCoord;                                         \n" +
            "void main()                                                    \n" +
            "{                                                              \n" +
            "   gl_FragColor = texture2D(texture, texCoord);                \n" +
            "}                                                              \n" +
            "";

    //
    private float _screenWidth;
    private float _screenHeight;
    private float _textureWidth;
    private float _textureHeight;
    //
    private float _backgroundRed;
    private float _backgroundGreen;
    private float _backgroundBlue;
    //
    // GEOMETRY
    private int _countSprite;
    private ArrayList<Float> _datas;
    private float[] _bufferDatas;
    private FloatBuffer _byteBufferDatas;
    private ArrayList<Float> _transfMatricesDatas;
    private float[] _bufferTransfMatricesDatas = new float[MAX_SPRITES*9];
    //
    // UNIFORMS
    private int _screenSizeUniform;
    private int _textureUniform;
    private int _textureSizeUniform;
    private int _transfMatricesListUniform;
    //
    // ATTRIBUTES
    private int _positionAttribute;
    private int _uvAttribute;
    private int _spriteIndexAttribute;

    private int __geomSize;
    private void flushOnScreen()
    {
        __geomSize = _datas.size();
        _bufferDatas = new float[__geomSize];
        for (int i=0 ; i<__geomSize ; i++)
            _bufferDatas[i] = _datas.get(i);

        _byteBufferDatas = ByteBuffer.allocateDirect(_bufferDatas.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        _byteBufferDatas.put(_bufferDatas).position(0);

        GLES20.glUniform2f(_screenSizeUniform, _screenWidth, _screenHeight);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _genTexture[0]);
        GLES20.glUniform1i(_textureUniform, 0);

        GLES20.glUniform2f(_textureSizeUniform, _textureWidth, _textureHeight);

        int count = _transfMatricesDatas.size();
        for (int i=0 ; i<count ; i++)
            _bufferTransfMatricesDatas[i] = _transfMatricesDatas.get(i);
        GLES20.glUniformMatrix3fv(_transfMatricesListUniform, _countSprite, false, _bufferTransfMatricesDatas, 0);

        _byteBufferDatas.position(0);
        GLES20.glVertexAttribPointer(_positionAttribute, 3, GLES20.GL_FLOAT, false, 6 * BYTES_PER_FLOAT, _byteBufferDatas);
        GLES20.glEnableVertexAttribArray(_positionAttribute);

        _byteBufferDatas.position(3);
        GLES20.glVertexAttribPointer(_uvAttribute, 2, GLES20.GL_FLOAT, false, 6 * BYTES_PER_FLOAT, _byteBufferDatas);
        GLES20.glEnableVertexAttribArray(_uvAttribute);

        _byteBufferDatas.position(5);
        GLES20.glVertexAttribPointer(_spriteIndexAttribute, 1, GLES20.GL_FLOAT, false, 6 * BYTES_PER_FLOAT, _byteBufferDatas);
        GLES20.glEnableVertexAttribArray(_spriteIndexAttribute);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, _countSprite * 6);

        _countSprite = 0;
        _datas = new ArrayList<Float>();
        _transfMatricesDatas = new ArrayList<Float>();
    }

    class TotoRenderer implements GLSurfaceView.Renderer
    {
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            _countSprite = 0;
            _datas = new ArrayList<Float>();
            _transfMatricesDatas = new ArrayList<Float>();

            _backgroundRed = 0.0f;
            _backgroundGreen = 1.0f;
            _backgroundBlue = 0.0f;


            // VERTEX SHADER COMPILE
            int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
            GLES20.glShaderSource(vertexShader, VERTEX_SHADER_CODE);
            GLES20.glCompileShader(vertexShader);
            // Get the compilation status.
            final int[] vertexShaderCompileStatus = new int[1];
            GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, vertexShaderCompileStatus, 0);
            // If the compilation failed, delete the shader.
            if (vertexShaderCompileStatus[0] == 0)
            {
                Log.v("log", "vertex shader compilation error");
                GLES20.glDeleteShader(vertexShader);
                return;
            }
            Log.v("log", "vertex shader compilation success");

            // FRAGMENT SHADER COMPILE
            int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
            GLES20.glShaderSource(fragmentShader, FRAGMENT_SHADER_CODE);
            GLES20.glCompileShader(fragmentShader);
            // Get the compilation status.
            final int[] fragmentShaderCompileStatus = new int[1];
            GLES20.glGetShaderiv(fragmentShader, GLES20.GL_COMPILE_STATUS, fragmentShaderCompileStatus, 0);
            // If the compilation failed, delete the shader.
            if (fragmentShaderCompileStatus[0] == 0)
            {
                Log.v("log", "fragment shader compilation error");
                GLES20.glDeleteShader(fragmentShader);
                return;
            }
            Log.v("log", "fragment shader compilation success");

            // CREATE PROGRAM
            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            // Bind attributes
            GLES20.glBindAttribLocation(program, 0, "position");
            GLES20.glBindAttribLocation(program, 1, "uv");
            GLES20.glLinkProgram(program);
            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0)
            {
                Log.v("log", "program link error");
                GLES20.glDeleteProgram(program);
                return;
            }
            Log.v("log", "program link success");

            _screenSizeUniform = GLES20.glGetUniformLocation(program, "screenSize");
            _textureUniform = GLES20.glGetUniformLocation(program, "texture");
            _textureSizeUniform = GLES20.glGetUniformLocation(program, "textureSize");
            _transfMatricesListUniform = GLES20.glGetUniformLocation(program, "transfMatricesList");
            //
            _positionAttribute = GLES20.glGetAttribLocation(program, "position");
            _uvAttribute = GLES20.glGetAttribLocation(program, "uv");
            _spriteIndexAttribute = GLES20.glGetAttribLocation(program, "spriteIndex");
            GLES20.glUseProgram(program);

            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFuncSeparate(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA, GLES20.GL_ONE, GLES20.GL_ONE);

            // background color
            GLES20.glClearColor(_backgroundRed, _backgroundGreen, _backgroundBlue, 1.0f);


            if (_surfaceInitedListener != null)
                _surfaceInitedListener.onSurfaceInited();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Log.v("log", "onSurfaceChanged");
            Log.v("log", String.valueOf(width));
            Log.v("log", String.valueOf(height));
            _screenWidth = (float)width;
            _screenHeight = (float)height;
            if (_surfaceChangedListener != null)
                _surfaceChangedListener.onSurfaceChanged(width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (!_started)
                return;

            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

            if (_surfaceDrawListener != null)
                _surfaceDrawListener.onSurfaceDraw();

            if (_countSprite > 0)
                flushOnScreen();
        }
    }

    public GLSurfaceView getSurface() {
        return _surface;
    }

    public float getScreenWidth() {
        return _screenWidth;
    }

    public float getScreenHeight() {
        return _screenHeight;
    }


}
