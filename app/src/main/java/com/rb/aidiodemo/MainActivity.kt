package com.rb.aidiodemo

import android.Manifest
import android.media.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.rb.aidiodemo.utils.PcmToWavUtil
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import kotlin.concurrent.thread
import com.permissionx.guolindev.PermissionX
import java.io.ByteArrayOutputStream
import java.io.FileInputStream


class MainActivity : AppCompatActivity() {

    private var recordBufferSize = 0
    private var audioRecord : AudioRecord? = null
    private var isRecording = false
    private var sdCardExist = false
    private var sdDir : File? = null
    private var player : MediaPlayer? = null
    private var wavFile : File? = null

    private var audioTrack : AudioTrack? = null
    private var audioData : ByteArray? = null
    private var trackBufferSize = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getSDCardPath()

        PermissionX.init(this)
            .permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO)
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    Toast.makeText(this, "All permissions are granted", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "These permissions are denied: $deniedList", Toast.LENGTH_LONG).show()
                }
            }

        player = MediaPlayer()
        wavFile = File(Environment.getExternalStorageDirectory()?.path,"newAudioRecord.wav")

        recordBufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        //AudioTrack缓冲区大小  第二个参数播放用out  录制用in
        trackBufferSize = AudioTrack.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        //实例化AudioTrack对象  使用MODE_STREAM模式  （MODE_STATIC是把文件先写入到缓冲区 录音文件比较大 用这种容易发生oom）
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            44100,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            trackBufferSize,
            AudioTrack.MODE_STREAM
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            recordBufferSize
        )

        //录制
        btn_record.setOnClickListener {
            if (isRecording) return@setOnClickListener
            isRecording = true
            startRecord()
        }
        //停止
        btn_stop.setOnClickListener {
            isRecording = false
            audioRecord?.stop()
            audioRecord?.release()
            PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT).pcmToWav(
                File(
                    Environment.getExternalStorageDirectory()?.path,"audioRecord.pcm").path,wavFile?.path?: File(
                    Environment.getExternalStorageDirectory()?.path,"newAudioRecord.wav").path)
        }
        //播放
        btn_read.setOnClickListener {
            Log.e("TAG","${wavFile?.path}")
            if (isRecording) {
                Toast.makeText(this,"请先停止录音", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            //通过AudioTrack播放（AudioTrack只能播放pcm数据流）
            playByAudioTrack()

            //通过MediaPlayer播放（MediaPlayer能播放多种格式的音频，是对AudioTrack的封装）
            playByMediaPlayer()
        }
    }

    /**
     * 通过MediaPlayer播放音频
     */
    private fun playByMediaPlayer() {
        try {
            player?.let {
                it.setDataSource(wavFile?.path)
                it.prepare()
                it.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 通过AudioTrack播放音频
     */
    private fun playByAudioTrack() {
        thread {
            var fis: FileInputStream? = null
            try {
                val tempBuffer = ByteArray(trackBufferSize)
                var readCount = 0
                fis = FileInputStream(wavFile!!)
                while (fis.available() > 0) {
                    readCount = fis.read(tempBuffer)
                    if (readCount == AudioTrack.ERROR_INVALID_OPERATION || readCount == AudioTrack.ERROR_BAD_VALUE) {
                        continue
                    }
                    if (readCount != 0 && readCount != -1) {
                        audioTrack?.play()
                        audioTrack?.write(tempBuffer, 0, readCount)
                    }
                }

    //                    val out = ByteArrayOutputStream(trackBufferSize)
    //                    var b: Int = fis.read()
    //                    while (b != -1) {
    //                        out.write(b)
    //                    }
    //                    audioData = out.toByteArray()
    //                    audioTrack?.write(audioData!!,0,audioData?.size?:0)
    //                    audioTrack?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                fis?.close()
            }
        }
    }

    /**
     * 获取SD卡路径
     */
    private fun getSDCardPath(){
        if(Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            //为真则SD卡已装入，
            sdCardExist= Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

        }

        if(sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory()//获取跟目录
            //查找SD卡根路径
            sdDir.toString()
            Log.e("main", "得到的根目录路径:$sdDir")
        }
    }

    /**
     * 开始录制
     * 将数据通过流写入文件保存
     */
    private fun startRecord(){
        val pcmFile = File(Environment.getExternalStorageDirectory()?.path,"audioRecord.pcm")
        thread {
            if (audioRecord?.state == AudioRecord.STATE_UNINITIALIZED) return@thread
            audioRecord?.startRecording()
            val os: FileOutputStream?
            try {
                os = FileOutputStream(pcmFile)
                val bytes = ByteArray(recordBufferSize)
                while (isRecording){
                    audioRecord?.read(bytes,0,bytes.size)
                    os.write(bytes)
                    os.flush()
                }
                os.flush()
                os.close()
            }catch (e: Exception){
                e.printStackTrace()
                audioRecord?.stop()
            }
        }
    }
}
