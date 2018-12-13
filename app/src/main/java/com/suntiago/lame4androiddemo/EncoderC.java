package com.suntiago.lame4androiddemo;

import android.media.AudioFormat;
import android.os.Environment;
import android.util.Log;

import com.naman14.androidlame.AndroidLame;
import com.naman14.androidlame.LameBuilder;
import com.naman14.androidlame.WaveReader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by zy on 2018/12/13.
 */

public class EncoderC {

    private final String TAG = getClass().getSimpleName();
    final int OUTPUT_STREAM_BUFFER = 8192;


    private static int frequency = 16000;
    //CHANNEL_IN_STEREO;//双声道
    private static int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    private static int channels = 1;
    //音频数据格式：脉冲编码调制（PCM）每个样品16位
    private static int EncodingBitRate = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_BPP = 16;

    WaveReader waveReader;

    BufferedOutputStream outputStream;

    //wav文件转mp3
    //成功后回调转码后的文件路径，失败回调null
    public void encodeAndSend(File input, final Trans tran) {
        if (input == null || tran == null) {
            return;
        }
        final File output = new File(Environment.getExternalStorageDirectory() + "/testencode.mp3");
        int CHUNK_SIZE = 8192;
        Log.d(TAG, "Initialising wav reader");
        waveReader = new WaveReader(input);

        try {
            waveReader.openWave();
        } catch (IOException e) {
            e.printStackTrace();
            tran.trans(null);
        }

        Log.d(TAG, "Intitialising encoder");
        Log.d(TAG, "waveReader.getSampleRate():" + waveReader.getSampleRate());
        Log.d(TAG, "waveReader.getChannels():" + waveReader.getChannels());
        AndroidLame androidLame = new LameBuilder()
                .setInSampleRate(waveReader.getSampleRate())
                .setOutChannels(waveReader.getChannels())
                .setOutBitrate(32)
                .setOutSampleRate(waveReader.getSampleRate())
                .setQuality(1)
                .build();
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(output), OUTPUT_STREAM_BUFFER);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            tran.trans(null);
        }

        int bytesRead = 0;

        short[] buffer_l = new short[CHUNK_SIZE];
        short[] buffer_r = new short[CHUNK_SIZE];
        byte[] mp3Buf = new byte[CHUNK_SIZE];

        int channels = waveReader.getChannels();

        Log.d(TAG, "started encoding");
        while (true) {
            try {
                if (channels == 2) {
                    Log.d(TAG, "started encoding channels == 2");
                    bytesRead = waveReader.read(buffer_l, buffer_r, CHUNK_SIZE);
                    Log.d(TAG, "bytes read=" + bytesRead);

                    if (bytesRead > 0) {

                        int bytesEncoded = 0;
                        bytesEncoded = androidLame.encode(buffer_l, buffer_r, bytesRead, mp3Buf);
                        Log.d(TAG, "bytes encoded=" + bytesEncoded);

                        if (bytesEncoded > 0) {
                            try {
                                Log.d(TAG, "writing mp3 buffer to outputstream with " + bytesEncoded + " bytes");
                                outputStream.write(mp3Buf, 0, bytesEncoded);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    } else break;
                } else {
                    Log.d(TAG, "started encoding channels == 1");
                    bytesRead = waveReader.read(buffer_l, CHUNK_SIZE);
                    Log.d(TAG, "bytes read=" + bytesRead);

                    if (bytesRead > 0) {
                        int bytesEncoded = 0;
                        bytesEncoded = androidLame.encode(buffer_l, buffer_l, bytesRead, mp3Buf);
                        Log.d(TAG, "bytes encoded=" + bytesEncoded);
                        if (bytesEncoded > 0) {
                            try {
                                Log.d(TAG, "writing mp3 buffer to outputstream with " + bytesEncoded + " bytes");
                                outputStream.write(mp3Buf, 0, bytesEncoded);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                tran.trans(null);
            }
        }

        Log.d(TAG, "flushing final mp3buffer");
        int outputMp3buf = androidLame.flush(mp3Buf);
        Log.d(TAG, "flushed " + outputMp3buf + " bytes");

        if (outputMp3buf > 0) {
            try {
                Log.d(TAG, "writing final mp3buffer to outputstream");
                outputStream.write(mp3Buf, 0, outputMp3buf);
                Log.d(TAG, "closing output stream");
                outputStream.close();
                if (tran != null) {
                    tran.trans(output.getPath());
                }
                Log.d(TAG, "encodeAndSend  [input]:" + output.getPath());
                Log.d(TAG, "Output mp3 saved in" + output.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                tran.trans(null);
            }
        } else {
            tran.trans(null);
        }
    }

    //PCM  原始文件传wav
    // 为wav文件添加头，尾
    private void copyWaveFile(String inFilename, String outFilename) {
        Log.d(TAG, "copyWaveFile");
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = frequency;
        long byteRate = RECORDER_BPP * frequency * channels / 8;
        byte[] data = new byte[1280];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {
        Log.d(TAG, "WriteWaveFileHeader");
        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

    public interface Trans {
        void trans(String path);
    }
}
