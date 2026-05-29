package com.voicecalendar.asr;

/**
 * 语音识别服务提供者接口
 * 支持扩展不同的 ASR 服务商：百度、阿里云、讯飞等
 */
public interface AsrProvider {

    /**
     * 识别语音数据，返回文本
     * @param audioData 音频字节数据 (支持 WAV / PCM 格式)
     * @param format 音频格式: "wav" / "pcm"
     * @param sampleRate 采样率: 8000 / 16000
     * @return 识别的文本，失败时返回 null
     */
    String recognize(byte[] audioData, String format, int sampleRate) throws Exception;

    /** 服务商名称 */
    String getProviderName();
}
