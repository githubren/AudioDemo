package com.rb.aidiodemo

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
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


class MainActivity : AppCompatActivity() {

    private var recordBufferSize = 0
    private var audioRecord : AudioRecord? = null
    private var isRecording = false
    private var sdCardExist = false
    private var sdDir : File? = null
    private var player : MediaPlayer? = null
    private var wavFile : File? = null

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

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            recordBufferSize
        )

        //录制M
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
            try {
                player?.let {
                    it.setDataSource(wavFile?.path)
                    it.prepare()
                    it.start()
                }

            }catch (e: Exception){
                e.printStackTrace()
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
            var os: FileOutputStream?
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
